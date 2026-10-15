using System.Globalization;
using System.Net.Http.Headers;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Options;

namespace MedicalInfo;

public sealed class SearchOptions
{
    public string Url { get; set; } = "http://localhost:9200";
    public string Index { get; set; } = "item";
    public string? ApiKey { get; set; }
    public int TimeoutSeconds { get; set; } = 5;
}

public sealed record Facility(
    [property: JsonPropertyName("ID")] string Id,
    [property: JsonPropertyName("NAME")] string Name,
    [property: JsonPropertyName("ADDRESS")] string Address,
    [property: JsonPropertyName("PHONE")] string Phone,
    [property: JsonPropertyName("LAT")] double? Latitude,
    [property: JsonPropertyName("LNG")] double? Longitude,
    [property: JsonPropertyName("CATEGORY_NAME")] string Category,
    [property: JsonPropertyName("DEPT_NAME")] string[] Departments,
    [property: JsonPropertyName("SOURCE")] string Source = "",
    [property: JsonPropertyName("SOURCE_PERIOD")] string SourcePeriod = "",
    [property: JsonPropertyName("DETAILS")] Dictionary<string, string>? Details = null);

public sealed class SearchInputException(string message) : Exception(message);
public sealed class SearchUnavailableException(Exception inner) : Exception("Search backend is unavailable.", inner);

public static class SearchQuery
{
    private static JsonNode Node(object value) => JsonSerializer.SerializeToNode(value)!;

    public static JsonObject Keyword(string? keyword)
    {
        var text = keyword?.Trim() ?? "";
        if (text.Length is 0 or > 100)
            throw new SearchInputException("검색어는 1~100자로 입력해주세요.");
        var words = text.Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries);
        var regions = new Dictionary<string, string> {
            ["서울"] = "서울특별시", ["부산"] = "부산광역시", ["대구"] = "대구광역시",
            ["인천"] = "인천광역시", ["광주"] = "광주광역시", ["대전"] = "대전광역시",
            ["울산"] = "울산광역시", ["세종"] = "세종특별자치시", ["경기"] = "경기도",
            ["충북"] = "충청북도", ["충남"] = "충청남도", ["전남"] = "전라남도",
            ["경북"] = "경상북도", ["경남"] = "경상남도", ["제주"] = "제주특별자치도"
        };
        var must = new JsonArray();
        for (var i = 0; i < words.Length; i++) {
            var word = words[i];
            if (i == 0 && words.Length > 1 && regions.TryGetValue(word, out var address))
                must.Add(Node(new { match_phrase = new { ADDRESS = address } }));
            else must.Add(Node(new { multi_match = new { query = word,
                fields = new[] { "NAME", "ADDRESS", "CATEGORY_NAME", "DEPT_NAME" }, @operator = "and" } }));
        }
        var boolean = new JsonObject { ["must"] = must };
        if (words.Contains("한의원"))
            boolean["must_not"] = Node(new object[] {
                new { term = new { CATEGORY_NAME = "의원" } },
                new { term = new { CATEGORY_NAME = "치과의원" } }
            });
        return new JsonObject { ["size"] = 200, ["track_total_hits"] = false,
            ["query"] = new JsonObject { ["bool"] = boolean } };
    }

    public static JsonObject Nearby(double lat, double lng, int level, string? category, string? dept = null)
    {
        if (!double.IsFinite(lat) || lat is < -90 or > 90 ||
            !double.IsFinite(lng) || lng is < -180 or > 180)
            throw new SearchInputException("유효한 위도와 경도를 입력해주세요.");
        if (level is < 0 or > 14) throw new SearchInputException("지도 레벨은 0~14여야 합니다.");
        if (category?.Length > 80 || dept?.Length > 80)
            throw new SearchInputException("검색 조건은 80자 이하여야 합니다.");
        var radius = level <= 10 ? 80 * (1 << level) : 300000;
        var filters = new JsonArray(Node(new { geo_distance = new {
            distance = radius.ToString(CultureInfo.InvariantCulture) + "m", geoLocation = new { lat, lon = lng } } }));
        if (!string.IsNullOrWhiteSpace(category))
            filters.Add(Node(new { match_phrase = new { CATEGORY_NAME = category.Trim() } }));
        if (!string.IsNullOrWhiteSpace(dept))
            filters.Add(Node(new { match_phrase = new { DEPT_NAME = dept.Trim() } }));
        return new JsonObject { ["size"] = 200, ["track_total_hits"] = false,
            ["query"] = new JsonObject { ["bool"] = new JsonObject { ["filter"] = filters } },
            ["sort"] = Node(new object[] { new { _geo_distance = new {
                geoLocation = new { lat, lon = lng }, order = "asc", unit = "m", distance_type = "arc" } } }) };
    }
}

