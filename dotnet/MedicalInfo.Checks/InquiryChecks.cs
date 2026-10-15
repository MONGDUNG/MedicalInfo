using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.Data.Sqlite;

static class InquiryChecks
{
    public static async Task Run(Uri address,string state,Action<bool,string> check,Func<Task> restart) {
        using var owner=new HttpClient(new HttpClientHandler {CookieContainer=new()}) {BaseAddress=address};
        using var other=new HttpClient(new HttpClientHandler {CookieContainer=new()}) {BaseAddress=address};
        using var admin=new HttpClient(new HttpClientHandler {CookieContainer=new()}) {BaseAddress=address};
        using var guest=new HttpClient {BaseAddress=address};
        var tokens=new Dictionary<HttpClient,string>();var password="Inquiry-"+Guid.NewGuid();
        async Task Ready() {
            for(var i=0;i<40;i++){try{using var response=await guest.GetAsync("/health/live");if(response.IsSuccessStatusCode)return;}catch(HttpRequestException){} await Task.Delay(250);}
            throw new Exception("Inquiry test server not ready");
        }
        async Task Reset(){await restart();await Ready();}
        async Task Token(HttpClient client){using var r=await client.GetAsync("/api/account/csrf");r.EnsureSuccessStatusCode();tokens[client]=(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("token").GetString()!;}
        async Task<HttpResponseMessage> Send(HttpClient client,string path,HttpMethod method,object? body=null){
            using var request=new HttpRequestMessage(method,path);if(body is not null)request.Content=JsonContent.Create(body);
            if(tokens.TryGetValue(client,out var token))request.Headers.Add("X-CSRF-TOKEN",token);
            return await client.SendAsync(request);
        }
        async Task Status(HttpClient client,string path,HttpMethod method,object? body,HttpStatusCode expected,string label){using var r=await Send(client,path,method,body);check(r.StatusCode==expected,label+$" ({(int)r.StatusCode})");}
        async Task<JsonElement> Get(HttpClient client,string path){using var r=await client.GetAsync(path);r.EnsureSuccessStatusCode();return await r.Content.ReadFromJsonAsync<JsonElement>();}
        async Task<long> Create(HttpClient client,string path,object body){using var r=await Send(client,path,HttpMethod.Post,body);r.EnsureSuccessStatusCode();return (await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();}
        var png=Convert.FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
        async Task<HttpResponseMessage> Upload(HttpClient client,string path,string name,byte[] data,bool csrf=true){
            using var request=new HttpRequestMessage(HttpMethod.Post,path);using var form=new MultipartFormDataContent();
            form.Add(new ByteArrayContent(data),"file",name);request.Content=form;
            request.Headers.ExpectContinue=true;
            if(csrf && tokens.TryGetValue(client,out var token))request.Headers.Add("X-CSRF-TOKEN",token);
            return await client.SendAsync(request);
        }
        await Reset();
        foreach(var (client,name) in new[]{(owner,"inquiry.owner"),(other,"inquiry.other"),(admin,"inquiry.admin")}){
            await Token(client);using var register=await Send(client,"/api/account/register",HttpMethod.Post,new{username=name,email=name+"@example.invalid",password,displayName=name});register.EnsureSuccessStatusCode();
            if(client==admin){
                // Seed a role only in this isolated test database; no production test endpoint.
                await using var connection=new SqliteConnection(new SqliteConnectionStringBuilder{DataSource=Path.Combine(state,"members.db")}.ToString());await connection.OpenAsync();
                using var command=connection.CreateCommand();command.CommandText="INSERT OR IGNORE INTO AspNetRoles (Id,Name,NormalizedName,ConcurrencyStamp) VALUES ('test-admin','Admin','ADMIN','test'); INSERT INTO AspNetUserRoles (UserId,RoleId) SELECT u.Id,r.Id FROM AspNetUsers u CROSS JOIN AspNetRoles r WHERE u.UserName='inquiry.admin' AND r.NormalizedName='ADMIN';";await command.ExecuteNonQueryAsync();
            }
            using var login=await Send(client,"/api/account/login",HttpMethod.Post,new{username=name,password});login.EnsureSuccessStatusCode();await Token(client);
        }
        var body=new {title="private-needle",content="private body",visibility="PRIVATE"};
        const string root="/api/inquiries/";
        await Status(guest,root,HttpMethod.Post,body,HttpStatusCode.Unauthorized,"anonymous inquiry creation denied");
        using(var r=await owner.PostAsJsonAsync(root,body))check(r.StatusCode==HttpStatusCode.BadRequest,"inquiry mutation requires CSRF");
        await Status(owner,root,HttpMethod.Post,new{title="bad",content="bad",visibility="typo"},HttpStatusCode.BadRequest,"invalid visibility cannot silently publish inquiry");
        var id=await Create(owner,root,body);var path=root+id;var files="/api/attachments/inquiries/"+id;
        foreach(var client in new[]{guest,other}){
            var list=await Get(client,root+"?keyword=private-needle");check(list.GetProperty("total").GetInt32()==0,"private title absent from results and counts");
            await Status(client,path,HttpMethod.Get,null,HttpStatusCode.NotFound,"private direct URL hidden");
            await Status(client,path+"/answers",HttpMethod.Get,null,HttpStatusCode.NotFound,"private answer list hidden");
            await Status(client,files,HttpMethod.Get,null,HttpStatusCode.NotFound,"private attachment list hidden");
        }
        check((await Get(owner,path)).GetProperty("canEdit").GetBoolean(),"private inquiry owner can read and edit");
        check((await Get(admin,path)).GetProperty("canEdit").GetBoolean(),"administrator can access private inquiry");
        await Status(other,path+"/answers",HttpMethod.Post,new{content="intrusion"},HttpStatusCode.NotFound,"nonowner cannot answer private inquiry");
        var answer=await Create(admin,path+"/answers",new{content="secret-answer-needle"});
        check((await Get(guest,root+"?keyword=secret-answer-needle")).GetProperty("total").GetInt32()==0,"private answer search does not leak inquiry");
        using(var r=await Upload(owner,files,"tiny.png",png,false))check(r.StatusCode==HttpStatusCode.BadRequest,"multipart uploads require CSRF");
        using(var r=await Upload(other,files,"tiny.png",png))check(r.StatusCode==HttpStatusCode.NotFound,"nonowner cannot upload private attachment");
        using(var r=await Upload(owner,files,"script.html",png))check(r.StatusCode==HttpStatusCode.BadRequest,"active file extension rejected");
        using(var r=await Upload(owner,files,"fake.png","<html>bad</html>"u8.ToArray()))check(r.StatusCode==HttpStatusCode.BadRequest,"forged image extension rejected");
        using(var r=await Upload(owner,files,"big.png",new byte[5*1024*1024+65537]))check(r.StatusCode==HttpStatusCode.RequestEntityTooLarge,"oversized upload rejected");
        string attachment;
        using(var r=await Upload(owner,files,"../../tiny.png",png)){r.EnsureSuccessStatusCode();var value=await r.Content.ReadFromJsonAsync<JsonElement>();attachment="/api/attachments/"+value.GetProperty("id").GetString();check(value.GetProperty("name").GetString()=="tiny.png","client path stripped from download filename");}
        foreach(var client in new[]{guest,other})await Status(client,attachment,HttpMethod.Get,null,HttpStatusCode.NotFound,"private file URL cannot bypass parent access");
        using(var r=await admin.GetAsync(attachment))check(r.IsSuccessStatusCode && (await r.Content.ReadAsByteArrayAsync()).SequenceEqual(png) && r.Content.Headers.ContentDisposition?.DispositionType=="attachment" && r.Headers.CacheControl?.NoStore==true,"administrator downloads exact bytes with attachment and no-store headers");
        for(var i=0;i<2;i++){using var r=await Upload(owner,files,"tiny.png",png);r.EnsureSuccessStatusCode();}
        using(var r=await Upload(owner,files,"tiny.png",png))check(r.StatusCode==HttpStatusCode.Conflict,"per-parent attachment quota enforced");
        await Reset();
        using(var r=await owner.GetAsync(attachment))check(r.IsSuccessStatusCode && (await r.Content.ReadAsByteArrayAsync()).SequenceEqual(png),"private attachments and session survive restart");
        var publicBody=new{title="public inquiry",content="public body",visibility="PUBLIC"};
        await Status(owner,path,HttpMethod.Put,publicBody,HttpStatusCode.NoContent,"owner can publish inquiry");
        await Status(guest,attachment,HttpMethod.Get,null,HttpStatusCode.OK,"public inquiry file downloadable");
        var publicAnswer=await Create(other,path+"/answers",new{content="member answer"});
        await Status(owner,root+"answers/"+publicAnswer,HttpMethod.Put,new{content="overwrite"},HttpStatusCode.Forbidden,"inquiry owner cannot overwrite another member answer");
        await Status(other,path,HttpMethod.Delete,null,HttpStatusCode.Forbidden,"public inquiry still requires owner to delete");
        await Status(other,attachment,HttpMethod.Delete,null,HttpStatusCode.Forbidden,"public file deletion requires parent ownership");
        await Status(other,root+"answers/"+publicAnswer,HttpMethod.Put,new{content="edited"},HttpStatusCode.NoContent,"public answer author can edit");
        await Status(owner,path,HttpMethod.Put,body,HttpStatusCode.NoContent,"inquiry can become private");
        await Status(other,root+"answers/"+publicAnswer,HttpMethod.Delete,null,HttpStatusCode.NotFound,"answer author loses access when parent becomes private");
        await Status(guest,attachment,HttpMethod.Get,null,HttpStatusCode.NotFound,"previous public download URL respects changed privacy");
        await Status(admin,root+"answers/"+answer,HttpMethod.Put,new{content="admin edited"},HttpStatusCode.NoContent,"administrator can edit private answer");
        var post=await Create(owner,"/api/community/posts",new{title="attachment post",content="body",category="자유"});var postFiles="/api/attachments/posts/"+post;string postFile;
        using(var r=await Upload(owner,postFiles,"tiny.png",png)){r.EnsureSuccessStatusCode();postFile="/api/attachments/"+(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetString();}
        await Status(guest,postFile,HttpMethod.Get,null,HttpStatusCode.OK,"board attachment is public");
        using(var r=await Upload(other,postFiles,"tiny.png",png))check(r.StatusCode==HttpStatusCode.Forbidden,"nonowner cannot attach to public board post");
        await Status(owner,"/api/community/posts/"+post,HttpMethod.Delete,null,HttpStatusCode.NoContent,"board post deletion succeeds");
        await Status(guest,postFile,HttpMethod.Get,null,HttpStatusCode.NotFound,"board deletion removes attachment bytes");
        var disposable=await Create(owner,root,publicBody);
        await Create(other,root+disposable+"/answers",new{content="delete cascade"});
        await Status(owner,root+disposable,HttpMethod.Delete,null,HttpStatusCode.NoContent,"owner can delete inquiry with replies");
        await Status(guest,root+disposable+"/answers",HttpMethod.Get,null,HttpStatusCode.NotFound,"deleted inquiry replies are inaccessible");
        var attached=(await Get(owner,files))[1].GetProperty("id").GetString();
        await Status(owner,"/api/attachments/"+attached,HttpMethod.Delete,null,HttpStatusCode.NoContent,"owner can delete individual attachment");
        using(var r=await Upload(owner,files,"replacement.png",png))check(r.IsSuccessStatusCode,"deleting attachment releases quota");
        await Status(owner,"/api/account/me",HttpMethod.Delete,new{password},HttpStatusCode.NoContent,"inquiry author can withdraw");
        await Status(admin,path,HttpMethod.Get,null,HttpStatusCode.NotFound,"withdrawal deletes authored inquiry even for administrator");
        await Status(admin,attachment,HttpMethod.Get,null,HttpStatusCode.NotFound,"withdrawal deletes inquiry attachments");
        await using var db=new SqliteConnection(new SqliteConnectionStringBuilder{DataSource=Path.Combine(state,"members.db")}.ToString());await db.OpenAsync();
        using var count=db.CreateCommand();count.CommandText="SELECT (SELECT COUNT(*) FROM InquiryAnswers WHERE InquiryId=$id)+(SELECT COUNT(*) FROM Attachments WHERE InquiryId=$id);";count.Parameters.AddWithValue("$id",id);
        check(Convert.ToInt64(await count.ExecuteScalarAsync())==0,"withdrawal cascades answer and attachment storage without orphan rows");
    }
}
