using Microsoft.AspNetCore.Antiforgery;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class Member : IdentityUser
{
    public string DisplayName { get; set; } = "";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? LastLoginAt { get; set; }
    public DateTime? SuspendedUntil { get; set; }
    public long? TierId { get; set; }
}

public sealed class MemberDatabase(DbContextOptions<MemberDatabase> options) : IdentityDbContext<Member>(options)
{
    public DbSet<CommunityPost> Posts => Set<CommunityPost>();
    public DbSet<CommunityComment> Comments => Set<CommunityComment>();
    public DbSet<FacilityReview> Reviews => Set<FacilityReview>();
    public DbSet<Inquiry> Inquiries => Set<Inquiry>();
    public DbSet<InquiryAnswer> InquiryAnswers => Set<InquiryAnswer>();
    public DbSet<ContentAttachment> Attachments => Set<ContentAttachment>();
    public DbSet<MemberTier> MemberTiers => Set<MemberTier>();
    public DbSet<AdminAudit> AdminAudits => Set<AdminAudit>();
    protected override void OnModelCreating(ModelBuilder model)
    {
        base.OnModelCreating(model);
        model.Entity<Member>().HasOne<MemberTier>().WithMany().HasForeignKey(x => x.TierId).OnDelete(DeleteBehavior.Restrict);
        model.Entity<MemberTier>().HasIndex(x => x.Code).IsUnique();
        model.Entity<Inquiry>().HasOne(x => x.Author).WithMany().HasForeignKey(x => x.AuthorId).OnDelete(DeleteBehavior.Cascade);
        model.Entity<InquiryAnswer>().HasOne(x => x.Author).WithMany().HasForeignKey(x => x.AuthorId).OnDelete(DeleteBehavior.SetNull);
        model.Entity<InquiryAnswer>().HasOne<Inquiry>().WithMany().HasForeignKey(x => x.InquiryId).OnDelete(DeleteBehavior.Cascade);
        model.Entity<ContentAttachment>().HasOne<Inquiry>().WithMany().HasForeignKey(x => x.InquiryId).OnDelete(DeleteBehavior.Cascade);
        model.Entity<ContentAttachment>().HasOne<CommunityPost>().WithMany().HasForeignKey(x => x.PostId).OnDelete(DeleteBehavior.Cascade);
        model.Entity<ContentAttachment>().ToTable(t => t.HasCheckConstraint("CK_Attachment_Parent", "(PostId IS NULL) <> (InquiryId IS NULL)"));
        model.Entity<Member>().HasIndex(x => x.NormalizedEmail).IsUnique();
        model.Entity<Member>().Property(x => x.DisplayName).HasMaxLength(50);
        model.Entity<CommunityPost>().HasOne(x => x.Author).WithMany().HasForeignKey(x => x.AuthorId).OnDelete(DeleteBehavior.SetNull);
        model.Entity<CommunityComment>().HasOne(x => x.Author).WithMany().HasForeignKey(x => x.AuthorId).OnDelete(DeleteBehavior.SetNull);
        model.Entity<CommunityComment>().HasOne<CommunityPost>().WithMany().HasForeignKey(x => x.PostId).OnDelete(DeleteBehavior.Cascade);
        model.Entity<CommunityPost>().HasIndex(x => new { x.Category, x.Id });
        model.Entity<FacilityReview>().HasOne(x => x.Author).WithMany().HasForeignKey(x => x.AuthorId).OnDelete(DeleteBehavior.SetNull);
        model.Entity<FacilityReview>().HasIndex(x => new { x.FacilityId, x.AuthorId }).IsUnique();
    }
}

public record RegisterMember(string? Username, string? Email, string? Password, string? DisplayName);
public record LoginMember(string? Username, string? Password);
public record ChangeMemberPassword(string? CurrentPassword, string? NewPassword);
public record DeleteMember(string? Password);
public record UpdateMember(string? DisplayName);

