using System.Security.Claims;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class MemberTier
{
    public long Id { get; set; }
    public string Code { get; set; } = "";
    public string Name { get; set; } = "";
}
public sealed class AdminAudit
{
    public long Id { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public string ActorId { get; set; } = "";
    public string TargetId { get; set; } = "";
    public string Action { get; set; } = "";
    public string Detail { get; set; } = "";
    public string Reason { get; set; } = "";
}
public record MemberRoleInput(string? Role, string? Reason);
public record SuspensionInput(int Days, string? Reason);
public record TierInput(string? Code, string? Name);
public record AssignTierInput(long? TierId, string? Reason);

public static class Administration
{
    private static readonly string[] Roles = ["Admin", "Doctor", "Pharmacist"];
    private static bool ReasonValid(string? value) => !string.IsNullOrWhiteSpace(value) && value.Trim().Length <= 500;
    internal static Task<int> AdminCount(MemberDatabase db) =>
        (from link in db.UserRoles join role in db.Roles on link.RoleId equals role.Id where role.NormalizedName == "ADMIN" select link.UserId).CountAsync();
    private static void Audit(MemberDatabase db, string actor, string target, string action, string detail, string reason) =>
        db.AdminAudits.Add(new AdminAudit { ActorId=actor,TargetId=target,Action=action,Detail=detail,Reason=reason.Trim() });
    private static string Actor(HttpContext context) => context.User.FindFirstValue(ClaimTypes.NameIdentifier)!;

    // Only a host operator can invoke this command. It cannot create an account or reset a password.
    public static async Task<int> Bootstrap(WebApplication app, string username) {
        using var scope=app.Services.CreateScope();var db=scope.ServiceProvider.GetRequiredService<MemberDatabase>();
        var users=scope.ServiceProvider.GetRequiredService<UserManager<Member>>();var roles=scope.ServiceProvider.GetRequiredService<RoleManager<IdentityRole>>();
        await using var transaction=await db.Database.BeginTransactionAsync();
        if(await AdminCount(db)>0){Console.Error.WriteLine("An administrator already exists. Use the administrator page to transfer roles.");return 1;}
        var user=await users.FindByNameAsync(username);
        if(user is null || user.SuspendedUntil>DateTime.UtcNow){Console.Error.WriteLine("An active registered account is required.");return 1;}
        if(!await roles.RoleExistsAsync("Admin") && !(await roles.CreateAsync(new IdentityRole("Admin"))).Succeeded)return 1;
        if(!(await users.AddToRoleAsync(user,"Admin")).Succeeded || !(await users.UpdateSecurityStampAsync(user)).Succeeded)return 1;
        Audit(db,"host-operator",user.Id,"bootstrap-admin","Admin","Initial administrator provisioning");
        await db.SaveChangesAsync();await transaction.CommitAsync();Console.WriteLine("Initial administrator provisioned. Sign in again.");return 0;
    }

