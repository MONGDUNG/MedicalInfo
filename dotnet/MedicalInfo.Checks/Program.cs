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
await UpgradeChecks.Run(Check);
MailConfigurationChecks.Run(Check);
await NewsChecks.Run(Check);
var drugFixture = Path.GetTempFileName();
try {
    File.WriteAllText(drugFixture, """
{"itemSeq":"1","itemName":"시험약","entpName":"제약사","updateDe":"2025-01-01","efcyQesitm":"이전 정보"}
{"itemSeq":"1","itemName":"시험약","entpName":"제약사","updateDe":"2026-01-01","efcyQesitm":"현재 정보"}
""");
    var catalog = new DrugCatalog(drugFixture);
    Check(catalog.Find("1") is { Length: 2 } records && DrugCatalog.Text(records[0], "updateDe") == "2026-01-01", "conflicting drug records preserved newest first");
    Check(catalog.Find("missing") is null, "unknown drug not found");
    var hits = JsonSerializer.SerializeToElement(catalog.Search("시험약 제약사", 1));
    Check(hits.GetProperty("total").GetInt32() == 1, "drug search returns unique products matching all terms");
    Check(JsonSerializer.SerializeToElement(catalog.Search(" ", 1)).GetProperty("total").GetInt32()==1, "blank catalog search lists all products");
    Invalid(() => catalog.Search("시험", 0), "invalid drug page rejected");
    Check(!new DrugCatalog(null).Available, "unconfigured drug dataset marked unavailable");
    File.WriteAllText(drugFixture, """
{"STTEMNT_NO":"123","PRDUCT":"유산균","ENTRPS":"제조사","REGIST_DT":"20200101","MAIN_FNCTN":"공식 기능성"}
""");
    var supplementCatalog = new DrugCatalog(drugFixture, true);
    var supplementHits = JsonSerializer.SerializeToElement(supplementCatalog.Search("유산균", 1));
    Check(supplementHits.GetProperty("items")[0].GetProperty("dateLabel").GetString() == "등록일", "supplement registration date not labeled as revision date");
    Check(DrugCatalog.Text(supplementCatalog.Find("123")![0], "MAIN_FNCTN") == "공식 기능성", "supplement official functionality preserved");
} finally { File.Delete(drugFixture); }
Invalid(() => SearchQuery.Keyword(new string('a', 101)), "long keyword rejected");
Invalid(() => SearchQuery.Nearby(double.NaN, 127, 3, null), "nonfinite coordinate rejected");
Invalid(() => SearchQuery.Nearby(91, 127, 3, null), "out of range coordinate rejected");
Invalid(() => SearchQuery.Nearby(37, 127, -1, null), "invalid map level rejected");
var keyword = SearchQuery.Keyword("서울 한의원");
Check(keyword["query"]!["bool"]!["must"]!.AsArray().Count == 2, "all search words required");
Check(keyword["query"]!["bool"]!["must"]![0]!["match_phrase"]!["ADDRESS"]!.GetValue<string>() == "서울특별시", "leading region applies to address instead of facility name");
Check(keyword["query"]!["bool"]!["must_not"]!.AsArray().Count == 2, "Korean medicine exclusions retained");
var near = SearchQuery.Nearby(37.5, 127, 3, "의원", "내과");
Check(near["query"]!["bool"]!["filter"]![0]!["geo_distance"]!["distance"]!.GetValue<string>() == "640m", "distance has explicit meter units");
Check(near["sort"]![0]!["_geo_distance"]!["order"]!.GetValue<string>() == "asc", "nearest first ordering");

