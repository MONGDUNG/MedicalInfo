using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using MedicalInfo;
using Microsoft.Data.Sqlite;

static class MemberDetailsChecks
{
    public static async Task Run(Uri address,string state,Action<bool,string> check,Func<Task> restart)
    {
        using var owner=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var other=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var guest=new HttpClient{BaseAddress=address};
        async Task Ready(){await restart();for(var i=0;i<40;i++){try{using var r=await guest.GetAsync("/health/live");if(r.IsSuccessStatusCode)return;}catch(HttpRequestException){}await Task.Delay(250);}throw new Exception("Details server not ready");}
        async Task<HttpResponseMessage> Send(HttpClient c,string path,HttpMethod method,object data){
            var csrf=await c.GetFromJsonAsync<JsonElement>("/api/account/csrf");using var message=new HttpRequestMessage(method,path){Content=JsonContent.Create(data)};
            message.Headers.Add("X-CSRF-TOKEN",csrf.GetProperty("token").GetString());return await c.SendAsync(message);
        }
        await Ready();
        using(var r=await guest.GetAsync("/api/account/details"))check(r.StatusCode==HttpStatusCode.Unauthorized,"private details require authentication");
        var password="Details-"+Guid.NewGuid();
        foreach(var(c,name)in new[]{(owner,"details.owner"),(other,"details.other")}) {
            using(var r=await Send(c,"/api/account/register",HttpMethod.Post,new{username=name,email=name+"@example.invalid",displayName=name,password}))r.EnsureSuccessStatusCode();
            using(var r=await Send(c,"/api/account/login",HttpMethod.Post,new{username=name,password}))r.EnsureSuccessStatusCode();
        }
        var input=new MemberDetailsInput("본인 전용 이름","테스트 주소","상세","01234",new DateOnly(2000,1,1),"female",37.5,127,"fixture-health-note",true);
        using(var r=await owner.PutAsJsonAsync("/api/account/details",input))check(r.StatusCode==HttpStatusCode.BadRequest,"private profile mutation requires CSRF");
        using(var r=await Send(owner,"/api/account/details",HttpMethod.Put,input))check(r.IsSuccessStatusCode,"optional private member details saved");
        var details=await owner.GetFromJsonAsync<JsonElement>("/api/account/details");
        check(details.GetProperty("postcode").GetString()=="01234" && details.GetProperty("healthConsentAt").ValueKind==JsonValueKind.String,"leading-zero postcode and health consent timestamp preserved");
        var unrelated=await other.GetFromJsonAsync<JsonElement>("/api/account/details?memberId=details.owner");
        check(unrelated.GetProperty("fullName").ValueKind==JsonValueKind.Null,"other member cannot select someone else's private profile");
        var me=await owner.GetStringAsync("/api/account/me");check(!me.Contains("fixture-health-note")&&!me.Contains("본인 전용 이름"),"general profile response excludes private information");
        using(var db=new SqliteConnection("Data Source="+Path.Combine(state,"members.db"))) {
            await db.OpenAsync();using var cmd=db.CreateCommand();cmd.CommandText="SELECT ProtectedDetails FROM AspNetUsers WHERE UserName='details.owner'";
            var stored=(string)(await cmd.ExecuteScalarAsync())!;check(!stored.Contains("fixture-health-note")&&!stored.Contains("01234"),"private profile encrypted at rest");
        }
        foreach(var invalid in new[]{input with{Postcode="1234"},input with{BirthDate=DateOnly.FromDateTime(DateTime.UtcNow.AddDays(1))},input with{Latitude=91},input with{Longitude=null},input with{Sex="invalid"},input with{HealthConsent=false}})
            using(var r=await Send(owner,"/api/account/details",HttpMethod.Put,invalid))check(r.StatusCode==HttpStatusCode.BadRequest,"invalid private profile rejected without saving");
        await Ready();details=await owner.GetFromJsonAsync<JsonElement>("/api/account/details");
        check(details.GetProperty("underlyingCondition").GetString()=="fixture-health-note","encrypted details and cookie survive restart with persistent keys");
        using(var r=await Send(owner,"/api/account/details",HttpMethod.Put,input with{UnderlyingCondition=null,HealthConsent=false}))r.EnsureSuccessStatusCode();
        details=await owner.GetFromJsonAsync<JsonElement>("/api/account/details");check(details.GetProperty("healthConsentAt").ValueKind==JsonValueKind.Null&&details.GetProperty("underlyingCondition").ValueKind==JsonValueKind.Null,"health consent withdrawal removes health memo and timestamp");
        var now=DateTime.UtcNow;
        check(MemberDetails.Inactive(new Member{CreatedAt=now.AddYears(-1)},now)&&!MemberDetails.Inactive(new Member{CreatedAt=now.AddYears(-1),LastLoginAt=now},now),"inactivity uses creation fallback and successful login reactivates status");
        using(var db=new SqliteConnection("Data Source="+Path.Combine(state,"members.db"))) {
            await db.OpenAsync();using var cmd=db.CreateCommand();cmd.CommandText="UPDATE AspNetUsers SET ProtectedDetails='unreadable' WHERE UserName='details.owner'";await cmd.ExecuteNonQueryAsync();
        }
        using(var r=await owner.GetAsync("/api/account/details"))check(r.StatusCode==HttpStatusCode.ServiceUnavailable,"unreadable private profile is unavailable rather than empty success");
        using(var r=await Send(owner,"/api/account/details",HttpMethod.Put,input))check(r.StatusCode==HttpStatusCode.ServiceUnavailable,"unreadable private profile cannot be silently overwritten");
        using(var r=await Send(owner,"/api/account/me",HttpMethod.Delete,new{password}))r.EnsureSuccessStatusCode();
        using(var db=new SqliteConnection("Data Source="+Path.Combine(state,"members.db"))) {
            await db.OpenAsync();using var cmd=db.CreateCommand();cmd.CommandText="SELECT COUNT(*) FROM AspNetUsers WHERE UserName='details.owner'";
            check((long)(await cmd.ExecuteScalarAsync())! == 0,"member withdrawal deletes private profile together with member");
        }
    }
}