    public static void MapAdministration(this WebApplication app) {
        var group=Inquiries.Protect(app.MapGroup("/api/admin"));
        group.RequireAuthorization(policy=>policy.RequireRole("Admin"));
        group.MapGet("/members",async(string? keyword,int? page,bool? inactive,MemberDatabase db)=>{
            var current=page??1;if(current is <1 or >100000 || keyword?.Length>100)return Results.BadRequest();
            var query=db.Users.AsNoTracking();
            var cutoff=DateTime.UtcNow.AddMonths(-6);
            if(inactive==true)query=query.Where(x=>(x.LastLoginAt??x.CreatedAt)<cutoff);
            if(!string.IsNullOrWhiteSpace(keyword)){var term=keyword.Trim().ToUpperInvariant();query=query.Where(x=>x.NormalizedUserName!.Contains(term)||x.NormalizedEmail!.Contains(term)||x.DisplayName.Contains(keyword.Trim()));}
            var total=await query.CountAsync();
            var items=await query.OrderByDescending(x=>x.CreatedAt).ThenBy(x=>x.Id).Skip((current-1)*10).Take(10)
                .Select(x=>new {x.Id,username=x.UserName,x.DisplayName,x.Email,x.EmailConfirmed,x.CreatedAt,x.LastLoginAt,x.SuspendedUntil,x.TierId,
                    inactive=(x.LastLoginAt??x.CreatedAt)<cutoff,
                    roles=(from link in db.UserRoles join role in db.Roles on link.RoleId equals role.Id where link.UserId==x.Id select role.Name).ToArray()}).ToArrayAsync();
            return Results.Ok(new {total,page=current,pageSize=10,items});
        });
        group.MapPut("/members/{id}/role",async(string id,MemberRoleInput input,HttpContext context,MemberDatabase db,UserManager<Member> users,RoleManager<IdentityRole> roles)=>{
            if(input.Role is not ("Member" or "Admin" or "Doctor" or "Pharmacist") || !ReasonValid(input.Reason))return Results.BadRequest();
            if(id==Actor(context))return Results.Problem(statusCode:409,title:"자신의 권한은 변경할 수 없습니다. 다른 관리자에게 요청해주세요.");
            await using var transaction=await db.Database.BeginTransactionAsync();
            var user=await users.FindByIdAsync(id);if(user is null)return Results.NotFound();
            if(input.Role=="Admin" && user.SuspendedUntil>DateTime.UtcNow)return Results.Problem(statusCode:409,title:"이용 제한 중인 회원은 관리자로 지정할 수 없습니다.");
            var previous=await users.GetRolesAsync(user);
            if(previous.Contains("Admin") && input.Role!="Admin" && await AdminCount(db)<=1)return Results.Conflict();
            var removed=previous.Where(Roles.Contains).ToArray();
            if(removed.Length>0 && !(await users.RemoveFromRolesAsync(user,removed)).Succeeded)return Results.Conflict();
            if(input.Role!="Member"){
                if(!await roles.RoleExistsAsync(input.Role) && !(await roles.CreateAsync(new IdentityRole(input.Role))).Succeeded)return Results.Conflict();
                if(!(await users.AddToRoleAsync(user,input.Role)).Succeeded)return Results.Conflict();
            }
            if(!(await users.UpdateSecurityStampAsync(user)).Succeeded)return Results.Conflict();
            Audit(db,Actor(context),id,"role",$"{string.Join(',',previous)} -> {input.Role}",input.Reason!);
            await db.SaveChangesAsync();await transaction.CommitAsync();return Results.NoContent();
        });
        group.MapPut("/members/{id}/suspension",async(string id,SuspensionInput input,HttpContext context,MemberDatabase db,UserManager<Member> users)=>{
            if(input.Days is <0 or >365 || !ReasonValid(input.Reason))return Results.BadRequest();
            if(id==Actor(context))return Results.Problem(statusCode:409,title:"자신의 계정은 이용 제한할 수 없습니다.");
            await using var transaction=await db.Database.BeginTransactionAsync();
            var user=await users.FindByIdAsync(id);if(user is null)return Results.NotFound();
            if(await users.IsInRoleAsync(user,"Admin"))return Results.Problem(statusCode:409,title:"관리자의 권한을 일반 회원으로 변경한 뒤 이용 제한해주세요.");
            var previous=user.SuspendedUntil;user.SuspendedUntil=input.Days==0?null:DateTime.UtcNow.AddDays(input.Days);
            if(!(await users.UpdateSecurityStampAsync(user)).Succeeded)return Results.Conflict();
            Audit(db,Actor(context),id,"suspension",$"{previous:O} -> {user.SuspendedUntil:O}",input.Reason!);
            await db.SaveChangesAsync();await transaction.CommitAsync();return Results.NoContent();
        });
        group.MapGet("/tiers",async(MemberDatabase db)=>Results.Ok(await db.MemberTiers.AsNoTracking().OrderBy(x=>x.Code).ToArrayAsync()));
        group.MapPost("/tiers",async(TierInput input,MemberDatabase db,HttpContext context)=>{
            if(input.Code is null || !System.Text.RegularExpressions.Regex.IsMatch(input.Code,"^[a-z0-9_-]{1,30}$") || string.IsNullOrWhiteSpace(input.Name)||input.Name.Trim().Length>50)return Results.BadRequest();
            await using var transaction=await db.Database.BeginTransactionAsync();
            if(await db.MemberTiers.AnyAsync(x=>x.Code==input.Code))return Results.Conflict();
            var tier=new MemberTier{Code=input.Code,Name=input.Name.Trim()};db.MemberTiers.Add(tier);await db.SaveChangesAsync();
            Audit(db,Actor(context),tier.Id.ToString(),"tier-create",tier.Code+" / "+tier.Name,"등급 생성");await db.SaveChangesAsync();await transaction.CommitAsync();return Results.Ok(tier);
        });
        group.MapPut("/tiers/{id:long}",async(long id,TierInput input,MemberDatabase db,HttpContext context)=>{
            if(string.IsNullOrWhiteSpace(input.Name)||input.Name.Trim().Length>50)return Results.BadRequest();
            var tier=await db.MemberTiers.FindAsync(id);if(tier is null)return Results.NotFound();
            var previous=tier.Name;tier.Name=input.Name.Trim();Audit(db,Actor(context),id.ToString(),"tier-rename",previous+" -> "+tier.Name,"등급 이름 변경");await db.SaveChangesAsync();return Results.NoContent();
        });
        group.MapDelete("/tiers/{id:long}",async(long id,MemberDatabase db,HttpContext context)=>{
            await using var transaction=await db.Database.BeginTransactionAsync();var tier=await db.MemberTiers.FindAsync(id);if(tier is null)return Results.NotFound();
            if(await db.Users.AnyAsync(x=>x.TierId==id))return Results.Problem(statusCode:409,title:"회원에게 사용 중인 등급은 삭제할 수 없습니다.");
            Audit(db,Actor(context),id.ToString(),"tier-delete",tier.Code,"등급 삭제");db.MemberTiers.Remove(tier);await db.SaveChangesAsync();await transaction.CommitAsync();return Results.NoContent();
        });
        group.MapPut("/members/{id}/tier",async(string id,AssignTierInput input,MemberDatabase db,UserManager<Member> users,HttpContext context)=>{
            if(!ReasonValid(input.Reason))return Results.BadRequest();
            await using var transaction=await db.Database.BeginTransactionAsync();var user=await users.FindByIdAsync(id);if(user is null)return Results.NotFound();
            if(input.TierId is not null && !await db.MemberTiers.AnyAsync(x=>x.Id==input.TierId))return Results.BadRequest();
            var previous=user.TierId;user.TierId=input.TierId;if(!(await users.UpdateAsync(user)).Succeeded)return Results.Conflict();
            Audit(db,Actor(context),id,"tier-assign",$"{previous} -> {input.TierId}",input.Reason!);await db.SaveChangesAsync();await transaction.CommitAsync();return Results.NoContent();
        });
        group.MapGet("/audit",async(int? page,MemberDatabase db)=>{
            var current=page??1;if(current is <1 or >100000)return Results.BadRequest();
            return Results.Ok(new{total=await db.AdminAudits.CountAsync(),page=current,pageSize=20,items=await db.AdminAudits.AsNoTracking().OrderByDescending(x=>x.Id).Skip((current-1)*20).Take(20).ToArrayAsync()});
        });
    }
}