public static class Accounts
{
    public static void AddAccounts(this WebApplicationBuilder builder)
    {
        var directory = Path.GetFullPath(builder.Configuration["State:Path"] ?? Path.Combine(builder.Environment.ContentRootPath, "App_Data"));
        Directory.CreateDirectory(directory);
        builder.Services.AddDataProtection().SetApplicationName("MedicalInfo")
            .PersistKeysToFileSystem(new DirectoryInfo(Path.Combine(directory, "keys")));
        var connection = new SqliteConnectionStringBuilder { DataSource = Path.Combine(directory, "members.db"), ForeignKeys = true }.ToString();
        builder.Services.AddDbContext<MemberDatabase>(options => options.UseSqlite(connection));
        builder.Services.AddIdentity<Member, IdentityRole>(options => {
            options.Password.RequiredLength = 12;
            options.Password.RequireDigit = false;
            options.Password.RequireLowercase = false;
            options.Password.RequireUppercase = false;
            options.Password.RequireNonAlphanumeric = false;
            options.User.RequireUniqueEmail = true;
            options.Lockout.MaxFailedAccessAttempts = 5;
            options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
        }).AddEntityFrameworkStores<MemberDatabase>().AddDefaultTokenProviders();
        builder.Services.Configure<SecurityStampValidatorOptions>(options => options.ValidationInterval = TimeSpan.Zero);
        builder.Services.ConfigureApplicationCookie(options => {
            options.Cookie.Name = "MedicalInfo.Session";
            options.Cookie.HttpOnly = true;
            options.Cookie.SameSite = SameSiteMode.Strict;
            options.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
            options.ExpireTimeSpan = TimeSpan.FromHours(8);
            options.SlidingExpiration = false;
            options.Events.OnRedirectToLogin = context => { context.Response.StatusCode = 401; return Task.CompletedTask; };
            options.Events.OnRedirectToAccessDenied = context => { context.Response.StatusCode = 403; return Task.CompletedTask; };
            options.Events.OnValidatePrincipal = async context => {
                await SecurityStampValidator.ValidatePrincipalAsync(context);
                if (context.Principal?.Identity?.IsAuthenticated == true) {
                    var users = context.HttpContext.RequestServices.GetRequiredService<UserManager<Member>>();
                    var user = await users.GetUserAsync(context.Principal);
                    if (user?.SuspendedUntil > DateTime.UtcNow) {
                        context.RejectPrincipal();
                        await context.HttpContext.RequestServices.GetRequiredService<SignInManager<Member>>().SignOutAsync();
                    }
                }
            };
        });
        builder.Services.AddAntiforgery(options => {
            options.HeaderName = "X-CSRF-TOKEN";
            options.Cookie.Name = "MedicalInfo.Csrf";
            options.Cookie.SameSite = SameSiteMode.Strict;
            options.Cookie.HttpOnly = true;
        });
        builder.Services.AddAuthorization();
    }

    public static async Task InitializeAccounts(this WebApplication app)
    {
        using var scope = app.Services.CreateScope();
        var database = scope.ServiceProvider.GetRequiredService<MemberDatabase>();
        await database.Database.MigrateAsync();
    }

    private static IResult Invalid(string message) => Results.Problem(statusCode: 400, title: message);
    private static bool PasswordLength(string? password) => password?.Length is >= 12 and <= 128;
    private static bool NameValid(string? name) => !string.IsNullOrWhiteSpace(name) && name.Trim().Length <= 50;
    private static object Profile(Member user, bool isAdmin = false) => new { user.Id, username = user.UserName, user.DisplayName,
        user.Email, user.EmailConfirmed, user.CreatedAt, user.LastLoginAt, user.TierId, isAdmin };

