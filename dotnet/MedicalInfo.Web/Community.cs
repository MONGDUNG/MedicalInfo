using System.Security.Claims;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class CommunityPost
{
    public long Id { get; set; }
    public string Title { get; set; } = "";
    public string Content { get; set; } = "";
    public string Category { get; set; } = "자유";
    public string? AuthorId { get; set; }
    public Member? Author { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }
}
public sealed class CommunityComment
{
    public long Id { get; set; }
    public long PostId { get; set; }
    public string Content { get; set; } = "";
    public string? AuthorId { get; set; }
    public Member? Author { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }
}
public record PostInput(string? Title, string? Content, string? Category);
public record CommentInput(string? Content);

public static class Community
{
    private static bool Owns(ClaimsPrincipal user, string? author) => user.Identity?.IsAuthenticated == true &&
        (author == user.FindFirstValue(ClaimTypes.NameIdentifier) || user.IsInRole("Admin"));
    private static bool TextValid(string? text, int max) => !string.IsNullOrWhiteSpace(text) && text.Length <= max;
    private static bool PostValid(PostInput input) => TextValid(input.Title, 200) && TextValid(input.Content, 4000) && input.Category is "자유" or "질문" or "공지사항";
    private static IResult Invalid() => Results.Problem(statusCode: 400, title: "제목은 1~200자, 본문은 1~4000자이며 유효한 분류가 필요합니다.");
    public static void MapCommunity(this WebApplication app)
    {
        var group = app.MapGroup("/api/community").RequireRateLimiting("search");
        group.AddEndpointFilter(async (context, next) => {
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            if (!HttpMethods.IsGet(context.HttpContext.Request.Method)) {
                try { await context.HttpContext.RequestServices.GetRequiredService<IAntiforgery>().ValidateRequestAsync(context.HttpContext); }
                catch (AntiforgeryValidationException) { return Results.Problem(statusCode: 400, title: "보안 확인이 만료되었습니다. 새로고침해주세요."); }
            }
            return await next(context);
        });
        group.MapGet("/posts", async (string? keyword, string? category, int? page, MemberDatabase db) => {
            var current = page ?? 1;
            if (current is < 1 or > 100000 || keyword?.Length > 100 || category is not (null or "" or "자유" or "질문" or "공지사항")) return Invalid();
            var query = db.Posts.AsNoTracking();
            if (!string.IsNullOrWhiteSpace(keyword)) { var term = keyword.Trim(); query = query.Where(x => x.Title.Contains(term) || x.Content.Contains(term)); }
            if (!string.IsNullOrEmpty(category)) query = query.Where(x => x.Category == category);
            var total = await query.CountAsync();
            var items = await query.OrderByDescending(x => x.Id).Skip((current - 1) * 10).Take(10)
                .Select(x => new { x.Id, x.Title, x.Category, author = x.Author == null ? "탈퇴 회원" : x.Author.DisplayName, x.CreatedAt }).ToArrayAsync();
            return Results.Ok(new { total, page = current, pageSize = 10, items });
        });
        group.MapGet("/posts/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var post = await db.Posts.AsNoTracking().Include(x => x.Author).SingleOrDefaultAsync(x => x.Id == id);
            return post is null ? Results.NotFound() : Results.Ok(new { post.Id, post.Title, post.Content, post.Category,
                author = post.Author?.DisplayName ?? "탈퇴 회원", post.CreatedAt, post.UpdatedAt, canEdit = Owns(context.User, post.AuthorId) });
        });
        group.MapPost("/posts", async (PostInput input, MemberDatabase db, HttpContext context) => {
            if (!PostValid(input)) return Invalid();
            if (input.Category == "공지사항" && !context.User.IsInRole("Admin")) return Results.Forbid();
            var post = new CommunityPost { Title = input.Title!.Trim(), Content = input.Content!, Category = input.Category!, AuthorId = context.User.FindFirstValue(ClaimTypes.NameIdentifier) };
            db.Posts.Add(post); await db.SaveChangesAsync();
            return Results.Created($"/api/community/posts/{post.Id}", new { post.Id });
        }).RequireAuthorization();
        group.MapPut("/posts/{id:long}", async (long id, PostInput input, MemberDatabase db, HttpContext context) => {
            var post = await db.Posts.FindAsync(id);
            if (post is null) return Results.NotFound();
            if (!Owns(context.User, post.AuthorId) || ((post.Category == "공지사항" || input.Category == "공지사항") && !context.User.IsInRole("Admin"))) return Results.Forbid();
            if (!PostValid(input)) return Invalid();
            post.Title = input.Title!.Trim(); post.Content = input.Content!; post.Category = input.Category!; post.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/posts/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var post = await db.Posts.FindAsync(id);
            if (post is null) return Results.NotFound();
            if (!Owns(context.User, post.AuthorId)) return Results.Forbid();
            db.Posts.Remove(post); await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapGet("/posts/{id:long}/comments", async (long id, int? page, MemberDatabase db, HttpContext context) => {
            var current = page ?? 1;
            if (current is < 1 or > 100000) return Invalid();
            if (!await db.Posts.AnyAsync(x => x.Id == id)) return Results.NotFound();
            var query = db.Comments.AsNoTracking().Where(x => x.PostId == id);
            var total = await query.CountAsync();
            var comments = await query.OrderBy(x => x.Id).Skip((current - 1) * 20).Take(20).Include(x => x.Author).ToArrayAsync();
            return Results.Ok(new { total, page = current, pageSize = 20, items = comments.Select(x => new { x.Id, x.Content, author = x.Author?.DisplayName ?? "탈퇴 회원", x.CreatedAt, x.UpdatedAt, canEdit = Owns(context.User, x.AuthorId) }) });
        });
        group.MapPost("/posts/{id:long}/comments", async (long id, CommentInput input, MemberDatabase db, HttpContext context) => {
            if (!TextValid(input.Content, 2000)) return Results.Problem(statusCode: 400, title: "댓글은 1~2000자로 입력해주세요.");
            if (!await db.Posts.AnyAsync(x => x.Id == id)) return Results.NotFound();
            var comment = new CommunityComment { PostId = id, Content = input.Content!, AuthorId = context.User.FindFirstValue(ClaimTypes.NameIdentifier) };
            db.Comments.Add(comment); await db.SaveChangesAsync(); return Results.Created($"/api/community/posts/{id}/comments", new { comment.Id });
        }).RequireAuthorization();
        group.MapPut("/comments/{id:long}", async (long id, CommentInput input, MemberDatabase db, HttpContext context) => {
            var comment = await db.Comments.FindAsync(id);
            if (comment is null) return Results.NotFound();
            if (!Owns(context.User, comment.AuthorId)) return Results.Forbid();
            if (!TextValid(input.Content, 2000)) return Results.Problem(statusCode: 400, title: "댓글은 1~2000자로 입력해주세요.");
            comment.Content = input.Content!; comment.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/comments/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var comment = await db.Comments.FindAsync(id);
            if (comment is null) return Results.NotFound();
            if (!Owns(context.User, comment.AuthorId)) return Results.Forbid();
            db.Comments.Remove(comment); await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
    }
}
