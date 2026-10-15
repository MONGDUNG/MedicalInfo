using System.Net;
using System.Net.Http.Json;
using System.Text.Json;

static class CommunityChecks
{
    public static async Task Run(Uri address, Action<bool, string> check)
    {
        using var owner = new HttpClient(new HttpClientHandler { CookieContainer = new CookieContainer() }) { BaseAddress = address };
        using var other = new HttpClient(new HttpClientHandler { CookieContainer = new CookieContainer() }) { BaseAddress = address };
        using var guest = new HttpClient { BaseAddress = address };
        var tokens = new Dictionary<HttpClient, string>();
        var password = "Community-" + Guid.NewGuid();
        async Task Token(HttpClient client) {
            using var result = await client.GetAsync("/api/account/csrf"); result.EnsureSuccessStatusCode();
            tokens[client] = (await result.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("token").GetString()!;
        }
        async Task<HttpResponseMessage> Send(HttpClient client, string url, HttpMethod method, object? body = null) {
            using var request = new HttpRequestMessage(method, url);
            if (body is not null) request.Content = JsonContent.Create(body);
            if (tokens.TryGetValue(client, out var token)) request.Headers.Add("X-CSRF-TOKEN", token);
            return await client.SendAsync(request);
        }
        async Task Status(HttpClient client, string url, HttpMethod method, object? body, HttpStatusCode expected, string label) {
            using var result = await Send(client, url, method, body);
            check(result.StatusCode == expected, label + $" ({(int)result.StatusCode})");
        }
        foreach (var (client, name) in new[] { (owner, "post.owner"), (other, "post.other") }) {
            await Token(client);
            using var registration = await Send(client, "/api/account/register", HttpMethod.Post, new { username = name, email = name+"@example.invalid", password, displayName = name, role = "Admin" });
            registration.EnsureSuccessStatusCode();
            using var login = await Send(client, "/api/account/login", HttpMethod.Post, new { username = name, password });
            login.EnsureSuccessStatusCode(); await Token(client);
        }
        var input = new { title = "기능 이전 검증", content = "<script>alert('test')</script>", category = "자유", authorId = "fake-author" };
        const string posts = "/api/community/posts";
        const string reviews = "/api/reviews";
        var reviewBody = new { facilityId = "test-facility", rating = 4, content = "테스트 리뷰" };
        await Status(guest, reviews, HttpMethod.Post, reviewBody, HttpStatusCode.Unauthorized, "anonymous review creation denied");
        await Status(owner, reviews, HttpMethod.Post, new { facilityId = "missing", rating = 4, content = "테스트" }, HttpStatusCode.NotFound, "review requires existing facility");
        await Status(owner, reviews, HttpMethod.Post, new { facilityId = "test-facility", rating = 6, content = "테스트" }, HttpStatusCode.BadRequest, "out of range rating rejected");
        long reviewId;
        using (var created = await Send(owner, reviews, HttpMethod.Post, reviewBody)) {
            check(created.StatusCode == HttpStatusCode.Created, "review created for verified facility");
            reviewId = (await created.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();
        }
        await Status(owner, reviews, HttpMethod.Post, reviewBody, HttpStatusCode.Conflict, "one review per member and facility enforced by database");
        await Status(other, reviews+"/"+reviewId, HttpMethod.Put, reviewBody, HttpStatusCode.Forbidden, "another member cannot edit review");
        await Status(other, reviews+"/"+reviewId, HttpMethod.Delete, null, HttpStatusCode.Forbidden, "another member cannot delete review");
        await Status(owner, reviews+"/"+reviewId, HttpMethod.Put, new { facilityId = "another", rating = 5, content = "변조" }, HttpStatusCode.BadRequest, "review cannot be reassigned to a different facility");
        await Status(owner, reviews+"/"+reviewId, HttpMethod.Put, new { facilityId = "test-facility", rating = 5, content = "수정" }, HttpStatusCode.NoContent, "review owner can edit rating");
        using (var listing = await guest.GetAsync(reviews+"?facilityId=test-facility")) {
            var data = await listing.Content.ReadFromJsonAsync<JsonElement>();
            check(data.GetProperty("total").GetInt32() == 1 && data.GetProperty("average").GetDouble() == 5, "review total and average reflect edits");
        }
        await Status(owner, reviews+"/"+reviewId, HttpMethod.Delete, null, HttpStatusCode.NoContent, "review owner can delete review");
        using (var listing = await guest.GetAsync(reviews+"?facilityId=test-facility")) {
            var data = await listing.Content.ReadFromJsonAsync<JsonElement>();
            check(data.GetProperty("total").GetInt32() == 0 && data.GetProperty("average").ValueKind == JsonValueKind.Null, "last review deletion yields no rating instead of division by zero");
        }
        await Status(guest, posts, HttpMethod.Post, input, HttpStatusCode.Unauthorized, "anonymous post creation denied");
        using (var csrf = await owner.PostAsJsonAsync(posts, input)) check(csrf.StatusCode == HttpStatusCode.BadRequest, "authenticated post mutation without CSRF denied");
        await Status(owner, posts, HttpMethod.Post, new { title = "공지", content = "내용", category = "공지사항" }, HttpStatusCode.Forbidden, "signup role injection cannot grant notice privileges");
        long id;
        using (var created = await Send(owner, posts, HttpMethod.Post, input)) {
            check(created.StatusCode == HttpStatusCode.Created, "post creation succeeds");
            id = (await created.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();
        }
        using (var read = await guest.GetAsync(posts+"/"+id)) {
            var result = await read.Content.ReadFromJsonAsync<JsonElement>();
            check(result.GetProperty("author").GetString() == "post.owner" && !result.GetProperty("canEdit").GetBoolean(), "post author is derived from session and guest cannot edit");
            check(result.GetProperty("content").GetString() == input.content, "post body remains plain text for safe rendering");
        }
        await Status(other, posts+"/"+id, HttpMethod.Put, input, HttpStatusCode.Forbidden, "another member cannot edit post");
        await Status(other, posts+"/"+id, HttpMethod.Delete, null, HttpStatusCode.Forbidden, "another member cannot delete post");
        await Status(owner, posts+"/"+id, HttpMethod.Put, new { title = "수정 검증", content = "수정한 내용", category = "질문" }, HttpStatusCode.NoContent, "owner can edit post");
        using (var filtered = await guest.GetAsync(posts+"?category="+Uri.EscapeDataString("질문")+"&keyword="+Uri.EscapeDataString("수정"))) {
            var data = await filtered.Content.ReadFromJsonAsync<JsonElement>();
            check(data.GetProperty("total").GetInt32() == 1 && data.GetProperty("items").GetArrayLength() == 1, "filtered total and page use same query");
        }
        await Status(guest, posts+"?page=0", HttpMethod.Get, null, HttpStatusCode.BadRequest, "invalid community page rejected");
        long commentId;
        using (var comment = await Send(other, posts+"/"+id+"/comments", HttpMethod.Post, new { content = "다른 회원의 댓글" })) {
            check(comment.StatusCode == HttpStatusCode.Created, "authenticated comment creation succeeds");
            commentId = (await comment.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();
        }
        var commentUrl = "/api/community/comments/"+commentId;
        await Status(owner, commentUrl, HttpMethod.Put, new { content = "변조" }, HttpStatusCode.Forbidden, "post owner cannot edit another member comment");
        await Status(owner, commentUrl, HttpMethod.Delete, null, HttpStatusCode.Forbidden, "post owner cannot delete another member comment directly");
        await Status(other, commentUrl, HttpMethod.Put, new { content = "수정한 댓글" }, HttpStatusCode.NoContent, "comment author can edit comment");
        await Status(owner, posts+"/"+id, HttpMethod.Delete, null, HttpStatusCode.NoContent, "post owner can delete post");
        await Status(other, commentUrl, HttpMethod.Put, new { content = "없음" }, HttpStatusCode.NotFound, "post deletion cascades to comments");
        await Status(owner, posts+"/999999/comments", HttpMethod.Post, new { content = "없음" }, HttpStatusCode.NotFound, "comment on missing post rejected");
        using (var created = await Send(owner, posts, HttpMethod.Post, input)) {
            created.EnsureSuccessStatusCode(); id = (await created.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();
        }
        await Status(owner, "/api/account/me", HttpMethod.Delete, new { password }, HttpStatusCode.NoContent, "member with posts can withdraw");
        using (var read = await guest.GetAsync(posts+"/"+id)) {
            var result = await read.Content.ReadFromJsonAsync<JsonElement>();
            check(result.GetProperty("author").GetString() == "탈퇴 회원", "withdrawal removes account linkage while retaining public post");
        }
    }
}
