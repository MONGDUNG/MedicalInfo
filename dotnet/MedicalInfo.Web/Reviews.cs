using System.Security.Claims;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class FacilityReview
{
    public long Id { get; set; }
    public string FacilityId { get; set; } = "";
    public string? AuthorId { get; set; }
    public Member? Author { get; set; }
    public int Rating { get; set; }
    public string Content { get; set; } = "";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? UpdatedAt { get; set; }
}
public record ReviewInput(string? FacilityId, int Rating, string? Content);
public static class Reviews
{
    private static bool Valid(ReviewInput input) => input.Rating is >= 1 and <= 5 && !string.IsNullOrWhiteSpace(input.Content) && input.Content.Length <= 2000;
    private static bool Owns(HttpContext context, FacilityReview review) => context.User.FindFirstValue(ClaimTypes.NameIdentifier) == review.AuthorId || context.User.IsInRole("Admin");
    private static IResult Invalid() => Results.Problem(statusCode: 400, title: "별점은 1~5점, 리뷰는 1~2000자로 입력해주세요.");
    public static void MapReviews(this WebApplication app)
    {
        var group = app.MapGroup("/api/reviews").RequireRateLimiting("search");
        group.AddEndpointFilter(async (context, next) => {
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            if (!HttpMethods.IsGet(context.HttpContext.Request.Method)) {
                try { await context.HttpContext.RequestServices.GetRequiredService<IAntiforgery>().ValidateRequestAsync(context.HttpContext); }
                catch (AntiforgeryValidationException) { return Results.Problem(statusCode: 400, title: "보안 확인이 만료되었습니다. 새로고침해주세요."); }
            }
            try { return await next(context); }
            catch (SearchInputException ex) { return Results.Problem(statusCode: 400, title: ex.Message); }
            catch (SearchUnavailableException) { return Results.Problem(statusCode: 503, title: "기관 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."); }
        });
        group.MapGet("", async (string? facilityId, int? page, MemberDatabase db, FacilitySearch search, HttpContext context, CancellationToken ct) => {
            var current = page ?? 1;
            if (current is < 1 or > 100000) return Invalid();
            var facilities = await search.SearchAsync(SearchQuery.ById(facilityId), ct);
            if (facilities.Count == 0) return Results.NotFound();
            var query = db.Reviews.AsNoTracking().Where(x => x.FacilityId == facilityId);
            var summary = await query.GroupBy(x => 1).Select(g => new { count = g.Count(), average = g.Average(x => x.Rating) }).SingleOrDefaultAsync(ct);
            var items = await query.OrderByDescending(x => x.Id).Skip((current - 1) * 20).Take(20).Include(x => x.Author).ToArrayAsync(ct);
            return Results.Ok(new { facility = facilities[0], total = summary?.count ?? 0, average = summary?.average, page = current, pageSize = 20,
                items = items.Select(x => new { x.Id, x.Rating, x.Content, author = x.Author?.DisplayName ?? "탈퇴 회원", x.CreatedAt, x.UpdatedAt,
                    canEdit = context.User.Identity?.IsAuthenticated == true && Owns(context, x) }) });
        });
        group.MapPost("", async (ReviewInput input, MemberDatabase db, FacilitySearch search, HttpContext context, CancellationToken ct) => {
            if (!Valid(input)) return Invalid();
            var facilities = await search.SearchAsync(SearchQuery.ById(input.FacilityId), ct);
            if (facilities.Count == 0) return Results.NotFound();
            var review = new FacilityReview { FacilityId = facilities[0].Id, AuthorId = context.User.FindFirstValue(ClaimTypes.NameIdentifier), Rating = input.Rating, Content = input.Content! };
            db.Reviews.Add(review);
            try { await db.SaveChangesAsync(ct); }
            catch (DbUpdateException ex) when (ex.InnerException is SqliteException { SqliteErrorCode: 19 }) {
                return Results.Conflict(new { title = "이 기관에 이미 리뷰를 작성했습니다. 기존 리뷰를 수정해주세요." });
            }
            return Results.Created("/api/reviews?facilityId="+Uri.EscapeDataString(review.FacilityId), new { review.Id });
        }).RequireAuthorization();
        group.MapPut("/{id:long}", async (long id, ReviewInput input, MemberDatabase db, HttpContext context) => {
            var review = await db.Reviews.FindAsync(id);
            if (review is null) return Results.NotFound();
            if (!Owns(context, review)) return Results.Forbid();
            if (!Valid(input) || input.FacilityId != review.FacilityId) return Invalid();
            review.Rating = input.Rating; review.Content = input.Content!; review.UpdatedAt = DateTime.UtcNow;
            await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/{id:long}", async (long id, MemberDatabase db, HttpContext context) => {
            var review = await db.Reviews.FindAsync(id);
            if (review is null) return Results.NotFound();
            if (!Owns(context, review)) return Results.Forbid();
            db.Reviews.Remove(review); await db.SaveChangesAsync(); return Results.NoContent();
        }).RequireAuthorization();
    }
}
