using System.Net;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using Microsoft.Data.Sqlite;

static class RecoveryChecks
{
    public static async Task Run(Uri address,string state,Action<bool,string> check,Func<bool,Task> restart) {
        using var owner=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var second=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var guest=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        var tokens=new Dictionary<HttpClient,string>();var password="Recovery-"+Guid.NewGuid();const string email="recovery@example.invalid";
        async Task Token(HttpClient client){using var r=await client.GetAsync("/api/account/csrf");r.EnsureSuccessStatusCode();tokens[client]=(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("token").GetString()!;}
        async Task Reset(bool enabled=true){await restart(enabled);for(var i=0;i<40;i++){try{using var r=await guest.GetAsync("/health/live");if(r.IsSuccessStatusCode){await Token(guest);return;}}catch(HttpRequestException){}await Task.Delay(250);}throw new Exception("Recovery test server not ready");}
        async Task<HttpResponseMessage> Send(HttpClient client,string path,object body){using var request=new HttpRequestMessage(HttpMethod.Post,path){Content=JsonContent.Create(body)};request.Headers.Add("X-CSRF-TOKEN",tokens[client]);return await client.SendAsync(request);}
        async Task Status(HttpClient client,string path,object body,HttpStatusCode expected,string label){using var r=await Send(client,path,body);check(r.StatusCode==expected,label+$" ({(int)r.StatusCode})");}
        async Task Login(HttpClient client){await Token(client);using var r=await Send(client,"/api/account/login",new{username="recovery.member",password});r.EnsureSuccessStatusCode();await Token(client);}
        async Task Sql(string sql){await using var db=new SqliteConnection(new SqliteConnectionStringBuilder{DataSource=Path.Combine(state,"members.db")}.ToString());await db.OpenAsync();using var cmd=db.CreateCommand();cmd.CommandText=sql;await cmd.ExecuteNonQueryAsync();}
        var seen=new HashSet<string>();
        async Task<string> Mail(string purpose){
            for(var attempt=0;attempt<80;attempt++){
                foreach(var path in Directory.GetFiles(Path.Combine(state,"mail-pickup"),"*.eml")){
                    if(seen.Contains(path))continue;string raw;try{raw=await File.ReadAllTextAsync(path);}catch(IOException){continue;}
                    var parts=Regex.Split(raw,"\\r?\\n\\r?\\n",RegexOptions.None,TimeSpan.FromSeconds(1));if(parts.Length<2)continue;
                    var body=parts[0].Contains("base64",StringComparison.OrdinalIgnoreCase)?Encoding.UTF8.GetString(Convert.FromBase64String(parts[1])):parts[1];
                    var match=Regex.Match(body,@"http://localhost:\d+/recover.html#purpose="+purpose+@"&token=([A-F0-9]{64})");
                    if(match.Success){seen.Add(path);return match.Groups[1].Value;}
                }
                await Task.Delay(100);
            }
            throw new Exception("Expected local pickup email for "+purpose);
        }
        const string root="/api/recovery/";
        await Reset(false);
        await Status(guest,root+"request/reset",new{email},HttpStatusCode.ServiceUnavailable,"disabled mail does not pretend recovery email was sent");
        await Token(owner);using(var r=await Send(owner,"/api/account/register",new{username="recovery.member",email,password,displayName="Recovery member"}))r.EnsureSuccessStatusCode();
        await Login(owner);await Login(second);
        await Reset();
        using(var r=await owner.GetAsync("/api/account/me"))check(r.StatusCode==HttpStatusCode.Unauthorized,"enabling required email verification rejects existing unverified sessions");
        await Token(owner);await Status(owner,"/api/account/login",new{username="recovery.member",password},HttpStatusCode.Unauthorized,"required email verification blocks unverified login");
        string known,unknown;
        using(var r=await Send(guest,root+"request/reset",new{email})){r.EnsureSuccessStatusCode();known=await r.Content.ReadAsStringAsync();}
        using(var r=await Send(guest,root+"request/reset",new{email="unknown@example.invalid"})){r.EnsureSuccessStatusCode();unknown=await r.Content.ReadAsStringAsync();}
        check(known==unknown,"unknown and unconfirmed email recovery responses are identical");
        await Status(guest,root+"request/confirm",new{email},HttpStatusCode.OK,"email confirmation can be requested before login");var confirmation=await Mail("confirm");
        await using(var db=new SqliteConnection(new SqliteConnectionStringBuilder{DataSource=Path.Combine(state,"members.db")}.ToString())){
            await db.OpenAsync();using var cmd=db.CreateCommand();cmd.CommandText="SELECT Hash FROM AccountLinks WHERE Email='recovery@example.invalid' AND Purpose='confirm';";
            var stored=Convert.ToString(await cmd.ExecuteScalarAsync());check(stored==Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(Encoding.UTF8.GetBytes(confirmation))) && stored!=confirmation,"database stores only a hash of the bearer token");
        }
        await Status(guest,root+"request/confirm",new{email},HttpStatusCode.OK,"confirmation resend returns neutral response during cooldown");
        check(Directory.GetFiles(Path.Combine(state,"mail-pickup"),"*.eml").Length==1,"unconfirmed reset and repeated request do not produce extra email");
        using(var r=await guest.PostAsJsonAsync(root+"redeem",new{token=confirmation,purpose="confirm"}))check(r.StatusCode==HttpStatusCode.BadRequest,"email token redemption requires CSRF");
        await Status(guest,root+"redeem",new{token=new string('A',64),purpose="confirm"},HttpStatusCode.BadRequest,"forged token rejected");
        await Status(guest,root+"redeem",new{token=confirmation,purpose="reset",newPassword=password},HttpStatusCode.BadRequest,"confirmation token cannot reset a password");
        await Status(guest,root+"redeem",new{token=confirmation,purpose="confirm"},HttpStatusCode.OK,"mailbox proof confirms email");
        await Status(guest,root+"redeem",new{token=confirmation,purpose="confirm"},HttpStatusCode.BadRequest,"confirmation link is single use");
        using(var r=await second.GetAsync("/api/account/me"))check(r.StatusCode==HttpStatusCode.Unauthorized,"confirmation invalidates pre-verification sessions");
        await Login(owner);await Login(second);
        using(var r=await owner.GetAsync("/api/account/me"))check((await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("emailConfirmed").GetBoolean(),"confirmed status persists in account profile");
        await Reset();
        await Status(guest,root+"request/username",new{email},HttpStatusCode.OK,"username recovery request accepted");var usernameToken=await Mail("username");
        using(var r=await Send(guest,root+"redeem",new{token=usernameToken,purpose="username"})){r.EnsureSuccessStatusCode();check((await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("username").GetString()=="recovery.member","username disclosed only after mailbox proof");check(r.Headers.CacheControl?.NoStore==true,"recovered username response cannot be cached");}
        await Status(guest,root+"redeem",new{token=usernameToken,purpose="username"},HttpStatusCode.BadRequest,"username link cannot be replayed");
        await Status(guest,root+"request/reset",new{email},HttpStatusCode.OK,"verified member can request password reset");var reset=await Mail("reset");
        await Status(guest,root+"redeem",new{token=reset,purpose="reset",newPassword="short"},HttpStatusCode.BadRequest,"reset applies password length policy without consuming link");
        await Status(guest,root+"redeem",new{token=reset,purpose="username"},HttpStatusCode.BadRequest,"password token cannot disclose username");
        var previous=password;password="Recovered-"+Guid.NewGuid();
        await Status(guest,root+"redeem",new{token=reset,purpose="reset",newPassword=password},HttpStatusCode.OK,"valid reset changes password");
        await Status(guest,root+"redeem",new{token=reset,purpose="reset",newPassword=password},HttpStatusCode.BadRequest,"reset token cannot be replayed");
        foreach(var client in new[]{owner,second}){using var r=await client.GetAsync("/api/account/me");check(r.StatusCode==HttpStatusCode.Unauthorized,"password recovery revokes existing browser session");}
        await Token(owner);await Status(owner,"/api/account/login",new{username="recovery.member",password=previous},HttpStatusCode.Unauthorized,"old password no longer works after reset");await Login(owner);
        await Reset();
        await Status(guest,root+"request/reset",new{email},HttpStatusCode.OK,"new reset request after previous redemption accepted");var expired=await Mail("reset");
        await Sql("UPDATE AccountLinks SET ExpiresAt='2000-01-01' WHERE Purpose='reset';");
        await Status(guest,root+"redeem",new{token=expired,purpose="reset",newPassword=password},HttpStatusCode.BadRequest,"expired link cannot reset password");
        await Status(guest,root+"request/reset",new{email},HttpStatusCode.OK,"expired request can be replaced");var stale=await Mail("reset");
        var nextPassword="Changed-"+Guid.NewGuid();await Status(owner,"/api/account/password",new{currentPassword=password,newPassword=nextPassword},HttpStatusCode.NoContent,"normal password change succeeds with outstanding reset");password=nextPassword;
        await Status(guest,root+"redeem",new{token=stale,purpose="reset",newPassword=password},HttpStatusCode.BadRequest,"password change invalidates earlier reset links");
        await Sql("UPDATE AccountLinks SET IssuedAt='2000-01-01';");
        await Status(guest,root+"request/reset",new{email},HttpStatusCode.OK,"new reset link can replace old one after cooldown");var concurrent=await Mail("reset");
        await Reset();
        var attempts=await Task.WhenAll(Send(guest,root+"redeem",new{token=concurrent,purpose="reset",newPassword=password}),Send(guest,root+"redeem",new{token=concurrent,purpose="reset",newPassword=password}));
        check(attempts.Count(r=>r.StatusCode==HttpStatusCode.OK)==1 && attempts.Count(r=>r.StatusCode==HttpStatusCode.BadRequest)==1,"restart preserves links and concurrent redemption succeeds exactly once");foreach(var r in attempts)r.Dispose();
        await Status(guest,root+"request/username",new{email},HttpStatusCode.OK,"additional username link request succeeds");await Mail("username");
        await Login(owner);await Token(owner);
        using(var request=new HttpRequestMessage(HttpMethod.Delete,"/api/account/me"){Content=JsonContent.Create(new{password})}){request.Headers.Add("X-CSRF-TOKEN",tokens[owner]);using var r=await owner.SendAsync(request);r.EnsureSuccessStatusCode();}
        await using(var db=new SqliteConnection(new SqliteConnectionStringBuilder{DataSource=Path.Combine(state,"members.db")}.ToString())){await db.OpenAsync();using var cmd=db.CreateCommand();cmd.CommandText="SELECT COUNT(*) FROM AccountLinks WHERE Email='recovery@example.invalid';";check(Convert.ToInt64(await cmd.ExecuteScalarAsync())==0,"account deletion removes outstanding recovery links");}
        HttpStatusCode last=0;for(var i=0;i<12;i++){using var r=await guest.GetAsync(root+"status");last=r.StatusCode;}check(last==HttpStatusCode.TooManyRequests,"recovery endpoints have tighter request limits");
        await Reset(false);
    }
}