    public static void MapAccounts(this WebApplication app)
    {
        var group = app.MapGroup("/api/account").RequireRateLimiting("account");
        group.AddEndpointFilter(async (context, next) => {
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            if (!HttpMethods.IsGet(context.HttpContext.Request.Method)) {
                try { await context.HttpContext.RequestServices.GetRequiredService<IAntiforgery>().ValidateRequestAsync(context.HttpContext); }
                catch (AntiforgeryValidationException) { return Invalid("보안 확인이 만료되었습니다. 새로고침 후 다시 시도해주세요."); }
            }
            return await next(context);
        });
        group.MapGet("/csrf", (HttpContext context, IAntiforgery antiforgery) =>
            Results.Ok(new { token = antiforgery.GetAndStoreTokens(context).RequestToken }));
        group.MapPost("/register", async (RegisterMember input, UserManager<Member> users) => {
            if (input.Username?.Length is not (>= 3 and <= 40) ||
                !System.Text.RegularExpressions.Regex.IsMatch(input.Username, "^[a-zA-Z0-9._-]+$"))
                return Invalid("아이디는 영문·숫자·점·밑줄·하이픈 3~40자로 입력해주세요.");
            if (!PasswordLength(input.Password)) return Invalid("비밀번호는 12~128자로 입력해주세요.");
            if (!NameValid(input.DisplayName)) return Invalid("표시 이름은 1~50자로 입력해주세요.");
            if (string.IsNullOrWhiteSpace(input.Email) || input.Email.Length > 254 ||
                !System.Net.Mail.MailAddress.TryCreate(input.Email, out var email) || email.Address != input.Email)
                return Invalid("올바른 이메일 주소를 입력해주세요.");
            var user = new Member { UserName = input.Username, Email = input.Email, DisplayName = input.DisplayName!.Trim() };
            try {
                var result = await users.CreateAsync(user, input.Password!);
                return result.Succeeded ? Results.Created("/api/account/me", Profile(user))
                    : Invalid("가입 정보를 확인해주세요. 아이디 또는 이메일이 이미 사용 중일 수 있습니다.");
            } catch (DbUpdateException ex) when (ex.InnerException is SqliteException { SqliteErrorCode: 19 }) {
                return Results.Conflict(new { title = "이미 사용 중인 가입 정보입니다." });
            }
        });
        group.MapPost("/login", async (LoginMember input, UserManager<Member> users, SignInManager<Member> signIn) => {
            if (input.Username?.Length is not (>= 3 and <= 40) || input.Password?.Length is not (>= 1 and <= 128))
                return Results.Unauthorized();
            var user = await users.FindByNameAsync(input.Username);
            if (user is null || user.SuspendedUntil > DateTime.UtcNow) return Results.Unauthorized();
            var result = await signIn.CheckPasswordSignInAsync(user, input.Password, lockoutOnFailure: true);
            if (!result.Succeeded) return Results.Unauthorized();
            user.LastLoginAt = DateTime.UtcNow;
            if (!(await users.UpdateAsync(user)).Succeeded) return Results.Conflict();
            await signIn.SignInAsync(user, isPersistent: false);
            return Results.Ok(Profile(user));
        });
        group.MapGet("/me", async (HttpContext context, UserManager<Member> users) => {
            var user = await users.GetUserAsync(context.User);
            return user is null ? Results.Unauthorized() : Results.Ok(Profile(user, context.User.IsInRole("Admin")));
        }).RequireAuthorization();
        group.MapPost("/logout", async (SignInManager<Member> signIn) => {
            await signIn.SignOutAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapPut("/profile", async (UpdateMember input, HttpContext context, UserManager<Member> users) => {
            if (!NameValid(input.DisplayName)) return Invalid("표시 이름은 1~50자로 입력해주세요.");
            var user = await users.GetUserAsync(context.User);
            if (user is null) return Results.Unauthorized();
            user.DisplayName = input.DisplayName!.Trim();
            return (await users.UpdateAsync(user)).Succeeded ? Results.Ok(Profile(user)) : Results.Conflict();
        }).RequireAuthorization();
        group.MapPost("/password", async (ChangeMemberPassword input, HttpContext context, UserManager<Member> users, SignInManager<Member> signIn) => {
            if (input.CurrentPassword?.Length is not (>= 1 and <= 128) || !PasswordLength(input.NewPassword))
                return Invalid("현재 비밀번호와 12~128자의 새 비밀번호를 입력해주세요.");
            var user = await users.GetUserAsync(context.User);
            if (user is null) return Results.Unauthorized();
            var result = await users.ChangePasswordAsync(user, input.CurrentPassword, input.NewPassword!);
            if (!result.Succeeded) return Invalid("비밀번호를 확인해주세요.");
            await signIn.SignOutAsync();
            return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/me", async ([Microsoft.AspNetCore.Mvc.FromBody] DeleteMember input, HttpContext context, UserManager<Member> users, SignInManager<Member> signIn, MemberDatabase db) => {
            await using var transaction = await db.Database.BeginTransactionAsync();
            var user = await users.GetUserAsync(context.User);
            if (user is null) return Results.Unauthorized();
            if (input.Password?.Length is not (>= 1 and <= 128) || !await users.CheckPasswordAsync(user, input.Password))
                return Invalid("탈퇴하려면 현재 비밀번호를 확인해주세요.");
            if (await users.IsInRoleAsync(user, "Admin") && await Administration.AdminCount(db) <= 1)
                return Results.Problem(statusCode:409, title:"마지막 관리자는 탈퇴할 수 없습니다. 다른 관리자에게 먼저 권한을 넘겨주세요.");
            if (!(await users.DeleteAsync(user)).Succeeded) return Results.Conflict();
            await transaction.CommitAsync();
            await signIn.SignOutAsync(); return Results.NoContent();
        }).RequireAuthorization();
    }
}
