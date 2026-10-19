using System.Net.Http.Headers;
using System.Security.Claims;
using System.Text.Encodings.Web;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.OAuth;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace MedicalInfo;

public sealed class SocialOptions : OAuthOptions { public string PublicOrigin { get; set; } = ""; }
public sealed record SocialStart(string? Provider, string? Intent, string? Password);

// Keep callback URLs independent of Host/forwarded headers. The OAuth base handler validates
// protected state and correlation before exchanging a code; tokens are never saved to cookies/DB.
public sealed class SocialHandler(IOptionsMonitor<SocialOptions> options, ILoggerFactory logger, UrlEncoder encoder)
    : OAuthHandler<SocialOptions>(options, logger, encoder)
{
    protected override string BuildChallengeUrl(AuthenticationProperties properties, string redirectUri) =>
        base.BuildChallengeUrl(properties, Options.PublicOrigin + Options.CallbackPath);

    protected override async Task<OAuthTokenResponse> ExchangeCodeAsync(OAuthCodeExchangeContext context)
    {
        try {
            var fields = new Dictionary<string,string> { ["client_id"]=Options.ClientId,["client_secret"]=Options.ClientSecret,
                ["grant_type"]="authorization_code",["code"]=context.Code,["redirect_uri"]=Options.PublicOrigin+Options.CallbackPath };
            if (Scheme.Name == "Naver") fields["state"] = Request.Query["state"].ToString();
            using var response = await Backchannel.PostAsync(Options.TokenEndpoint,new FormUrlEncodedContent(fields),Context.RequestAborted);
            if (!response.IsSuccessStatusCode) return Failed();
            var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(Context.RequestAborted));
            if (!json.RootElement.TryGetProperty("access_token",out var token) || token.ValueKind != JsonValueKind.String || string.IsNullOrEmpty(token.GetString())) {
                json.Dispose(); return Failed();
            }
            return OAuthTokenResponse.Success(json);
        } catch (Exception ex) when (ex is HttpRequestException or JsonException or OperationCanceledException) {
            Context.RequestAborted.ThrowIfCancellationRequested();return Failed();
        }
    }
    private static OAuthTokenResponse Failed() => OAuthTokenResponse.Failed(new AuthenticationFailureException("Social provider unavailable."));
    protected override async Task<AuthenticationTicket> CreateTicketAsync(ClaimsIdentity identity, AuthenticationProperties properties, OAuthTokenResponse tokens)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, Options.UserInformationEndpoint);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer",tokens.AccessToken);
        using var response = await Backchannel.SendAsync(request,Context.RequestAborted);
        if (!response.IsSuccessStatusCode) throw new AuthenticationFailureException("Social provider unavailable.");
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync(Context.RequestAborted));
        var id = ProviderId(Scheme.Name,json.RootElement);
        identity.AddClaim(new Claim(ClaimTypes.NameIdentifier,id));
        identity.AddClaim(new Claim(ClaimTypes.Name,Scheme.DisplayName + " 사용자"));
        return new AuthenticationTicket(new ClaimsPrincipal(identity),properties,Scheme.Name);
    }
    public static string ProviderId(string provider,JsonElement json)
    {
        string? id = null;
        if (provider=="Kakao" && json.TryGetProperty("id",out var number) && number.ValueKind==JsonValueKind.Number && number.TryGetInt64(out var value) && value>0)
            id=value.ToString(System.Globalization.CultureInfo.InvariantCulture);
        if (provider=="Naver" && json.TryGetProperty("resultcode",out var code) && code.ValueKind==JsonValueKind.String && code.GetString()=="00" &&
            json.TryGetProperty("response",out var profile) && profile.TryGetProperty("id",out var key) && key.ValueKind==JsonValueKind.String)
            id=key.GetString();
        if (string.IsNullOrWhiteSpace(id) || id.Length>200 || id.Any(char.IsControl))
            throw new AuthenticationFailureException("Missing social provider identifier.");
        return id;
    }
}

