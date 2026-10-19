using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.Data.Sqlite;

static class AdministrationChecks
{
    public static async Task Run(Uri address,string state,Action<bool,string> check,Func<Task> restart,Func<string,Task<int>> bootstrap) {
        using var admin=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var member=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var second=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var guest=new HttpClient{BaseAddress=address};
        var tokens=new Dictionary<HttpClient,string>();var password="Administration-"+Guid.NewGuid();
        async Task Ready(){for(var i=0;i<40;i++){try{using var r=await guest.GetAsync("/health/live");if(r.IsSuccessStatusCode)return;}catch(HttpRequestException){}await Task.Delay(250);}throw new Exception("Admin test server not ready");}
        async Task Reset(){await restart();await Ready();}
        async Task Token(HttpClient client){using var r=await client.GetAsync("/api/account/csrf");r.EnsureSuccessStatusCode();tokens[client]=(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("token").GetString()!;}
        async Task<HttpResponseMessage> Send(HttpClient client,string path,HttpMethod method,object? body=null){using var request=new HttpRequestMessage(method,path);if(body is not null)request.Content=JsonContent.Create(body);if(tokens.TryGetValue(client,out var token))request.Headers.Add("X-CSRF-TOKEN",token);return await client.SendAsync(request);}
        async Task Status(HttpClient client,string path,HttpMethod method,object? body,HttpStatusCode expected,string label){using var r=await Send(client,path,method,body);check(r.StatusCode==expected,label+$" ({(int)r.StatusCode})");}
        async Task<JsonElement> Get(HttpClient client,string path){using var r=await client.GetAsync(path);r.EnsureSuccessStatusCode();return await r.Content.ReadFromJsonAsync<JsonElement>();}
        async Task Login(HttpClient client,string name){await Token(client);using var r=await Send(client,"/api/account/login",HttpMethod.Post,new{username=name,password});r.EnsureSuccessStatusCode();await Token(client);}
        await Reset();
        foreach(var(client,name)in new[]{(admin,"admin.operator"),(member,"admin.member")}){
            await Token(client);using var r=await Send(client,"/api/account/register",HttpMethod.Post,new{username=name,email=name+"@example.invalid",password,displayName=name});r.EnsureSuccessStatusCode();await Login(client,name);
        }
        var adminId=(await Get(admin,"/api/account/me")).GetProperty("id").GetString()!;
        var memberId=(await Get(member,"/api/account/me")).GetProperty("id").GetString()!;
        const string root="/api/admin/";var target=root+"members/"+memberId;
        await Status(guest,root+"members",HttpMethod.Get,null,HttpStatusCode.Unauthorized,"admin list requires login");
        await Status(member,root+"members",HttpMethod.Get,null,HttpStatusCode.Forbidden,"ordinary member cannot list members");
        await Status(member,target+"/role",HttpMethod.Put,new{role="Admin",reason="injection"},HttpStatusCode.Forbidden,"ordinary member cannot grant administrator");
        check(await bootstrap("unknown.member")==1,"bootstrap rejects unknown account");
        check(await bootstrap("admin.operator")==0,"host command provisions first existing administrator");
        await Status(admin,"/api/account/me",HttpMethod.Get,null,HttpStatusCode.Unauthorized,"bootstrap invalidates previous member session");
        await Login(admin,"admin.operator");
        check(await bootstrap("admin.member")==1,"bootstrap cannot be reused after administrator exists");
        check((await Get(admin,"/api/account/me")).GetProperty("isAdmin").GetBoolean(),"profile exposes administrator navigation only to role holder");
        var listing=await Get(admin,root+"members?keyword=admin.member");
        check(listing.GetProperty("total").GetInt32()==1 && listing.GetProperty("items")[0].GetProperty("id").GetString()==memberId,"admin search count and page share filter");
        check(!listing.ToString().Contains("password",StringComparison.OrdinalIgnoreCase)&&!listing.ToString().Contains("securityStamp",StringComparison.OrdinalIgnoreCase),"admin list excludes password hashes and stamps");
        await Status(admin,root+"members?page=0",HttpMethod.Get,null,HttpStatusCode.BadRequest,"admin pagination validates bounds");
        using(var r=await admin.PutAsJsonAsync(target+"/role",new{role="Admin",reason="csrf"}))check(r.StatusCode==HttpStatusCode.BadRequest,"administrator role change requires CSRF");
        await Status(admin,root+"members/"+adminId+"/role",HttpMethod.Put,new{role="Member",reason="self"},HttpStatusCode.Conflict,"administrator cannot demote self");
        await Status(admin,"/api/account/me",HttpMethod.Delete,new{password},HttpStatusCode.Conflict,"last administrator cannot withdraw");
        await Status(admin,target+"/role",HttpMethod.Put,new{role="unknown",reason="bad"},HttpStatusCode.BadRequest,"unknown role rejected");
        await Status(admin,target+"/role",HttpMethod.Put,new{role="Doctor",reason=""},HttpStatusCode.BadRequest,"role change requires reason");
        await Status(admin,target+"/role",HttpMethod.Put,new{role="Doctor",reason="qualification checked"},HttpStatusCode.NoContent,"administrator can assign professional role");
        await Status(member,"/api/account/me",HttpMethod.Get,null,HttpStatusCode.Unauthorized,"role change invalidates existing session");
        await Login(member,"admin.member");
        await Status(member,root+"members",HttpMethod.Get,null,HttpStatusCode.Forbidden,"professional role does not imply administration");
        await Status(admin,target+"/suspension",HttpMethod.Put,new{days=7,reason="policy violation"},HttpStatusCode.NoContent,"administrator can apply timed suspension");
        await Status(member,"/api/account/me",HttpMethod.Get,null,HttpStatusCode.Unauthorized,"suspension revokes existing session immediately");
        await Token(member);
        await Status(member,"/api/account/login",HttpMethod.Post,new{username="admin.member",password},HttpStatusCode.Unauthorized,"suspended member cannot sign in with correct password");
        await Status(admin,target+"/role",HttpMethod.Put,new{role="Admin",reason="bad promotion"},HttpStatusCode.Conflict,"suspended account cannot become administrator");
        await Status(admin,target+"/suspension",HttpMethod.Put,new{days=366,reason="too long"},HttpStatusCode.BadRequest,"suspension duration bounded");
        await Reset();
        await Status(member,"/api/account/login",HttpMethod.Post,new{username="admin.member",password},HttpStatusCode.Unauthorized,"suspension persists across restart");
        await Status(admin,target+"/suspension",HttpMethod.Put,new{days=0,reason="appeal accepted"},HttpStatusCode.NoContent,"administrator can lift suspension");
        await Login(member,"admin.member");await Login(second,"admin.member");
        long tier;
        using(var r=await Send(admin,root+"tiers",HttpMethod.Post,new{code="supporter",name="활동 회원"})){r.EnsureSuccessStatusCode();tier=(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();}
        await Status(admin,root+"tiers",HttpMethod.Post,new{code="supporter",name="중복"},HttpStatusCode.Conflict,"tier codes unique");
        await Status(admin,target+"/tier",HttpMethod.Put,new{tierId=tier,reason="contribution"},HttpStatusCode.NoContent,"administrator can assign member tier");
        check((await Get(member,"/api/account/me")).GetProperty("tierId").GetInt64()==tier,"member can see assigned tier identifier");
        await Status(admin,root+"tiers/"+tier,HttpMethod.Delete,null,HttpStatusCode.Conflict,"assigned tier cannot be deleted");
        await Status(admin,root+"tiers/"+tier,HttpMethod.Put,new{name="이름 변경"},HttpStatusCode.NoContent,"tier name can change without changing identity");
        await Status(admin,target+"/tier",HttpMethod.Put,new{tierId=(long?)null,reason="clear tier"},HttpStatusCode.NoContent,"tier can be unassigned");
        await Status(admin,root+"tiers/"+tier,HttpMethod.Delete,null,HttpStatusCode.NoContent,"unused tier can be deleted");
        await Status(admin,target+"/tier",HttpMethod.Put,new{tierId=tier,reason="stale"},HttpStatusCode.BadRequest,"deleted tier cannot be assigned");
        await Status(admin,target+"/role",HttpMethod.Put,new{role="Admin",reason="second operator"},HttpStatusCode.NoContent,"administrator can appoint another administrator");
        await Status(second,"/api/account/me",HttpMethod.Get,null,HttpStatusCode.Unauthorized,"role update revokes other browser sessions");
        await Login(member,"admin.member");
        await Status(member,root+"members",HttpMethod.Get,null,HttpStatusCode.OK,"new administrator has access after login");
        await Status(admin,target+"/suspension",HttpMethod.Put,new{days=7,reason="cannot ban admin"},HttpStatusCode.Conflict,"administrator must be demoted before suspension");
        await Status(admin,target+"/role",HttpMethod.Put,new{role="Member",reason="remove elevated access"},HttpStatusCode.NoContent,"administrator can revoke another administrator role");
        await Status(member,root+"members",HttpMethod.Get,null,HttpStatusCode.Unauthorized,"revoked administrator cannot reuse old session");
        await Login(member,"admin.member");
        await Status(member,root+"audit",HttpMethod.Get,null,HttpStatusCode.Forbidden,"audit history is administrator only");
        var audit=await Get(admin,root+"audit");var events=audit.GetProperty("items").EnumerateArray().ToArray();
        check(events.Any(x=>x.GetProperty("action").GetString()=="bootstrap-admin") && events.Any(x=>x.GetProperty("reason").GetString()=="remove elevated access"),"audit captures bootstrap and role change reasons");
        check(!audit.ToString().Contains(password),"audit does not expose account password");
        // Force expiry only in this isolated fixture, then exercise normal sign-in.
        await using(var db=new SqliteConnection(new SqliteConnectionStringBuilder{DataSource=Path.Combine(state,"members.db")}.ToString())){
            await db.OpenAsync();using var command=db.CreateCommand();command.CommandText="UPDATE AspNetUsers SET SuspendedUntil=$until WHERE Id=$id";command.Parameters.AddWithValue("$until",DateTime.UtcNow.AddDays(-1));command.Parameters.AddWithValue("$id",memberId);await command.ExecuteNonQueryAsync();
        }
        await Login(member,"admin.member");check(true,"expired suspension permits normal login without manual unban");
        await Reset();
        check((await Get(admin,root+"audit")).GetProperty("total").GetInt32()==audit.GetProperty("total").GetInt32(),"audit survives restart");
        using(var database=new SqliteConnection("Data Source="+Path.Combine(state,"members.db"))) {
            await database.OpenAsync();using var command=database.CreateCommand();command.CommandText="UPDATE AspNetUsers SET LastLoginAt=$last WHERE Id=$id";
            command.Parameters.AddWithValue("$last",DateTime.UtcNow.AddMonths(-7));command.Parameters.AddWithValue("$id",memberId);await command.ExecuteNonQueryAsync();
        }
        var inactive=await Get(admin,root+"members?inactive=true&keyword=admin.member");
        check(inactive.GetProperty("total").GetInt32()==1 && inactive.GetProperty("items")[0].GetProperty("inactive").GetBoolean(),"administrator can filter inactive members without exposing private profile");
        await Login(member,"admin.member");
        check((await Get(admin,root+"members?inactive=true&keyword=admin.member")).GetProperty("total").GetInt32()==0,"successful password login clears derived inactivity without manual unlock");
    }
}
