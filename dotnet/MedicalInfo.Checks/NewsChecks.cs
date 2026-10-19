using MedicalInfo;
using System.Net;
using System.Xml;

static class NewsChecks
{
    public static async Task Run(Action<bool,string> check)
    {
        const string xml = """
<rss><channel>
<item><title><![CDATA[<script>plain text</script>]]></title><link>https://www.mohw.go.kr/board.es?id=1</link><pubDate>Sun, 18 Oct 2026 03:05:00 GMT</pubDate></item>
<item><title>duplicate</title><link>https://www.mohw.go.kr/board.es?id=1</link></item>
<item><title>bad</title><link>javascript:alert(1)</link></item>
<item><title>bad</title><link>https://www.mohw.go.kr.evil.test/a</link></item>
<item><title>bad</title><link>https://user@www.mohw.go.kr/a</link></item>
<item><title>undated</title><link>https://www.mohw.go.kr/board.es?id=2</link><pubDate>invalid</pubDate></item>
</channel></rss>
""";
        var items = NewsFeed.Parse(xml);
        check(items.Count == 2 && items[0].Title.StartsWith("<script>") && items[1].PublishedAt is null,
            "news allows official HTTPS links only, deduplicates and preserves plain text");
        check(items[0].PublishedAt?.Year == 2026, "official RSS date parsed");
        try { NewsFeed.Parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><rss>&x;</rss>"); throw new Exception("DTD accepted"); }
        catch (XmlException) { check(true, "news DTD and external entities rejected"); }
        var clock = new Clock();
        var handler = new Handler { Body = xml };
        using var http = new HttpClient(handler);
        var feed = new NewsFeed(http, clock);
        var first = await feed.Read(default);
        await Task.WhenAll(Enumerable.Range(0, 8).Select(_ => feed.Read(default)));
        check(handler.Calls == 1 && first is { Stale: false }, "news concurrent requests share cached response");
        clock.Now = clock.Now.AddMinutes(11);
        handler.Body = "invalid XML";
        var stale = await feed.Read(default);
        await feed.Read(default);
        check(stale is { Stale: true } && stale.FetchedAt == first!.FetchedAt && handler.Calls == 2,
            "news failure preserves last successful data and backs off retries");
        clock.Now = clock.Now.AddMinutes(2);
        handler.Body = xml;
        check(await feed.Read(default) is { Stale: false } && handler.Calls == 3, "news recovers after upstream failure");
        handler.Body = "<rss><channel/></rss>";
        var empty = new NewsFeed(http, clock);
        check(await empty.Read(default) is null, "empty upstream news is unavailable rather than successful empty list");
        using var cts = new CancellationTokenSource(); cts.Cancel();
        try { await feed.Read(cts.Token); throw new Exception("Cancellation swallowed"); }
        catch (OperationCanceledException) { check(true, "news caller cancellation preserved"); }
    }
    sealed class Clock : TimeProvider {
        public DateTimeOffset Now = new(2026, 10, 19, 0, 0, 0, TimeSpan.Zero);
        public override DateTimeOffset GetUtcNow() => Now;
    }
    sealed class Handler : HttpMessageHandler {
        public string Body = "";
        public int Calls;
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct) {
            Calls++;
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent(Body) });
        }
    }
}
