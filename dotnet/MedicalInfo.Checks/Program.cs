using System.Net;
using System.Text.Json;
using MedicalInfo;
using Microsoft.Extensions.Options;

var passed = 0;
void Check(bool condition, string message)
{
    if (!condition) throw new Exception(message);
    Console.WriteLine("PASS " + message); passed++;
}
void Invalid(Action action, string name)
{
    try { action(); throw new Exception("Expected input rejection: " + name); }
    catch (SearchInputException) { Check(true, name); }
}
Invalid(() => SearchQuery.Keyword("  "), "blank keyword rejected");
Invalid(() => SearchQuery.Keyword(new string('a', 101)), "long keyword rejected");
Invalid(() => SearchQuery.Nearby(double.NaN, 127, 3, null), "nonfinite coordinate rejected");
Invalid(() => SearchQuery.Nearby(91, 127, 3, null), "out of range coordinate rejected");
Invalid(() => SearchQuery.Nearby(37, 127, -1, null), "invalid map level rejected");
var keyword = SearchQuery.Keyword("서울 한의원");
Check(keyword["query"]!["bool"]!["must"]!.AsArray().Count == 2, "all search words required");
Check(keyword["query"]!["bool"]!["must_not"]!.AsArray().Count == 2, "Korean medicine exclusions retained");
var near = SearchQuery.Nearby(37.5, 127, 3, "의원", "내과");
Check(near["query"]!["bool"]!["filter"]![0]!["geo_distance"]!["distance"]!.GetValue<string>() == "640m", "distance has explicit meter units");
Check(near["sort"]![0]!["_geo_distance"]!["order"]!.GetValue<string>() == "asc", "nearest first ordering");

