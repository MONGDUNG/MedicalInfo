using System.Globalization;
using System.Xml;
using System.Xml.Linq;

namespace MedicalInfo;

public record NewsItem(string Title, string Link, DateTimeOffset? PublishedAt);
public record NewsResult(IReadOnlyList<NewsItem> Items, DateTimeOffset FetchedAt, bool Stale);

public sealed class NewsFeed(HttpClient client, TimeProvider clock)
{
    public const string Source = "https://www.mohw.go.kr/rss/board.es?mid=a10503000000&bid=0027&info";
    private readonly SemaphoreSlim gate = new(1);
    private NewsResult? cached;
    private DateTimeOffset retryAt;

    public async Task<NewsResult?> Read(CancellationToken ct)
    {
        await gate.WaitAsync(ct);
        try {
            var now = clock.GetUtcNow();
            if (now < retryAt) return cached;
            try {
                // ResponseContentRead is bounded by MaxResponseContentBufferSize on this client.
                using var response = await client.GetAsync(Source, ct);
                response.EnsureSuccessStatusCode();
                var items = Parse(await response.Content.ReadAsStringAsync(ct));
                if (items.Count == 0) throw new XmlException("No usable news items.");
                cached = new(items, now, false);
                retryAt = now.AddMinutes(10);
            } catch (Exception ex) when (ex is HttpRequestException or XmlException or OperationCanceledException) {
                ct.ThrowIfCancellationRequested();
                retryAt = now.AddMinutes(1);
                if (cached is not null) cached = cached with { Stale = true };
            }
            return cached;
        } finally { gate.Release(); }
    }

    public static IReadOnlyList<NewsItem> Parse(string xml)
    {
        using var reader = XmlReader.Create(new StringReader(xml), new XmlReaderSettings {
            DtdProcessing = DtdProcessing.Prohibit, XmlResolver = null, MaxCharactersInDocument = 2_000_000
        });
        var document = XDocument.Load(reader);
        if (document.Root?.Name != "rss") throw new XmlException("Expected RSS.");
        return document.Root.Element("channel")?.Elements("item").Select(item => {
            var title = ((string?)item.Element("title"))?.Trim();
            var link = ((string?)item.Element("link"))?.Trim();
            if (string.IsNullOrEmpty(title) || title.Length > 500 ||
                !Uri.TryCreate(link, UriKind.Absolute, out var uri) || uri.Scheme != "https" ||
                uri.Host != "www.mohw.go.kr" || !uri.IsDefaultPort || uri.UserInfo.Length != 0) return null;
            DateTimeOffset? date = DateTimeOffset.TryParse((string?)item.Element("pubDate"), CultureInfo.InvariantCulture,
                DateTimeStyles.AllowWhiteSpaces, out var parsed) ? parsed : null;
            return new NewsItem(title, uri.AbsoluteUri, date);
        }).OfType<NewsItem>().DistinctBy(x => x.Link).Take(20).ToArray() ?? [];
    }
}
