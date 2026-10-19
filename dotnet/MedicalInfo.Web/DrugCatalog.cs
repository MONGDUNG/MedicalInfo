using System.Text.Json;
using System.Security.Cryptography;
using System.Globalization;
using System.Xml;
using System.Xml.Linq;

namespace MedicalInfo;

public sealed class DrugCatalog
{
    private readonly Dictionary<string, JsonElement[]> products = new(StringComparer.Ordinal);
    private readonly Dictionary<string, JsonElement[]> permits = new(StringComparer.Ordinal);
    private readonly Dictionary<string, JsonElement[]> permitDetails = new(StringComparer.Ordinal);
    private readonly Dictionary<string,string[]> permitEffects = new(StringComparer.Ordinal);
    private readonly string idField;
    private readonly string nameField;
    private readonly string companyField;
    private readonly string dateField;
    private readonly string effectField;
    public bool Available { get; }
    public bool PermitDataAvailable { get; }
    public DateTimeOffset? PermitCollectedAt { get; }
    public DateTimeOffset? PermitDetailsCollectedAt { get; }
    public DrugCatalog(string? path, bool supplements = false, string? permitsPath = null)
    {
        idField = supplements ? "STTEMNT_NO" : "itemSeq";
        nameField = supplements ? "PRDUCT" : "itemName";
        companyField = supplements ? "ENTRPS" : "entpName";
        dateField = supplements ? "REGIST_DT" : "updateDe";
        effectField = supplements ? "MAIN_FNCTN" : "efcyQesitm";
        if (!string.IsNullOrWhiteSpace(path)) {
            foreach (var group in Read(path, idField, nameField).GroupBy(x => Text(x, idField)))
                products.Add(group.Key, group.DistinctBy(x => x.GetRawText()).OrderByDescending(x => Text(x, dateField), StringComparer.Ordinal).ToArray());
        }
        if (!string.IsNullOrWhiteSpace(permitsPath)) {
            if (supplements) throw new InvalidDataException("Drug permits cannot be used as supplement data.");
            var manifest=Path.Combine(Path.GetDirectoryName(Path.GetFullPath(permitsPath))!,"manifest.json");
            if(File.Exists(manifest)) {
                using var metadata=JsonDocument.Parse(File.ReadAllText(manifest));
                using var stream=File.OpenRead(permitsPath);
                if(!string.Equals(Convert.ToHexString(SHA256.HashData(stream)),Text(metadata.RootElement,"sha256"),StringComparison.OrdinalIgnoreCase) ||
                    Text(metadata.RootElement,"status")!="complete" ||
                    !DateTimeOffset.TryParse(Text(metadata.RootElement,"collectedAt"),CultureInfo.InvariantCulture,DateTimeStyles.None,out var collected))
                    throw new InvalidDataException("Drug permit metadata/checksum is invalid.");
                PermitCollectedAt=collected;
            }
            var rows = Read(permitsPath, "ITEM_SEQ", "ITEM_NAME");
            if (rows.Any(x => Text(x, "SPCLTY_PBLC") is not ("일반의약품" or "일반")))
                throw new InvalidDataException("Only explicitly classified OTC products are accepted.");
            foreach (var group in rows.GroupBy(x => Text(x, "ITEM_SEQ")))
                permits.Add(group.Key, group.DistinctBy(x => x.GetRawText()).ToArray());
            var detailsPath=Path.Combine(Path.GetDirectoryName(Path.GetFullPath(permitsPath))!,"permit-details.jsonl");
            if(File.Exists(detailsPath)) {
                using var metadata=JsonDocument.Parse(File.ReadAllText(manifest));using var stream=File.OpenRead(detailsPath);
                if(!string.Equals(Convert.ToHexString(SHA256.HashData(stream)),Text(metadata.RootElement,"detailsSha256"),StringComparison.OrdinalIgnoreCase) ||
                    !DateTimeOffset.TryParse(Text(metadata.RootElement,"detailCollectedAt"),CultureInfo.InvariantCulture,DateTimeStyles.None,out var collected))
                    throw new InvalidDataException("Drug permit detail metadata/checksum is invalid.");
                PermitDetailsCollectedAt=collected;
                var details=Read(detailsPath,"ITEM_SEQ","ITEM_NAME");
                if(details.Any(r=>!permits.ContainsKey(Text(r,"ITEM_SEQ")) || Text(r,"ETC_OTC_CODE") is not ("일반의약품" or "일반")))
                    throw new InvalidDataException("Permit details must refer to known OTC identifiers.");
                foreach(var group in details.GroupBy(r=>Text(r,"ITEM_SEQ"))) {
                    var variants=group.DistinctBy(r=>r.GetRawText()).ToArray();permitDetails.Add(group.Key,variants);
                    permitEffects.Add(group.Key,variants.Select(r=>DocumentText(Text(r,"EE_DOC_DATA"))).ToArray());
                }
            }
            PermitDataAvailable = true;
        }
        Available = products.Count > 0 || permits.Count > 0;
    }
    private static JsonElement[] Read(string path, string id, string name) {
        var rows = new List<JsonElement>();
        foreach (var line in File.ReadLines(path)) {
            using var doc = JsonDocument.Parse(line);
            var row = doc.RootElement;
            if (row.ValueKind != JsonValueKind.Object || string.IsNullOrWhiteSpace(Text(row,id)) || string.IsNullOrWhiteSpace(Text(row,name)))
                throw new InvalidDataException("Drug data requires product identifiers and names.");
            rows.Add(row.Clone());
        }
        if (rows.Count == 0) throw new InvalidDataException("Drug dataset is empty.");
        return rows.ToArray();
    }
    public static string Text(JsonElement row, string field) => row.TryGetProperty(field, out var value) && value.ValueKind == JsonValueKind.String ? value.GetString() ?? "" : "";
    public static string DocumentText(string value) {
        if(string.IsNullOrWhiteSpace(value))return "";
        try {
            using var reader=XmlReader.Create(new StringReader(value),new XmlReaderSettings { DtdProcessing=DtdProcessing.Prohibit,XmlResolver=null,MaxCharactersInDocument=5_000_000 });
            var root=XDocument.Load(reader).Root;
            return root is null?"":System.Net.WebUtility.HtmlDecode(root.Value);
        } catch(XmlException) { return ""; }
    }
    public object Search(string? keyword, int page, string? effect = null, string? sort = null, IReadOnlyDictionary<string,int>? votes = null, string? ingredient = null)
    {
        if (keyword?.Length > 100 || effect?.Length > 100 || ingredient?.Length > 100 || page < 1 || page > 100000 || sort is not (null or "" or "name" or "popular"))
            throw new SearchInputException("검색어·효능·주성분 필터는 100자 이하, 페이지는 1~100000이며 유효한 정렬이 필요합니다.");
        var terms = (keyword??"").Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        var effectTerm=effect?.Trim()??"";
        var ingredientTerm=ingredient?.Trim()??"";
        if (ingredientTerm.Length > 0 && !PermitDataAvailable) throw new SearchInputException("주성분 데이터가 연결되지 않았습니다.");
        JsonElement[] Easy(string id) => products.GetValueOrDefault(id) ?? [];
        string Name(string id) => products.TryGetValue(id,out var rows) ? Text(rows[0],nameField) : Text(permits[id][0],"ITEM_NAME");
        string Company(string id) => products.TryGetValue(id,out var rows) ? Text(rows[0],companyField) : Text(permits[id][0],"ENTP_NAME");
        var matches = products.Keys.Concat(permits.Keys).Distinct(StringComparer.Ordinal).Where(id => terms.All(t => Easy(id).Any(r =>
            Text(r, nameField).Contains(t, StringComparison.OrdinalIgnoreCase) ||
            Text(r, companyField).Contains(t, StringComparison.OrdinalIgnoreCase)) || id.Contains(t, StringComparison.OrdinalIgnoreCase) ||
            FindPermits(id).Any(r => new[]{"ITEM_NAME","ENTP_NAME","ITEM_INGR_NAME"}.Any(f=>Text(r,f).Contains(t,StringComparison.OrdinalIgnoreCase))) ||
            FindPermitDetails(id).Any(r=>Text(r,"MAIN_ITEM_INGR").Contains(t,StringComparison.OrdinalIgnoreCase))) &&
            (effectTerm.Length==0 || Easy(id).Any(r=>Text(r,effectField).Contains(effectTerm,StringComparison.OrdinalIgnoreCase)) ||
                (permitEffects.GetValueOrDefault(id)??[]).Any(text=>text.Contains(effectTerm,StringComparison.OrdinalIgnoreCase))) &&
            (ingredientTerm.Length==0 || FindPermits(id).Any(r=>Text(r,"ITEM_INGR_NAME").Contains(ingredientTerm,StringComparison.OrdinalIgnoreCase)) ||
                FindPermitDetails(id).Any(r=>Text(r,"MAIN_ITEM_INGR").Contains(ingredientTerm,StringComparison.OrdinalIgnoreCase))))
            .OrderByDescending(id=>sort=="popular" ? votes?.GetValueOrDefault(id)??0 : 0)
            .ThenBy(Name, StringComparer.Ordinal).ThenBy(id => id, StringComparer.Ordinal).ToArray();
        return new { total = matches.Length, page, pageSize = 20, permitDataAvailable=PermitDataAvailable, permitCollectedAt=PermitCollectedAt, items = matches.Skip((page - 1) * 20).Take(20).Select(id => new {
            id, name = Name(id), company = Company(id),
            date = products.TryGetValue(id,out var rows) ? Text(rows[0],dateField) : Text(permits[id][0],"ITEM_PERMIT_DATE"),
            dateLabel = !products.ContainsKey(id) ? "허가일" : dateField == "REGIST_DT" ? "등록일" : "수정일",
            variants = Easy(id).Length, hasEasyInformation=products.ContainsKey(id),
            ingredients=FindPermits(id).Select(r=>Text(r,"ITEM_INGR_NAME")).Where(x=>x.Length>0).Distinct(),
            koreanIngredients=FindPermitDetails(id).Select(r=>Text(r,"MAIN_ITEM_INGR")).Where(x=>x.Length>0).Distinct(),
            permitStatuses=FindPermits(id).Select(r=>Text(r,"CANCEL_NAME")).Where(x=>x.Length>0).Distinct(),
            recommendations=votes?.GetValueOrDefault(id)??0
        }) };
    }
    public JsonElement[] FindPermits(string id) => permits.GetValueOrDefault(id) ?? [];
    public JsonElement[] FindPermitDetails(string id) => permitDetails.GetValueOrDefault(id) ?? [];
    public JsonElement[]? Find(string id) => products.GetValueOrDefault(id) ?? (permits.ContainsKey(id) ? [] : null);
}
