using System.Text.Json;

namespace MedicalInfo;

public sealed class DrugCatalog
{
    private readonly Dictionary<string, JsonElement[]> products = new(StringComparer.Ordinal);
    private readonly string idField;
    private readonly string nameField;
    private readonly string companyField;
    private readonly string dateField;
    public bool Available { get; }
    public DrugCatalog(string? path, bool supplements = false)
    {
        idField = supplements ? "STTEMNT_NO" : "itemSeq";
        nameField = supplements ? "PRDUCT" : "itemName";
        companyField = supplements ? "ENTRPS" : "entpName";
        dateField = supplements ? "REGIST_DT" : "updateDe";
        if (string.IsNullOrWhiteSpace(path)) return;
        var rows = new List<JsonElement>();
        foreach (var line in File.ReadLines(path)) {
            using var doc = JsonDocument.Parse(line);
            var row = doc.RootElement;
            if (string.IsNullOrWhiteSpace(Text(row, idField)) || string.IsNullOrWhiteSpace(Text(row, nameField)))
                throw new InvalidDataException("Drug data requires product identifiers and names.");
            rows.Add(row.Clone());
        }
        if (rows.Count == 0) throw new InvalidDataException("Drug dataset is empty.");
        foreach (var group in rows.GroupBy(x => Text(x, idField)))
            products.Add(group.Key, group.DistinctBy(x => x.GetRawText()).OrderByDescending(x => Text(x, dateField), StringComparer.Ordinal).ToArray());
        Available = true;
    }
    public static string Text(JsonElement row, string field) => row.TryGetProperty(field, out var value) && value.ValueKind == JsonValueKind.String ? value.GetString() ?? "" : "";
    public object Search(string? keyword, int page)
    {
        if (string.IsNullOrWhiteSpace(keyword) || keyword.Length > 100 || page < 1 || page > 1000)
            throw new SearchInputException("검색어는 1~100자, 페이지는 1~1000으로 입력해주세요.");
        var terms = keyword.Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        var matches = products.Where(p => terms.All(t => p.Value.Any(r =>
            Text(r, nameField).Contains(t, StringComparison.OrdinalIgnoreCase) ||
            Text(r, companyField).Contains(t, StringComparison.OrdinalIgnoreCase) || p.Key.Contains(t, StringComparison.OrdinalIgnoreCase))))
            .OrderBy(p => Text(p.Value[0], nameField), StringComparer.Ordinal).ThenBy(p => p.Key, StringComparer.Ordinal).ToArray();
        return new { total = matches.Length, page, pageSize = 20, items = matches.Skip((page - 1) * 20).Take(20).Select(p => new {
            id = p.Key, name = Text(p.Value[0], nameField), company = Text(p.Value[0], companyField),
            date = Text(p.Value[0], dateField), dateLabel = dateField == "REGIST_DT" ? "등록일" : "수정일", variants = p.Value.Length
        }) };
    }
    public JsonElement[]? Find(string id) => products.GetValueOrDefault(id);
}
