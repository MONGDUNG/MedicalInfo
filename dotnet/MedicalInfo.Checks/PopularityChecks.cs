using System.Net;
using System.Net.Http.Json;
using System.Text.Json;

static class PopularityChecks
{
    public static async Task Run(Uri address, Action<bool,string> check, Func<Task> restart)
    {
        using var client = new HttpClient(new HttpClientHandler { CookieContainer = new() }) { BaseAddress = address };
        async Task Ready() {
            await restart();
            for (var i = 0; i < 40; i++) {
                try { using var r = await client.GetAsync("/health/live"); if (r.IsSuccessStatusCode) return; }
                catch (HttpRequestException) { }
                await Task.Delay(250);
            }
            throw new Exception("Popularity server not ready");
        }
        await Ready();
        using (var r = await client.PostAsync("/api/facilities/test-facility/view", null))
            check(r.StatusCode == HttpStatusCode.BadRequest, "anonymous view tracking requires CSRF token");
        var token = (await client.GetFromJsonAsync<JsonElement>("/api/account/csrf")).GetProperty("token").GetString();
        client.DefaultRequestHeaders.Add("X-CSRF-TOKEN", token);
        using (var r = await client.PostAsync("/api/facilities/missing/view", null))
            check(r.StatusCode == HttpStatusCode.NotFound, "unknown facility cannot acquire view statistics");
        var replies = await Task.WhenAll(Enumerable.Range(0, 5).Select(_ => client.PostAsync("/api/facilities/test-facility/view", null)));
        try { check(replies.All(r => r.StatusCode == HttpStatusCode.NoContent), "concurrent view tracking succeeds"); }
        finally { foreach (var reply in replies) reply.Dispose(); }
        var items = await client.GetFromJsonAsync<JsonElement>("/api/facilities/popular");
        check(items.GetArrayLength() == 1 && items[0].GetProperty("views").GetInt64() == 1,
            "same visitor concurrent views are counted only once");
        check(items[0].GetProperty("facility").GetProperty("ID").GetString() == "test-facility",
            "popularity returns current official facility details");
        await Ready();
        items = await client.GetFromJsonAsync<JsonElement>("/api/facilities/popular");
        check(items[0].GetProperty("views").GetInt64() == 1, "view totals persist across app restart");
    }
}
