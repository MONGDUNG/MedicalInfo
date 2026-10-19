using System.Net;
using System.Net.Http.Json;
using System.Text.Json;

static class AccountChecks
{
    public static async Task Run(Uri address, Action<bool, string> check)
    {
        using var first = new HttpClient(new HttpClientHandler { CookieContainer = new CookieContainer() }) { BaseAddress = address };
        using var second = new HttpClient(new HttpClientHandler { CookieContainer = new CookieContainer() }) { BaseAddress = address };
        async Task<HttpResponseMessage> Send(HttpClient client, string path, HttpMethod method, object body)
        {
            using var tokenResponse = await client.GetAsync("/api/account/csrf");
            tokenResponse.EnsureSuccessStatusCode();
            var csrf = await tokenResponse.Content.ReadFromJsonAsync<JsonElement>();
            using var request = new HttpRequestMessage(method, "/api/account/" + path) { Content = JsonContent.Create(body) };
            request.Headers.Add("X-CSRF-TOKEN", csrf.GetProperty("token").GetString());
            return await client.SendAsync(request);
        }
        async Task Status(HttpClient client, string path, HttpMethod method, object body, HttpStatusCode expected, string label)
        {
            using var response = await Send(client, path, method, body);
            check(response.StatusCode == expected, label + $" ({(int)response.StatusCode})");
        }
        using var anonymous = await first.GetAsync("/api/account/me");
        check(anonymous.StatusCode == HttpStatusCode.Unauthorized, "profile requires authentication without redirect");
        var password = "Initial-" + Guid.NewGuid();
        var replacement = "Changed-" + Guid.NewGuid();
        var registration = new { username = "test.member", email = "test@example.invalid", password, displayName = "테스트", role = "Admin" };
        using var noCsrf = await first.PostAsJsonAsync("/api/account/register", registration);
        check(noCsrf.StatusCode == HttpStatusCode.BadRequest, "registration without CSRF token rejected");
        await Status(first, "register", HttpMethod.Post, registration, HttpStatusCode.Created, "member registered");
        await Status(first, "register", HttpMethod.Post, registration, HttpStatusCode.BadRequest, "duplicate username/email rejected");
        await Status(first, "login", HttpMethod.Post, new { registration.username, password }, HttpStatusCode.OK, "password login succeeds");
        using (var profile = await first.GetAsync("/api/account/me")) {
            var text = await profile.Content.ReadAsStringAsync();
            check(profile.IsSuccessStatusCode && text.Contains("test.member") && !text.Contains("password", StringComparison.OrdinalIgnoreCase), "profile excludes password and hash");
        }
        await Status(first, "profile", HttpMethod.Put, new { displayName = "수정한 이름", id = "someone-else", role = "Admin" }, HttpStatusCode.OK, "profile updates only authenticated member");
        await Status(second, "login", HttpMethod.Post, new { registration.username, password }, HttpStatusCode.OK, "second browser session created");
        await Status(first, "password", HttpMethod.Post, new { currentPassword = "incorrect", newPassword = replacement }, HttpStatusCode.BadRequest, "password change requires current password");
        await Status(first, "password", HttpMethod.Post, new { currentPassword = password, newPassword = replacement }, HttpStatusCode.NoContent, "password change succeeds");
        using (var stale = await second.GetAsync("/api/account/me"))
            check(stale.StatusCode == HttpStatusCode.Unauthorized, "password change revokes other browser sessions immediately");
        await Status(first, "login", HttpMethod.Post, new { registration.username, password }, HttpStatusCode.Unauthorized, "old password no longer works");
        await Status(first, "login", HttpMethod.Post, new { registration.username, password = replacement }, HttpStatusCode.OK, "new password works");
        await Status(first, "me", HttpMethod.Delete, new { password = "incorrect" }, HttpStatusCode.BadRequest, "account deletion requires password confirmation");
        using (var unsafeGet = await first.GetAsync("/member/delete"))
            check(unsafeGet.StatusCode == HttpStatusCode.NotFound, "legacy GET account deletion is not exposed");
        await Status(first, "logout", HttpMethod.Post, new { }, HttpStatusCode.NoContent, "logout succeeds");
        using (var loggedOut = await first.GetAsync("/api/account/me"))
            check(loggedOut.StatusCode == HttpStatusCode.Unauthorized, "logout clears authentication");
        await Status(first, "login", HttpMethod.Post, new { registration.username, password = replacement }, HttpStatusCode.OK, "member can sign in again");
        await Status(first, "me", HttpMethod.Delete, new { password = replacement }, HttpStatusCode.NoContent, "authenticated account deletion succeeds");
        await Status(first, "login", HttpMethod.Post, new { registration.username, password = replacement }, HttpStatusCode.Unauthorized, "deleted member cannot sign in");
        await Status(first, "register", HttpMethod.Post, new { username = "locked.member", email = "lock@example.invalid", password, displayName = "잠금검증" }, HttpStatusCode.Created, "lockout test member created");
        for (var attempt = 0; attempt < 5; attempt++)
            await Status(first, "login", HttpMethod.Post, new { username = "locked.member", password = "incorrect" }, HttpStatusCode.Unauthorized, "incorrect login rejected");
        await Status(first, "login", HttpMethod.Post, new { username = "locked.member", password }, HttpStatusCode.Unauthorized, "lockout rejects correct password after repeated failures");
    }
}
