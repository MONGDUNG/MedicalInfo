using System.Net;
using System.Net.Http.Json;
using System.Text.Json;

static class PersistenceChecks
{
    public static async Task Run(Uri address, Action<bool, string> check, Func<Task> restart)
    {
        using var client = new HttpClient(new HttpClientHandler { CookieContainer = new CookieContainer() }) { BaseAddress = address, Timeout = TimeSpan.FromSeconds(3) };
        // Run after restarting once so unrelated rate-limit checks do not affect this scenario.
        await restart(); await Ready();
        async Task Ready() {
            for (var attempt = 0; attempt < 40; attempt++) {
                try { using var health = await client.GetAsync("/health/live"); if (health.IsSuccessStatusCode) return; }
                catch (HttpRequestException) { } catch (TaskCanceledException) { }
                await Task.Delay(250);
            }
            throw new Exception("Restarted web process did not become ready");
        }
        async Task<string> Token() {
            using var response = await client.GetAsync("/api/account/csrf"); response.EnsureSuccessStatusCode();
            return (await response.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("token").GetString()!;
        }
        async Task<HttpResponseMessage> Post(string url, object body, string token) {
            using var request = new HttpRequestMessage(HttpMethod.Post, url) { Content = JsonContent.Create(body) };
            request.Headers.Add("X-CSRF-TOKEN", token); return await client.SendAsync(request);
        }
        var password = "Persistent-"+Guid.NewGuid();
        var token = await Token();
        using (var registration = await Post("/api/account/register", new { username = "persistent.member", email = "persistent@example.invalid", password, displayName = "유지 검증" }, token)) registration.EnsureSuccessStatusCode();
        using (var login = await Post("/api/account/login", new { username = "persistent.member", password }, token)) login.EnsureSuccessStatusCode();
        token = await Token(); long id;
        using (var post = await Post("/api/community/posts", new { title = "재시작 검증", content = "재시작 후 보존", category = "자유" }, token)) {
            post.EnsureSuccessStatusCode(); id = (await post.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();
        }
        await restart(); await Ready();
        using (var member = await client.GetAsync("/api/account/me"))
            check(member.IsSuccessStatusCode, "member database and session keys survive process restart");
        using (var post = await client.GetAsync("/api/community/posts/"+id)) {
            var item = await post.Content.ReadFromJsonAsync<JsonElement>();
            check(item.GetProperty("canEdit").GetBoolean() && item.GetProperty("content").GetString() == "재시작 후 보존", "post contents and ownership survive restart and migrations rerun safely");
        }
        using var logout = await Post("/api/account/logout", new { }, token);
        check(logout.StatusCode == HttpStatusCode.NoContent, "existing CSRF token remains valid after restart");
    }
}