var handler = new StubHandler();
using var http = new HttpClient(handler) { BaseAddress = new Uri("http://localhost:9200/") };
var search = new FacilitySearch(http, Options.Create(new SearchOptions { ApiKey = "test-only" }));
handler.Body = """
{"hits":{"hits":[
{"_id":"1","_source":{"NAME":"테스트 병원","LAT":"37.5","LNG":127.0,"DEPT_NAME":["내과","외과"],"RAW_DETAIL":{"진료시작시간_월요일":900,"휴진안내_일요일":"휴진","암호화요양기호":"internal","응급실_주간_운영여부":"Y","점심시간_평일":null}}},
{"_id":"2","_source":{"NAME":"테스트 약국","DEPT_NAME":"약국"}}
]}}
""";
var results = await search.SearchAsync(keyword, default);
Check(results.Count == 2 && results[0].Latitude == 37.5, "numeric and string coordinates parsed");
Check(results[0].Departments.Length == 2 && results[1].Departments[0] == "약국", "scalar and array departments supported");
Check(results[1].Latitude is null, "missing coordinates do not become zero");
Check(results[0].Details!["진료시작시간_월요일"] == "900" && results[0].Details!["휴진안내_일요일"] == "휴진", "official numeric hours and closure notices preserved");
Check(results[0].Details!.Count == 2, "detail response excludes internal IDs, nulls, and emergency availability claims");
Check(results[1].Details!.Count == 0, "missing detail stays unavailable rather than implying closed");
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
            var requestBody = await new StreamReader(context.Request.InputStream).ReadToEndAsync();
            var fixture = requestBody.Contains("\"test-facility\"", StringComparison.Ordinal)
                ? "{\"hits\":{\"hits\":[{\"_id\":\"test-facility\",\"_source\":{\"NAME\":\"테스트 기관\"}}]}}" : "{\"hits\":{\"hits\":[]}}";
            var bytes = System.Text.Encoding.UTF8.GetBytes(fixture);
            await context.Response.OutputStream.WriteAsync(bytes);
            context.Response.Close();
        }
    } catch (Exception ex) when (ex is HttpListenerException or ObjectDisposedException) { }
});
var appPort = FreePort();
var accountState = Path.Combine(Path.GetTempPath(), "medicalinfo-checks-" + Guid.NewGuid());
var webRoot = Path.GetFullPath("../../../../MedicalInfo.Web", AppContext.BaseDirectory);
var start = new System.Diagnostics.ProcessStartInfo("dotnet") { UseShellExecute = false, CreateNoWindow = true };
start.ArgumentList.Add(Path.Combine(AppContext.BaseDirectory, "MedicalInfo.Web.dll"));
start.ArgumentList.Add("--contentRoot"); start.ArgumentList.Add(webRoot);
start.Environment["ASPNETCORE_URLS"] = $"http://localhost:{appPort}";
start.Environment["ASPNETCORE_ENVIRONMENT"] = "Development";
start.Environment["Elasticsearch__Url"] = $"http://localhost:{backendPort}";
start.Environment["Logging__LogLevel__Default"] = "Error";
start.Environment["State__Path"] = accountState;
using var process = System.Diagnostics.Process.Start(start)!;
try {
    using var client = new HttpClient { BaseAddress = new Uri($"http://localhost:{appPort}"), Timeout = TimeSpan.FromSeconds(3) };
    var ready = false;
    for (var attempt = 0; attempt < 40; attempt++) {
        if (process.HasExited) throw new Exception("Web process exited during startup.");
        try { using var health = await client.GetAsync("/health/live"); ready = health.IsSuccessStatusCode; }
        catch (HttpRequestException) { }
        catch (TaskCanceledException) { }
        if (ready) break;
        await Task.Delay(250);
    }
    Check(ready, "HTTP app starts without production data");
    await AccountChecks.Run(client.BaseAddress!, Check);
    await CommunityChecks.Run(client.BaseAddress!, Check);
    using var home = await client.GetAsync("/");
    Check(home.IsSuccessStatusCode && (await home.Content.ReadAsStringAsync()).Contains("병원·약국 찾기"), "search page served");
    Check(home.Headers.Contains("Content-Security-Policy"), "security headers served");
    var mapPolicy = string.Join(";", home.Headers.GetValues("Content-Security-Policy"));
    Check(mapPolicy.Contains("script-src 'self'") && mapPolicy.Contains("https://tile.openstreetmap.org"), "map images allowed while scripts remain same-origin");
    using var mapScript = await client.GetAsync("/vendor/leaflet/leaflet.js");
    Check(mapScript.IsSuccessStatusCode && (await mapScript.Content.ReadAsStringAsync()).Contains("1.9.4"), "bundled map library served without external script dependency");
    using var drugPage = await client.GetAsync("/drugs.html");
    Check(drugPage.IsSuccessStatusCode, "drug search page served");
    using var supplementSearch = await client.GetAsync("/api/supplements?keyword=" + Uri.EscapeDataString("유산균"));
    if (!string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("Supplements__Path"))) {
        Check(supplementSearch.IsSuccessStatusCode, "live supplement search succeeds");
        using var supplementResult = JsonDocument.Parse(await supplementSearch.Content.ReadAsStringAsync());
        Check(supplementResult.RootElement.GetProperty("total").GetInt32() > 0, "live supplement records found");
    } else Check(supplementSearch.StatusCode == HttpStatusCode.ServiceUnavailable, "missing supplement data returns 503");
    using var drugSearch = await client.GetAsync("/api/drugs?keyword=" + Uri.EscapeDataString("타이레놀"));
    if (!string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("Drugs__Path"))) {
        Check(drugSearch.IsSuccessStatusCode, "live drug search returns success");
        var drugResult = JsonDocument.Parse(await drugSearch.Content.ReadAsStringAsync());
        Check(drugResult.RootElement.GetProperty("total").GetInt32() > 0, "live Korean drug name found");
        var id = drugResult.RootElement.GetProperty("items")[0].GetProperty("id").GetString();
        using var drugDetail = await client.GetAsync("/api/drugs/" + id);
        Check(drugDetail.IsSuccessStatusCode && (await drugDetail.Content.ReadAsStringAsync()).Contains("useMethodQesitm"), "live drug detail includes official usage information");
    } else Check(drugSearch.StatusCode == HttpStatusCode.ServiceUnavailable, "missing drug data returns 503 instead of empty success");
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
    await PersistenceChecks.Run(client.BaseAddress!, Check, async () => {
        process.Kill(entireProcessTree: true); await process.WaitForExitAsync();
        process.Start();
    });
    await AdministrationChecks.Run(client.BaseAddress!, accountState, Check, async () => {
        process.Kill(entireProcessTree: true); await process.WaitForExitAsync(); process.Start();
    }, async username => {
        var command = new System.Diagnostics.ProcessStartInfo("dotnet") { UseShellExecute=false, CreateNoWindow=true, RedirectStandardOutput=true, RedirectStandardError=true };
        command.ArgumentList.Add(Path.Combine(AppContext.BaseDirectory,"MedicalInfo.Web.dll"));
        command.ArgumentList.Add("--grant-admin"); command.ArgumentList.Add(username);
        command.ArgumentList.Add("--contentRoot"); command.ArgumentList.Add(webRoot);
        foreach(var variable in start.Environment) command.Environment[variable.Key]=variable.Value;
        using var child=System.Diagnostics.Process.Start(command)!;
        var output=child.StandardOutput.ReadToEndAsync();var errors=child.StandardError.ReadToEndAsync();
        await child.WaitForExitAsync();await output;await errors;return child.ExitCode;
    });
    await InquiryChecks.Run(client.BaseAddress!, accountState, Check, async () => {
        process.Kill(entireProcessTree: true); await process.WaitForExitAsync();
        process.Start();
    });
    await RecoveryChecks.Run(client.BaseAddress!, accountState, Check, async enabled => {
        process.Kill(entireProcessTree:true); await process.WaitForExitAsync();
        start.Environment["Mail__Mode"]=enabled?"Pickup":"Disabled";
        start.Environment["Mail__From"]="test@example.invalid";
        start.Environment["Mail__RequireConfirmedEmail"]=enabled?"true":"false";
        start.Environment["Mail__PublicUrl"]=client.BaseAddress!.ToString().TrimEnd('/');
        process.Start();
    });
    var productDrugs=Path.Combine(accountState,"test-drugs.jsonl");var productSupplements=Path.Combine(accountState,"test-supplements.jsonl");
    await File.WriteAllTextAsync(productDrugs,"""
{"itemSeq":"one","itemName":"시험약","entpName":"시험업체","efcyQesitm":"해열"}
""");
    await File.WriteAllTextAsync(productSupplements,"""
{"STTEMNT_NO":"one","PRDUCT":"zeta","ENTRPS":"alpha","MAIN_FNCTN":"눈 건강"}
{"STTEMNT_NO":"two","PRDUCT":"alpha","ENTRPS":"beta","MAIN_FNCTN":"장 건강"}
""");
    await ProductChecks.Run(client.BaseAddress!,Check,async enabled=>{
        process.Kill(entireProcessTree:true);await process.WaitForExitAsync();
        start.Environment["Drugs__Path"]=enabled?productDrugs:"";start.Environment["Supplements__Path"]=enabled?productSupplements:"";
        process.Start();
    });
}
finally {
    if (!process.HasExited) { process.Kill(entireProcessTree: true); await process.WaitForExitAsync(); }
    backend.Stop(); await backendTask;
}
var liveUrl = Environment.GetEnvironmentVariable("MEDICALINFO_LIVE_ES_URL");
if (!string.IsNullOrWhiteSpace(liveUrl))
{
    using var liveHttp = new HttpClient { BaseAddress = new Uri(liveUrl.TrimEnd('/') + "/"), Timeout = TimeSpan.FromSeconds(10) };
    var liveSearch = new FacilitySearch(liveHttp, Options.Create(new SearchOptions {
        Index = Environment.GetEnvironmentVariable("MEDICALINFO_LIVE_ES_INDEX") ?? "item",
        ApiKey = Environment.GetEnvironmentVariable("MEDICALINFO_LIVE_ES_API_KEY")
    }));
    var hospitals = await liveSearch.SearchAsync(SearchQuery.Keyword("서울 내과"), default);
    Check(hospitals.Count is > 0 and <= 200, "live Korean keyword search returns bounded results");
    Check(hospitals.All(x => x.Address.StartsWith("서울특별시", StringComparison.Ordinal)), "live Seoul search excludes namesakes in other regions");
    var koreanMedicine = await liveSearch.SearchAsync(SearchQuery.Keyword("서울 한의원"), default);
    Check(koreanMedicine.Count > 0 && koreanMedicine.All(x => x.Category != "의원" && x.Category != "치과의원"), "live Korean medicine exclusions work");
    var nearby = await liveSearch.SearchAsync(SearchQuery.Nearby(37.5665, 126.978, 5, "약국"), default);
    Check(nearby.Count > 0 && nearby.All(x => x.Category == "약국" && x.Latitude.HasValue && x.Longitude.HasValue), "live nearby pharmacy search returns coordinates");
    double Distance(Facility facility)
    {
        const double radians = Math.PI / 180;
        var dlat = (facility.Latitude!.Value - 37.5665) * radians;
        var dlon = (facility.Longitude!.Value - 126.978) * radians;
        var a = Math.Pow(Math.Sin(dlat / 2), 2) + Math.Cos(37.5665 * radians) * Math.Cos(facility.Latitude.Value * radians) * Math.Pow(Math.Sin(dlon / 2), 2);
        return 6371008.8 * 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
    }
    var distances = nearby.Select(Distance).ToArray();
    Check(distances.All(x => x <= 2561) && distances.Zip(distances.Skip(1)).All(x => x.First <= x.Second + 1), "live results respect radius and nearest-first ordering");
    var department = await liveSearch.SearchAsync(SearchQuery.Nearby(37.5665, 126.978, 5, "의원", "내과"), default);
    Check(department.Count > 0 && department.All(x => x.Departments.Any(d => d.Contains("내과"))), "live department filter works");
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