public sealed class FacilitySearch(HttpClient http, IOptions<SearchOptions> options)
{
    public async Task<IReadOnlyList<Facility>> SearchAsync(JsonObject query, CancellationToken cancellationToken)
    {
        try
        {
            using var request = new HttpRequestMessage(HttpMethod.Post,
                Uri.EscapeDataString(options.Value.Index) + "/_search") { Content = JsonContent.Create(query) };
            if (!string.IsNullOrEmpty(options.Value.ApiKey))
                request.Headers.Authorization = new AuthenticationHeaderValue("ApiKey", options.Value.ApiKey);
            using var response = await http.SendAsync(request, cancellationToken);
            response.EnsureSuccessStatusCode();
            using var document = JsonDocument.Parse(await response.Content.ReadAsStringAsync(cancellationToken));
            var root = document.RootElement;
            if (root.TryGetProperty("timed_out", out var timedOut) && timedOut.GetBoolean())
                throw new JsonException("Incomplete search response.");
            if (root.TryGetProperty("_shards", out var shards) &&
                shards.TryGetProperty("failed", out var failed) && failed.GetInt32() > 0)
                throw new JsonException("Search shards failed.");
            return root.GetProperty("hits").GetProperty("hits").EnumerateArray().Select(Parse).ToArray();
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested) { throw; }
        catch (Exception ex) when (ex is HttpRequestException or JsonException or InvalidOperationException
            or KeyNotFoundException or FormatException or OperationCanceledException)
        { throw new SearchUnavailableException(ex); }
    }

    private static Facility Parse(JsonElement hit)
    {
        var source = hit.GetProperty("_source");
        string Text(string field) => source.TryGetProperty(field, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()! : "";
        double? Number(string field)
        {
            if (!source.TryGetProperty(field, out var value)) return null;
            if (value.ValueKind == JsonValueKind.Number && value.TryGetDouble(out var number) && double.IsFinite(number)) return number;
            if (value.ValueKind == JsonValueKind.String && double.TryParse(value.GetString(), NumberStyles.Float,
                CultureInfo.InvariantCulture, out number) && double.IsFinite(number)) return number;
            return null;
        }
        var departments = Array.Empty<string>();
        if (source.TryGetProperty("DEPT_NAME", out var dept))
            departments = dept.ValueKind == JsonValueKind.Array
                ? dept.EnumerateArray().Where(v => v.ValueKind == JsonValueKind.String).Select(v => v.GetString()!).ToArray()
                : dept.ValueKind == JsonValueKind.String ? [dept.GetString()!] : [];
        var details = new Dictionary<string, string>();
        if (source.TryGetProperty("RAW_DETAIL", out var raw) && raw.ValueKind == JsonValueKind.Object)
            foreach (var field in raw.EnumerateObject()) {
                if (!new[] { "진료시작시간_", "진료종료시간_", "점심시간_", "접수시간_", "휴진안내_", "주차_", "위치_" }
                    .Any(prefix => field.Name.StartsWith(prefix, StringComparison.Ordinal))) continue;
                if (field.Value.ValueKind is not (JsonValueKind.String or JsonValueKind.Number)) continue;
                var value = field.Value.ToString().Trim();
                if (value.Length > 0) details[field.Name] = value;
            }
        return new Facility(hit.GetProperty("_id").GetString()!, Text("NAME"), Text("ADDRESS"),
            Text("PHONE"), Number("LAT"), Number("LNG"), Text("CATEGORY_NAME"), departments, Text("SOURCE"), Text("SOURCE_PERIOD"), details);
    }
}
