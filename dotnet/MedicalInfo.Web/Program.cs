using System.Threading.RateLimiting;
using MedicalInfo;
using Microsoft.Extensions.Options;

if (args is ["--health-check"])
{
    try {
        using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
        using var response = await client.GetAsync("http://127.0.0.1:8080/health/live");
        Environment.Exit(response.IsSuccessStatusCode ? 0 : 1);
    } catch (HttpRequestException) { Environment.Exit(1); }
    catch (OperationCanceledException) { Environment.Exit(1); }
    return;
}
var builder = WebApplication.CreateBuilder(args);
var drugs = new DrugCatalog(builder.Configuration["Drugs:Path"]);
builder.Services.AddProblemDetails();
builder.Services.Configure<Microsoft.AspNetCore.Routing.RouteHandlerOptions>(options => options.ThrowOnBadRequest = false);
builder.Services.AddOptions<SearchOptions>().BindConfiguration("Elasticsearch")
    .Validate(o => Uri.TryCreate(o.Url, UriKind.Absolute, out var uri) &&
        uri.Scheme is "http" or "https" && string.IsNullOrEmpty(uri.UserInfo), "Invalid Elasticsearch URL.")
    .Validate(o => System.Text.RegularExpressions.Regex.IsMatch(o.Index, "^[a-z0-9][a-z0-9_-]{0,127}$"), "Invalid index name.")
    .Validate(o => o.TimeoutSeconds is >= 1 and <= 30, "Invalid search timeout.").ValidateOnStart();
builder.Services.AddHttpClient<FacilitySearch>((services, client) => {
    var options = services.GetRequiredService<IOptions<SearchOptions>>().Value;
    client.BaseAddress = new Uri(options.Url.TrimEnd('/') + "/");
    client.Timeout = TimeSpan.FromSeconds(options.TimeoutSeconds);
});
builder.Services.AddRateLimiter(options => {
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.AddPolicy("search", context => RateLimitPartition.GetFixedWindowLimiter(
        context.Connection.RemoteIpAddress?.ToString() ?? "unknown",
        _ => new FixedWindowRateLimiterOptions { PermitLimit = 60, Window = TimeSpan.FromMinutes(1), QueueLimit = 0 }));
});
var app = builder.Build();
app.UseExceptionHandler();
app.Use(async (context, next) => {
    context.Response.Headers.XContentTypeOptions = "nosniff";
    context.Response.Headers["Referrer-Policy"] = "strict-origin-when-cross-origin";
    context.Response.Headers.ContentSecurityPolicy = "default-src 'self'; style-src 'self'; script-src 'self'; frame-ancestors 'none'";
    await next();
});
app.UseRateLimiter();
app.UseDefaultFiles();
app.UseStaticFiles();
app.MapGet("/api/drugs", (string? keyword, int? page) => {
    if (!drugs.Available) return Results.Problem(statusCode: 503, title: "의약품 데이터를 준비 중입니다.");
    try { return Results.Ok(drugs.Search(keyword, page ?? 1)); }
    catch (SearchInputException ex) { return Results.Problem(statusCode: 400, title: ex.Message); }
}).RequireRateLimiting("search");
app.MapGet("/api/drugs/{id}", (string id) => {
    if (!drugs.Available) return Results.Problem(statusCode: 503, title: "의약품 데이터를 준비 중입니다.");
    var variants = drugs.Find(id);
    return variants is null ? Results.NotFound() : Results.Ok(new { source = "https://www.data.go.kr/data/15075057/openapi.do", variants });
}).RequireRateLimiting("search");
app.MapGet("/health/live", () => Results.Ok(new { status = "alive" }));
app.MapGet("/health/ready", async (FacilitySearch search, CancellationToken ct) =>
    await Run(search, () => new System.Text.Json.Nodes.JsonObject { ["size"] = 0,
        ["query"] = new System.Text.Json.Nodes.JsonObject { ["match_all"] = new System.Text.Json.Nodes.JsonObject() } }, ct))
    .RequireRateLimiting("search");
app.MapGet("/map/search/item", async (string? keyword, FacilitySearch search, CancellationToken ct) =>
    await Run(search, () => SearchQuery.Keyword(keyword), ct)).RequireRateLimiting("search");
app.MapGet("/map/nearbyHospitals", async (double lat, double lng, int level, string? category, FacilitySearch search, CancellationToken ct) =>
    await Run(search, () => SearchQuery.Nearby(lat, lng, level, category), ct)).RequireRateLimiting("search");
app.MapGet("/map/medicalFacilityByDept", async (double lat, double lng, int level, string? category, string? dept, FacilitySearch search, CancellationToken ct) =>
    await Run(search, () => SearchQuery.Nearby(lat, lng, level, category, dept), ct)).RequireRateLimiting("search");
app.Run();

static async Task<IResult> Run(FacilitySearch search, Func<System.Text.Json.Nodes.JsonObject> query, CancellationToken ct)
{
    try { return Results.Ok(await search.SearchAsync(query(), ct)); }
    catch (SearchInputException ex) { return Results.Problem(statusCode: 400, title: ex.Message); }
    catch (SearchUnavailableException) { return Results.Problem(statusCode: 503, title: "검색 서비스를 일시적으로 이용할 수 없습니다."); }
}

public partial class Program;