var handler = new StubHandler();
using var http = new HttpClient(handler) { BaseAddress = new Uri("http://localhost:9200/") };
var search = new FacilitySearch(http, Options.Create(new SearchOptions { ApiKey = "test-only" }));
handler.Body = """
{"hits":{"hits":[
{"_id":"1","_source":{"NAME":"테스트 병원","LAT":"37.5","LNG":127.0,"DEPT_NAME":["내과","외과"]}},
{"_id":"2","_source":{"NAME":"테스트 약국","DEPT_NAME":"약국"}}
]}}
""";
var results = await search.SearchAsync(keyword, default);
Check(results.Count == 2 && results[0].Latitude == 37.5, "numeric and string coordinates parsed");
Check(results[0].Departments.Length == 2 && results[1].Departments[0] == "약국", "scalar and array departments supported");
Check(results[1].Latitude is null, "missing coordinates do not become zero");
Check(handler.Path == "/item/_search" && handler.Authorization == "ApiKey test-only", "configured index and API authentication used");
var json = JsonSerializer.Serialize(results[0]);
Check(json.Contains("\"NAME\"") && json.Contains("\"DEPT_NAME\""), "legacy response field casing preserved");
foreach (var (status, body) in new[] {
    (HttpStatusCode.ServiceUnavailable, "unavailable"),
    (HttpStatusCode.OK, "invalid JSON"),
    (HttpStatusCode.OK, "{\"timed_out\":true,\"hits\":{\"hits\":[]}}"),
    (HttpStatusCode.OK, "{\"_shards\":{\"failed\":1},\"hits\":{\"hits\":[]}}") })
{
    handler.Status = status; handler.Body = body;
    try { await search.SearchAsync(keyword, default); throw new Exception("Expected unavailable"); }
    catch (SearchUnavailableException) { Check(true, "backend failure is not reported as empty success"); }
}
handler.ThrowCancellation = true;
using var cancellation = new CancellationTokenSource(); cancellation.Cancel();
try { await search.SearchAsync(keyword, cancellation.Token); throw new Exception("Expected cancellation"); }
catch (OperationCanceledException) { Check(true, "caller cancellation preserved"); }
// Exercise the real web process against a local HTTP Elasticsearch fixture.
int FreePort()
{
    var listener = new System.Net.Sockets.TcpListener(IPAddress.Loopback, 0);
    listener.Start(); var port = ((IPEndPoint)listener.LocalEndpoint).Port; listener.Stop(); return port;
}
var backendPort = FreePort();
using var backend = new HttpListener();
backend.Prefixes.Add($"http://localhost:{backendPort}/"); backend.Start();
var backendStatus = 200;
var backendTask = Task.Run(async () => {
    try {
        while (backend.IsListening) {
            var context = await backend.GetContextAsync();
            context.Response.StatusCode = Volatile.Read(ref backendStatus);
            context.Response.ContentType = "application/json";
            var bytes = System.Text.Encoding.UTF8.GetBytes("{\"hits\":{\"hits\":[]}}");
            await context.Response.OutputStream.WriteAsync(bytes);
            context.Response.Close();
        }
    } catch (Exception ex) when (ex is HttpListenerException or ObjectDisposedException) { }
});
var appPort = FreePort();
var webRoot = Path.GetFullPath("../../../../MedicalInfo.Web", AppContext.BaseDirectory);
var start = new System.Diagnostics.ProcessStartInfo("dotnet") { UseShellExecute = false, CreateNoWindow = true };
start.ArgumentList.Add(Path.Combine(AppContext.BaseDirectory, "MedicalInfo.Web.dll"));
start.ArgumentList.Add("--contentRoot"); start.ArgumentList.Add(webRoot);
start.Environment["ASPNETCORE_URLS"] = $"http://localhost:{appPort}";
start.Environment["ASPNETCORE_ENVIRONMENT"] = "Development";
start.Environment["Elasticsearch__Url"] = $"http://localhost:{backendPort}";
start.Environment["Logging__LogLevel__Default"] = "Error";
using var process = System.Diagnostics.Process.Start(start)!;
try {
    using var client = new HttpClient { BaseAddress = new Uri($"http://localhost:{appPort}"), Timeout = TimeSpan.FromSeconds(3) };
    var ready = false;
    for (var attempt = 0; attempt < 40; attempt++) {
        if (process.HasExited) throw new Exception("Web process exited during startup.");
        try { using var health = await client.GetAsync("/health/live"); ready = health.IsSuccessStatusCode; }
        catch (HttpRequestException) { }
        if (ready) break;
        await Task.Delay(250);
    }
    Check(ready, "HTTP app starts without production data");
    using var home = await client.GetAsync("/");
    Check(home.IsSuccessStatusCode && (await home.Content.ReadAsStringAsync()).Contains("병원·약국 찾기"), "search page served");
    Check(home.Headers.Contains("Content-Security-Policy"), "security headers served");
    using var invalid = await client.GetAsync("/map/search/item?keyword=%20");
    Check(invalid.StatusCode == HttpStatusCode.BadRequest, "invalid HTTP input returns 400");
    using var valid = await client.GetAsync("/map/search/item?keyword=test");
    Check(valid.IsSuccessStatusCode && await valid.Content.ReadAsStringAsync() == "[]", "HTTP search routes to backend");
    using var missing = await client.GetAsync("/map/nearbyHospitals");
    Check(missing.StatusCode == HttpStatusCode.BadRequest, "missing coordinates return 400");
    Volatile.Write(ref backendStatus, 503);
    using var unavailable = await client.GetAsync("/map/search/item?keyword=test");
    Check(unavailable.StatusCode == HttpStatusCode.ServiceUnavailable, "backend outage returns HTTP 503");
    using var readiness = await client.GetAsync("/health/ready");
    Check(readiness.StatusCode == HttpStatusCode.ServiceUnavailable, "readiness detects backend failure");
    HttpStatusCode last = HttpStatusCode.OK;
    for (var i = 0; i < 65; i++) { using var response = await client.GetAsync("/map/search/item?keyword=%20"); last = response.StatusCode; }
    Check(last == HttpStatusCode.TooManyRequests, "HTTP search rate limit enforced");
}
finally {
    if (!process.HasExited) { process.Kill(entireProcessTree: true); await process.WaitForExitAsync(); }
    backend.Stop(); await backendTask;
}
Console.WriteLine($"{passed} checks passed.");

sealed class StubHandler : HttpMessageHandler
{
    public string Body { get; set; } = "";
    public HttpStatusCode Status { get; set; } = HttpStatusCode.OK;
    public string? Path { get; private set; }
    public string? Authorization { get; private set; }
    public bool ThrowCancellation { get; set; }
    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        if (ThrowCancellation) throw new OperationCanceledException(cancellationToken);
        Path = request.RequestUri!.AbsolutePath;
        Authorization = request.Headers.Authorization?.ToString();
        return Task.FromResult(new HttpResponseMessage(Status) { Content = new StringContent(Body) });
    }
}
