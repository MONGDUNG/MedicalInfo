using System.Security.Claims;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class Inquiry
{
    public long Id { get; set; }
    public string Title { get; set; } = "";
    public string Content { get; set; } = "";
    public bool IsPrivate { get; set; }
    public string AuthorId { get; set; } = "";
    public Member Author { get; set; } = null!;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }
}
public sealed class InquiryAnswer
{
    public long Id { get; set; }
    public long InquiryId { get; set; }
    public string Content { get; set; } = "";
    public string? AuthorId { get; set; }
    public Member? Author { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }
}
public record InquiryInput(string? Title, string? Content, string? Visibility);

public static class Inquiries
{
    internal static bool Owns(ClaimsPrincipal user, string? author) => user.Identity?.IsAuthenticated == true &&
        (user.FindFirstValue(ClaimTypes.NameIdentifier) == author || user.IsInRole("Admin"));
    internal static IQueryable<Inquiry> Visible(MemberDatabase db, ClaimsPrincipal user) {
        var id = user.FindFirstValue(ClaimTypes.NameIdentifier);
        var admin = user.IsInRole("Admin");
        return db.Inquiries.Where(x => !x.IsPrivate || x.AuthorId == id || admin);
    }
    internal static RouteGroupBuilder Protect(RouteGroupBuilder group) {
        group.RequireRateLimiting("search");
        group.AddEndpointFilter(async (context, next) => {
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            if (!HttpMethods.IsGet(context.HttpContext.Request.Method)) {
                try { await context.HttpContext.RequestServices.GetRequiredService<IAntiforgery>().ValidateRequestAsync(context.HttpContext); }
                catch (AntiforgeryValidationException) { return Results.Problem(statusCode:400, title:"보안 확인이 만료되었습니다. 새로고침해주세요."); }
            }
            return await next(context);
        });
        return group;
    }
    private static bool Valid(InquiryInput input) => !string.IsNullOrWhiteSpace(input.Title) && input.Title.Length <= 200 &&
        !string.IsNullOrWhiteSpace(input.Content) && input.Content.Length <= 4000 && input.Visibility is "PUBLIC" or "PRIVATE";
    public static void MapInquiries(this WebApplication app) {
        var group = Protect(app.MapGroup("/api/inquiries"));
        group.MapGet("/", async (string? keyword, int? page, MemberDatabase db, HttpContext context) => {
            var current = page ?? 1;
            if (current is < 1 or > 100000 || keyword?.Length > 100) return Results.BadRequest();
            var query = Visible(db, context.User).AsNoTracking();
            if (!string.IsNullOrWhiteSpace(keyword)) { var term = keyword.Trim(); query = query.Where(x => x.Title.Contains(term) || x.Content.Contains(term) || db.InquiryAnswers.Any(a => a.InquiryId == x.Id && a.Content.Contains(term))); }
            var total = await query.CountAsync();
            var items = await query.OrderByDescending(x => x.Id).Skip((current-1)*10).Take(10)
                .Select(x => new { x.Id, x.Title, x.IsPrivate, author=x.Author.DisplayName, x.CreatedAt, answerCount=db.InquiryAnswers.Count(a => a.InquiryId == x.Id) }).ToArrayAsync();
            return Results.Ok(new { total, page=current, pageSize=10, items });
        });
        group.MapGet("/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var item = await Visible(db, context.User).Include(x => x.Author).SingleOrDefaultAsync(x => x.Id == id);
            return item is null ? Results.NotFound() : Results.Ok(new { item.Id, item.Title, item.Content, item.IsPrivate, author=item.Author.DisplayName, item.CreatedAt, item.UpdatedAt, canEdit=Owns(context.User,item.AuthorId) });
        });
        group.MapPost("/", async (InquiryInput input, MemberDatabase db, HttpContext context) => {
            if (!Valid(input)) return Results.Problem(statusCode:400,title:"제목 1~200자, 본문 1~4000자, 공개 여부가 필요합니다.");
            var item = new Inquiry { Title=input.Title!.Trim(), Content=input.Content!, IsPrivate=input.Visibility=="PRIVATE", AuthorId=context.User.FindFirstValue(ClaimTypes.NameIdentifier)! };
            db.Inquiries.Add(item); await db.SaveChangesAsync(); return Results.Created($"/api/inquiries/{item.Id}",new { item.Id });
        }).RequireAuthorization();
        group.MapPut("/{id:long}", async (long id, InquiryInput input, MemberDatabase db, HttpContext context) => {
            var item = await Visible(db,context.User).SingleOrDefaultAsync(x => x.Id==id);
            if(item is null) return Results.NotFound();
            if(!Owns(context.User,item.AuthorId)) return Results.Forbid();
            if(!Valid(input)) return Results.BadRequest();
            item.Title=input.Title!.Trim(); item.Content=input.Content!; item.IsPrivate=input.Visibility=="PRIVATE"; item.UpdatedAt=DateTime.UtcNow;
            await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var item=await Visible(db,context.User).SingleOrDefaultAsync(x=>x.Id==id);
            if(item is null) return Results.NotFound();
            if(!Owns(context.User,item.AuthorId)) return Results.Forbid();
            db.Inquiries.Remove(item); await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapGet("/{id:long}/answers", async (long id, int? page, MemberDatabase db, HttpContext context) => {
            if(!await Visible(db,context.User).AnyAsync(x=>x.Id==id)) return Results.NotFound();
            var current=page??1; if(current is <1 or >100000) return Results.BadRequest();
            var query=db.InquiryAnswers.AsNoTracking().Where(x=>x.InquiryId==id); var total=await query.CountAsync();
            var rows=await query.Include(x=>x.Author).OrderBy(x=>x.Id).Skip((current-1)*20).Take(20).ToArrayAsync();
            return Results.Ok(new { total, page=current, pageSize=20, items=rows.Select(x=>new {x.Id,x.Content,author=x.Author?.DisplayName??"탈퇴 회원",x.CreatedAt,x.UpdatedAt,canEdit=Owns(context.User,x.AuthorId)}) });
        });
        group.MapPost("/{id:long}/answers", async (long id, CommentInput input, MemberDatabase db, HttpContext context) => {
            if(!await Visible(db,context.User).AnyAsync(x=>x.Id==id)) return Results.NotFound();
            if(string.IsNullOrWhiteSpace(input.Content)||input.Content.Length>4000) return Results.BadRequest();
            var item=new InquiryAnswer { InquiryId=id,Content=input.Content,AuthorId=context.User.FindFirstValue(ClaimTypes.NameIdentifier) };
            db.InquiryAnswers.Add(item); await db.SaveChangesAsync(); return Results.Ok(new {item.Id});
        }).RequireAuthorization();
        group.MapPut("/answers/{id:long}", async (long id, CommentInput input, MemberDatabase db, HttpContext context) => {
            var item=await db.InquiryAnswers.FindAsync(id);
            if(item is null||!await Visible(db,context.User).AnyAsync(x=>x.Id==item.InquiryId)) return Results.NotFound();
            if(!Owns(context.User,item.AuthorId)) return Results.Forbid();
            if(string.IsNullOrWhiteSpace(input.Content)||input.Content.Length>4000) return Results.BadRequest();
            item.Content=input.Content;item.UpdatedAt=DateTime.UtcNow;await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/answers/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var item=await db.InquiryAnswers.FindAsync(id);
            if(item is null||!await Visible(db,context.User).AnyAsync(x=>x.Id==item.InquiryId)) return Results.NotFound();
            if(!Owns(context.User,item.AuthorId)) return Results.Forbid();
            db.InquiryAnswers.Remove(item); await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
    }
}
