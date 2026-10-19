using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using MedicalInfo;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using System.Threading.RateLimiting;
using Microsoft.AspNetCore.RateLimiting;

static class SocialProtocolChecks
{
    public static async Task Run(Action<bool,string> check)
    {
        foreach(var provider in new[]{"Kakao","Naver"})await RunProvider(provider,check);
    }
    private static async Task RunProvider(string provider,Action<bool,string> check)
    {
        var listener=new System.Net.Sockets.TcpListener(IPAddress.Loopback,0);listener.Start();var port=((IPEndPoint)listener.LocalEndpoint).Port;listener.Stop();
        var origin=$"http://127.0.0.1:{port}";
        var state=Path.Combine(Path.GetTempPath(),"medicalinfo-social-"+Guid.NewGuid());
        var builder=WebApplication.CreateBuilder(new WebApplicationOptions{EnvironmentName="Development"});
        builder.Logging.ClearProviders();builder.WebHost.UseUrls(origin);
        builder.Configuration.AddInMemoryCollection(new Dictionary<string,string?>{
            ["State:Path"]=state,["Social:PublicUrl"]=origin,[$"Social:{provider}:Enabled"]="true",
            [$"Social:{provider}:ClientId"]="test-client",[$"Social:{provider}:ClientSecret"]="test-secret"});
        builder.AddAccounts();builder.AddSocialAccounts();
        var backchannel=new ProviderStub(provider,origin);
        builder.Services.PostConfigure<SocialOptions>(provider,o=>o.Backchannel=new HttpClient(backchannel));
        builder.Services.AddRateLimiter(options=>{
            foreach(var name in new[]{"account","search"})options.AddPolicy(name,_=>RateLimitPartition.GetFixedWindowLimiter("fixture",_=>new FixedWindowRateLimiterOptions{PermitLimit=60,Window=TimeSpan.FromMinutes(1)}));
        });
        await using var app=builder.Build();await app.InitializeAccounts();
        app.UseRateLimiter();app.UseAuthentication();app.UseAuthorization();app.MapAccounts();app.MapSocialAccounts();
        await app.StartAsync();
        using var client=new HttpClient(new HttpClientHandler{AllowAutoRedirect=false,CookieContainer=new()}){BaseAddress=new Uri(origin)};
        var password="Social-"+Guid.NewGuid();
        async Task<HttpResponseMessage> Send(string path,HttpMethod method,object data){
            var token=await client.GetFromJsonAsync<JsonElement>("/api/account/csrf");using var req=new HttpRequestMessage(method,path){Content=JsonContent.Create(data)};
            req.Headers.Add("X-CSRF-TOKEN",token.GetProperty("token").GetString());return await client.SendAsync(req);
        }
        var checkedCorrelation=false;
        async Task Authenticate(string intent="login",string? suppliedPassword=null){
            using var start=await Send("/api/social/start",HttpMethod.Post,new{provider,intent,password=suppliedPassword});
            check(start.StatusCode==HttpStatusCode.Redirect,provider+" authorization challenge starts");
            var url=start.Headers.Location!;var query=QueryHelpers.ParseQuery(url.Query);
            check(query["redirect_uri"]==origin+"/signin-"+provider.ToLowerInvariant(),provider+" authorization uses fixed callback origin");
            backchannel.ExpectedState=query["state"].ToString();
            var callbackUrl="/signin-"+provider.ToLowerInvariant()+"?"+await new FormUrlEncodedContent(new Dictionary<string,string>{{"code","test-code"},{"state",backchannel.ExpectedState}}).ReadAsStringAsync();
            if(!checkedCorrelation){
                using var stranger=new HttpClient(new HttpClientHandler{AllowAutoRedirect=false,CookieContainer=new()}){BaseAddress=client.BaseAddress};
                using var denied=await stranger.GetAsync(callbackUrl);
                check(denied.StatusCode==HttpStatusCode.Redirect&&backchannel.TokenCalls==0,provider+" stolen state without initiating browser cookie is rejected");checkedCorrelation=true;
            }
            using var callback=await client.GetAsync(callbackUrl);
            check(callback.StatusCode==HttpStatusCode.Redirect&&callback.Headers.Location?.ToString()==origin+"/social.html",provider+" callback validates correlation and creates temporary external identity");
        }
        try {
            using(var r=await client.PostAsJsonAsync("/api/social/start",new{provider,intent="login"}))check(r.StatusCode==HttpStatusCode.BadRequest,provider+" social start rejects missing CSRF");
            using(var r=await client.GetAsync("/signin-"+provider.ToLowerInvariant()+"?code=stolen&state=invalid"))
                check(r.StatusCode==HttpStatusCode.Redirect&&backchannel.TokenCalls==0,provider+" invalid OAuth state never reaches token endpoint");
            await Authenticate();
            using(var r=await Send("/api/social/complete",HttpMethod.Post,new{}))check(r.StatusCode==HttpStatusCode.Conflict,provider+" unknown provider identity does not auto-create or merge account");
            using(var scope=app.Services.CreateScope()){
                var users=scope.ServiceProvider.GetRequiredService<Microsoft.AspNetCore.Identity.UserManager<Member>>();
                await users.CreateAsync(new Member{UserName="existing.member",Email="existing@example.invalid",DisplayName="기존 회원"},password);
            }
            using(var r=await Send("/api/social/register",HttpMethod.Post,new{username="attempted.merge",email="existing@example.invalid",displayName="차단 검증",password}))
                check(r.StatusCode==HttpStatusCode.Conflict,provider+" matching existing email cannot acquire another member account");
            using(var scope=app.Services.CreateScope()){
                var users=scope.ServiceProvider.GetRequiredService<Microsoft.AspNetCore.Identity.UserManager<Member>>();
                check(await users.FindByNameAsync("attempted.merge") is null,provider+" failed social signup rolls back member creation");
            }
            using(var r=await Send("/api/social/register",HttpMethod.Post,new{username="social.member",email="social@example.invalid",displayName="소셜 테스트",password}))
                check(r.IsSuccessStatusCode,provider+" explicit signup creates local member and provider link");
            var me=await client.GetFromJsonAsync<JsonElement>("/api/account/me");
            check(!me.GetProperty("emailConfirmed").GetBoolean(),provider+" provider identity does not bypass local email confirmation");
            using(var r=await client.GetAsync("/api/social/pending"))check(r.StatusCode==HttpStatusCode.Unauthorized,provider+" external identity cleared after signup");
            using(var r=await Send("/api/account/logout",HttpMethod.Post,new{}))r.EnsureSuccessStatusCode();
            using(var scope=app.Services.CreateScope()){
                var users=scope.ServiceProvider.GetRequiredService<Microsoft.AspNetCore.Identity.UserManager<Member>>();var user=(await users.FindByNameAsync("social.member"))!;
                user.SuspendedUntil=DateTime.UtcNow.AddDays(1);await users.UpdateAsync(user);
            }
            await Authenticate();
            using(var r=await Send("/api/social/complete",HttpMethod.Post,new{}))check(r.StatusCode==HttpStatusCode.Forbidden,provider+" social identity cannot bypass member suspension");
            using(var scope=app.Services.CreateScope()){
                var users=scope.ServiceProvider.GetRequiredService<Microsoft.AspNetCore.Identity.UserManager<Member>>();var user=(await users.FindByNameAsync("social.member"))!;
                user.SuspendedUntil=null;await users.UpdateAsync(user);
            }
            using(var r=await Send("/api/social/complete",HttpMethod.Post,new{}))check(r.IsSuccessStatusCode,provider+" linked provider logs into existing member");
            using(var r=await Send("/api/social/links/"+provider,HttpMethod.Delete,new{password="wrong"}))check(r.StatusCode==HttpStatusCode.Unauthorized,provider+" unlink requires current password");
            using(var r=await Send("/api/social/links/"+provider,HttpMethod.Delete,new{password}))check(r.StatusCode==HttpStatusCode.NoContent,provider+" owner can unlink provider with password");
            using(var r=await client.GetAsync("/api/account/me"))check(r.IsSuccessStatusCode,provider+" unlink refreshes owner's current session");
            using(var r=await Send("/api/account/login",HttpMethod.Post,new{username="social.member",password}))r.EnsureSuccessStatusCode();
            using(var r=await Send("/api/social/start",HttpMethod.Post,new{provider,intent="link",password="wrong"}))check(r.StatusCode==HttpStatusCode.Unauthorized,provider+" link requires recent password confirmation");
            await Authenticate("link",password);
            using(var r=await Send("/api/social/complete",HttpMethod.Post,new{}))check(r.IsSuccessStatusCode,provider+" explicit link validates initiating member and security stamp");
            check(backchannel.ValidRequests,provider+" code/token/profile exchange uses official endpoints and expected state");
        }finally{await app.StopAsync();}
    }
    private sealed class ProviderStub(string provider,string origin):HttpMessageHandler
    {
        public int TokenCalls;
        public string ExpectedState="";
        public bool ValidRequests=true;
        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request,CancellationToken ct)
        {
            string body;
            ValidRequests&=request.RequestUri!.Scheme=="https" && (provider=="Kakao"?request.RequestUri.Host is "kauth.kakao.com" or "kapi.kakao.com":request.RequestUri.Host is "nid.naver.com" or "openapi.naver.com");
            if(request.RequestUri!.AbsolutePath.EndsWith("token",StringComparison.Ordinal)) {
                TokenCalls++;var fields=QueryHelpers.ParseQuery(await request.Content!.ReadAsStringAsync(ct));
                ValidRequests&=fields["redirect_uri"]==origin+"/signin-"+provider.ToLowerInvariant()&&fields["client_secret"]=="test-secret"&&fields["code"]=="test-code";
                if(provider=="Naver")ValidRequests&=fields["state"]==ExpectedState;
                body="{\"access_token\":\"fixture-token\",\"token_type\":\"bearer\"}";
            }else{
                ValidRequests&=request.Headers.Authorization?.ToString()=="Bearer fixture-token";
                body=provider=="Kakao"?"{\"id\":1234}":"{\"resultcode\":\"00\",\"response\":{\"id\":\"naver-1234\"}}";
            }
            return new HttpResponseMessage(HttpStatusCode.OK){Content=new StringContent(body)};
        }
    }
}
