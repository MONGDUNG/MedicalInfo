using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;

namespace MedicalInfo;

public sealed class FacilityPopularity
{
    public string Id { get; set; } = "";
    public long Views { get; set; }
}

// Short-lived deduplication only: no visitor identifier or IP is persisted in the DB.
public sealed class ViewDeduplication : IDisposable
{
    private readonly MemoryCache cache = new(new MemoryCacheOptions { SizeLimit = 10000 });
    private readonly SemaphoreSlim gate = new(1);
    private readonly byte[] salt = RandomNumberGenerator.GetBytes(32);
    public async Task Count(string id, string visitor, MemberDatabase db, CancellationToken ct)
    {
        var key = Convert.ToHexString(HMACSHA256.HashData(salt, Encoding.UTF8.GetBytes(visitor + "\n" + id)));
        await gate.WaitAsync(ct);
        try {
            if (cache.TryGetValue(key, out _)) return;
            await db.Database.ExecuteSqlInterpolatedAsync($"INSERT INTO FacilityPopularities (Id, Views) VALUES ({id}, 1) ON CONFLICT(Id) DO UPDATE SET Views = Views + 1", ct);
            cache.Set(key, true, new MemoryCacheEntryOptions { AbsoluteExpirationRelativeToNow = TimeSpan.FromMinutes(30), Size = 1 });
        } finally { gate.Release(); }
    }
    public void Dispose() { cache.Dispose(); gate.Dispose(); }
}

public static class Popularity
{
    public static void MapPopularity(this WebApplication app)
    {
        var group = app.MapGroup("/api/facilities").RequireRateLimiting("search");
        group.AddEndpointFilter(async (context, next) => {
            context.HttpContext.Response.Headers.CacheControl = "no-store";
            if (!HttpMethods.IsGet(context.HttpContext.Request.Method)) {
                try { await context.HttpContext.RequestServices.GetRequiredService<IAntiforgery>().ValidateRequestAsync(context.HttpContext); }
                catch (AntiforgeryValidationException) { return Results.BadRequest(); }
            }
            try { return await next(context); }
            catch (SearchInputException ex) { return Results.Problem(statusCode: 400, title: ex.Message); }
            catch (SearchUnavailableException) { return Results.Problem(statusCode: 503, title: "기관 정보를 확인할 수 없습니다."); }
        });
        group.MapPost("/{id}/view", async (string id, HttpContext context, MemberDatabase db, FacilitySearch search, ViewDeduplication dedup, CancellationToken ct) => {
            var facilities = await search.SearchAsync(SearchQuery.ById(id), ct);
            if (facilities.Count == 0) return Results.NotFound();
            await dedup.Count(facilities[0].Id, context.Connection.RemoteIpAddress?.ToString() ?? "unknown", db, ct);
            return Results.NoContent();
        });
        group.MapGet("/popular", async (MemberDatabase db, FacilitySearch search, CancellationToken ct) => {
            var ranks = await db.FacilityPopularities.AsNoTracking().OrderByDescending(x => x.Views).ThenBy(x => x.Id).Take(100).ToArrayAsync(ct);
            if (ranks.Length == 0) return Results.Ok(Array.Empty<object>());
            var query = new JsonObject { ["size"] = 100, ["query"] = JsonSerializer.SerializeToNode(new { ids = new { values = ranks.Select(x => x.Id).ToArray() } }) };
            var facilities = (await search.SearchAsync(query, ct)).ToDictionary(x => x.Id);
            return Results.Ok(ranks.Where(x => facilities.ContainsKey(x.Id)).Take(5).Select(x => new { facility = facilities[x.Id], views = x.Views }));
        });
    }
}