public static class SocialAccounts
{
    private static readonly string[] Providers = ["Kakao","Naver"];
    public static string? PublicOrigin(IConfiguration config,IHostEnvironment environment)
    {
        var raw=config["Social:PublicUrl"]?.TrimEnd('/');
        if (!Uri.TryCreate(raw,UriKind.Absolute,out var uri) || uri.UserInfo.Length!=0 || uri.Query.Length!=0 || uri.Fragment.Length!=0 || uri.AbsolutePath!="/" ||
            uri.Scheme!="https" && !(environment.IsDevelopment()&&uri.Scheme=="http"&&uri.IsLoopback)) return null;
        return uri.GetLeftPart(UriPartial.Authority);
    }
    public static void AddSocialAccounts(this WebApplicationBuilder builder)
    {
        var authentication=builder.Services.AddAuthentication();
        builder.Services.ConfigureExternalCookie(options=> {
            options.ExpireTimeSpan=TimeSpan.FromMinutes(5);options.Cookie.Name="MedicalInfo.External";
            options.Cookie.HttpOnly=true;options.Cookie.SameSite=SameSiteMode.Lax;
            options.Cookie.SecurePolicy=CookieSecurePolicy.SameAsRequest;
        });
        foreach(var provider in Providers) {
            if (!builder.Configuration.GetValue<bool>($"Social:{provider}:Enabled")) continue;
            var origin=PublicOrigin(builder.Configuration,builder.Environment) ?? throw new InvalidOperationException("Social:PublicUrl must be a fixed HTTPS origin.");
            var clientId=builder.Configuration[$"Social:{provider}:ClientId"];
            var secret=builder.Configuration[$"Social:{provider}:ClientSecret"];
            if (string.IsNullOrWhiteSpace(clientId) || string.IsNullOrWhiteSpace(secret)) throw new InvalidOperationException("Enabled social providers require client ID and client secret.");
            authentication.AddOAuth<SocialOptions,SocialHandler>(provider,provider=="Kakao"?"카카오":"네이버",options=> {
                options.ClientId=clientId;options.ClientSecret=secret;options.PublicOrigin=origin;
                options.SignInScheme=IdentityConstants.ExternalScheme;options.CallbackPath="/signin-"+provider.ToLowerInvariant();
                options.AuthorizationEndpoint=provider=="Kakao"?"https://kauth.kakao.com/oauth/authorize":"https://nid.naver.com/oauth2.0/authorize";
                options.TokenEndpoint=provider=="Kakao"?"https://kauth.kakao.com/oauth/token":"https://nid.naver.com/oauth2.0/token";
                options.UserInformationEndpoint=provider=="Kakao"?"https://kapi.kakao.com/v2/user/me":"https://openapi.naver.com/v1/nid/me";
                options.SaveTokens=false;options.RemoteAuthenticationTimeout=TimeSpan.FromMinutes(5);
                options.Backchannel=new HttpClient(new HttpClientHandler{AllowAutoRedirect=false}) { Timeout=TimeSpan.FromSeconds(10),MaxResponseContentBufferSize=1_000_000 };
                options.CorrelationCookie.SameSite=SameSiteMode.Lax;
                options.CorrelationCookie.SecurePolicy=origin.StartsWith("https://",StringComparison.Ordinal)?CookieSecurePolicy.Always:CookieSecurePolicy.SameAsRequest;
                options.Events.OnRemoteFailure=context=> {
                    // Provider error messages can contain tokens or other personal information.
                    context.Response.Redirect("/account.html?social=failed");context.HandleResponse();return Task.CompletedTask;
                };
            });
        }
    }
    private static string? Property(AuthenticationProperties properties,string name) => properties.Items.TryGetValue(name,out var value)?value:null;
    private static bool Fresh(ExternalLoginInfo? info) => info?.AuthenticationProperties?.IssuedUtc is { } issued && issued>DateTimeOffset.UtcNow.AddMinutes(-5);
    private static async Task<IResult> FinishLogin(Member user,HttpContext context,UserManager<Member> users,SignInManager<Member> signIn)
    {
        if(user.SuspendedUntil>DateTime.UtcNow || !await signIn.CanSignInAsync(user) || await users.IsLockedOutAsync(user))
            return Results.Problem(statusCode:403,title:"이용 제한·이메일 인증·로그인 잠금 상태를 확인해주세요.");
        user.LastLoginAt=DateTime.UtcNow;
        if (!(await users.UpdateAsync(user)).Succeeded)return Results.Conflict();
        await signIn.SignInAsync(user,false);await context.SignOutAsync(IdentityConstants.ExternalScheme);
        return Results.Ok(new{redirect="/account.html"});
    }
    public static void MapSocialAccounts(this WebApplication app)
    {
        var group=Inquiries.Protect(app.MapGroup("/api/social")).RequireRateLimiting("account");
        group.MapGet("/providers",async(IAuthenticationSchemeProvider schemes)=> Results.Ok(
            (await schemes.GetAllSchemesAsync()).Where(s=>Providers.Contains(s.Name)).Select(s=>new{provider=s.Name,name=s.DisplayName})));
        group.MapPost("/start",async(HttpContext context,SignInManager<Member> signIn,UserManager<Member> users,IAuthenticationSchemeProvider schemes,IConfiguration config,IHostEnvironment env)=> {
            SocialStart? input;
            if(context.Request.HasFormContentType) {
                var form=await context.Request.ReadFormAsync();input=new(form["provider"],form["intent"],form["password"]);
            } else input=await context.Request.ReadFromJsonAsync<SocialStart>();
            if(input is null)return Results.BadRequest();
            if(input.Provider is null || !Providers.Contains(input.Provider) || await schemes.GetSchemeAsync(input.Provider) is null)return Results.NotFound();
            if(input.Intent is not ("login" or "link"))return Results.BadRequest();
            var user=await users.GetUserAsync(context.User);
            if(input.Intent=="login" && user is not null)return Results.Conflict();
            var origin=PublicOrigin(config,env)!;
            var properties=signIn.ConfigureExternalAuthenticationProperties(input.Provider,origin+"/social.html",input.Intent=="link"?user?.Id:null);
            properties.Items["intent"]=input.Intent;
            if(input.Intent=="link") {
                if(user is null)return Results.Unauthorized();
                if(input.Password?.Length is not (>=1 and <=128) || !(await signIn.CheckPasswordSignInAsync(user,input.Password,true)).Succeeded)return Results.Unauthorized();
                properties.Items["stamp"]=await users.GetSecurityStampAsync(user);
            }
            await context.SignOutAsync(IdentityConstants.ExternalScheme);
            return Results.Challenge(properties,[input.Provider]);
        });
        group.MapGet("/pending",async(SignInManager<Member> signIn)=> {
            var info=await signIn.GetExternalLoginInfoAsync();
            return !Fresh(info)?Results.Unauthorized():Results.Ok(new{provider=info!.LoginProvider,intent=Property(info.AuthenticationProperties!,"intent")});
        });
        group.MapGet("/links",async(HttpContext context,UserManager<Member> users)=> {
            var user=await users.GetUserAsync(context.User);if(user is null)return Results.Unauthorized();
            return Results.Ok((await users.GetLoginsAsync(user)).Select(x=>new{provider=x.LoginProvider}));
        }).RequireAuthorization();
        group.MapPost("/complete",async(HttpContext context,UserManager<Member> users,SignInManager<Member> signIn)=> {
            var info=await signIn.GetExternalLoginInfoAsync();
            if(!Fresh(info))return Results.Unauthorized();
            var intent=Property(info!.AuthenticationProperties!,"intent");
            var current=await users.GetUserAsync(context.User);
            if(intent=="link") {
                if(current is null)return Results.Unauthorized();
                var expected=await signIn.GetExternalLoginInfoAsync(current.Id);
                if(!Fresh(expected) || Property(info.AuthenticationProperties!,"stamp")!=await users.GetSecurityStampAsync(current))return Results.Unauthorized();
                if(!(await users.AddLoginAsync(current,info)).Succeeded)return Results.Problem(statusCode:409,title:"이 소셜 계정은 이미 연결되어 있습니다.");
                await context.SignOutAsync(IdentityConstants.ExternalScheme);return Results.Ok(new{redirect="/account.html"});
            }
            if(intent!="login" || current is not null)return Results.Conflict();
            var user=await users.FindByLoginAsync(info.LoginProvider,info.ProviderKey);
            if(user is null)return Results.Problem(statusCode:409,title:"연결된 회원이 없습니다. 새로 가입하거나 기존 회원으로 로그인한 뒤 연결해주세요.");
            return await FinishLogin(user,context,users,signIn);
        });
        group.MapPost("/register",async(RegisterMember input,HttpContext context,UserManager<Member> users,SignInManager<Member> signIn,MemberDatabase db)=> {
            var info=await signIn.GetExternalLoginInfoAsync();
            if(!Fresh(info) || Property(info!.AuthenticationProperties!,"intent")!="login" || context.User.Identity?.IsAuthenticated==true)return Results.Unauthorized();
            var validation=Accounts.RegistrationError(input);if(validation is not null)return Results.Problem(statusCode:400,title:validation);
            await using var transaction=await db.Database.BeginTransactionAsync();
            var user=new Member{UserName=input.Username,Email=input.Email,DisplayName=input.DisplayName!.Trim()};
            try {
                if(!(await users.CreateAsync(user,input.Password!)).Succeeded || !(await users.AddLoginAsync(user,info!)).Succeeded)return Results.Problem(statusCode:409,title:"가입 정보 또는 소셜 계정이 이미 사용 중입니다.");
                await transaction.CommitAsync();
            }catch(DbUpdateException){return Results.Conflict();}
            // Email ownership is confirmed through our own verification flow, never by matching provider email.
            await context.SignOutAsync(IdentityConstants.ExternalScheme);
            return await signIn.CanSignInAsync(user)?await FinishLogin(user,context,users,signIn):Results.Ok(new{redirect="/account.html?social=verify-email"});
        });
        group.MapDelete("/links/{provider}",async(string provider,[Microsoft.AspNetCore.Mvc.FromBody] DeleteMember input,HttpContext context,UserManager<Member> users,SignInManager<Member> signIn)=> {
            var user=await users.GetUserAsync(context.User);if(user is null)return Results.Unauthorized();
            if(input.Password?.Length is not (>=1 and <=128) || !await users.CheckPasswordAsync(user,input.Password))return Results.Unauthorized();
            var link=(await users.GetLoginsAsync(user)).SingleOrDefault(x=>x.LoginProvider==provider);
            if(link is null)return Results.NotFound();
            if(!(await users.RemoveLoginAsync(user,provider,link.ProviderKey)).Succeeded)return Results.Conflict();
            await signIn.RefreshSignInAsync(user);
            return Results.NoContent();
        }).RequireAuthorization();
    }
}
