using System.Net;
using System.Net.Http.Json;
using System.Text.Json;

static class ProductChecks
{
    public static async Task Run(Uri address,Action<bool,string> check,Func<bool,Task> restart){
        using var owner=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var other=new HttpClient(new HttpClientHandler{CookieContainer=new()}){BaseAddress=address};
        using var guest=new HttpClient{BaseAddress=address};var tokens=new Dictionary<HttpClient,string>();var password="Product-"+Guid.NewGuid();
        async Task Reset(bool enabled=true){await restart(enabled);for(var i=0;i<40;i++){try{using var r=await guest.GetAsync("/health/live");if(r.IsSuccessStatusCode)return;}catch(HttpRequestException){}await Task.Delay(250);}throw new Exception("Product check server not ready");}
        async Task Token(HttpClient c){using var r=await c.GetAsync("/api/account/csrf");r.EnsureSuccessStatusCode();tokens[c]=(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("token").GetString()!;}
        async Task<HttpResponseMessage> Send(HttpClient c,string path,HttpMethod method,object? data=null){using var r=new HttpRequestMessage(method,path);if(data is not null)r.Content=JsonContent.Create(data);if(tokens.TryGetValue(c,out var t))r.Headers.Add("X-CSRF-TOKEN",t);return await c.SendAsync(r);}
        async Task Status(HttpClient c,string path,HttpMethod method,object? data,HttpStatusCode expected,string label){using var r=await Send(c,path,method,data);check(r.StatusCode==expected,label+$" ({(int)r.StatusCode})");}
        async Task<JsonElement> Get(HttpClient c,string path){using var r=await c.GetAsync(path);r.EnsureSuccessStatusCode();return await r.Content.ReadFromJsonAsync<JsonElement>();}
        await Reset();
        foreach(var(c,name)in new[]{(owner,"product.owner"),(other,"product.other")}){await Token(c);using(var r=await Send(c,"/api/account/register",HttpMethod.Post,new{username=name,email=name+"@example.invalid",password,displayName=name}))r.EnsureSuccessStatusCode();using(var r=await Send(c,"/api/account/login",HttpMethod.Post,new{username=name,password}))r.EnsureSuccessStatusCode();await Token(c);}
        const string product="/api/products/supplements/one",drug="/api/products/drugs/one";
        check((await Get(guest,"/api/supplements")).GetProperty("total").GetInt32()==2,"empty query exposes complete supplement catalog");
        var filtered=await Get(guest,"/api/supplements?effect="+Uri.EscapeDataString("장 건강"));check(filtered.GetProperty("total").GetInt32()==1&&filtered.GetProperty("items")[0].GetProperty("id").GetString()=="two","official functionality filter and count agree");
        check((await Get(guest,"/api/supplements?keyword=zeta&effect="+Uri.EscapeDataString("장 건강"))).GetProperty("total").GetInt32()==0,"keyword and functionality filters intersect");
        check((await Get(guest,"/api/drugs?effect="+Uri.EscapeDataString("해열"))).GetProperty("total").GetInt32()==1,"drug efficacy filter searches official text");
        var permitSearch=await Get(guest,"/api/drugs?ingredient="+Uri.EscapeDataString("다른 주성분"));
        check(permitSearch.GetProperty("total").GetInt32()==1&&permitSearch.GetProperty("items")[0].GetProperty("id").GetString()=="permit-only","API ingredient filter finds products absent from easy catalog");
        check((await Get(guest,"/api/drugs?ingredient="+Uri.EscapeDataString("다른 주성분")+"&effect="+Uri.EscapeDataString("해열"))).GetProperty("total").GetInt32()==0,"API ingredient and efficacy filters do not cross product identities");
        var permitDetail=await Get(guest,"/api/drugs/permit-only");
        check(permitDetail.GetProperty("permitDetails")[0].GetProperty("MAIN_ITEM_INGR").GetString()=="한글시험성분"&&(await Get(guest,"/api/drugs?effect="+Uri.EscapeDataString("공식원문 시험효능"))).GetProperty("total").GetInt32()==1,"API serves official permit documents and searches their efficacy without easy description");
        check((await Get(guest,"/api/drugs?ingredient="+Uri.EscapeDataString("한글시험성분"))).GetProperty("items")[0].GetProperty("id").GetString()=="permit-only","API searches Korean active ingredient source fields");
        check(permitDetail.GetProperty("variants").GetArrayLength()==0&&permitDetail.GetProperty("permits")[0].GetProperty("CANCEL_NAME").GetString()=="취하","permit-only detail preserves source status without fabricated easy information");
        await Status(owner,"/api/products/drugs/permit-only/comments",HttpMethod.Post,new{content="permit lookup discussion"},HttpStatusCode.OK,"official permit-only product supports existing product comments");
        await Status(guest,"/api/drugs?ingredient="+new string('x',101),HttpMethod.Get,null,HttpStatusCode.BadRequest,"ingredient filter length bounded");
        await Status(guest,"/api/supplements?sort=unsafe",HttpMethod.Get,null,HttpStatusCode.BadRequest,"unsupported ranking rejected");
        await Status(guest,"/api/supplements?page=100001",HttpMethod.Get,null,HttpStatusCode.BadRequest,"catalog pagination bounded");
        await Status(guest,product+"/comments",HttpMethod.Post,new{content="anonymous"},HttpStatusCode.Unauthorized,"anonymous product comment denied");
        using(var r=await owner.PostAsJsonAsync(product+"/comments",new{content="no csrf"}))check(r.StatusCode==HttpStatusCode.BadRequest,"product comment requires CSRF");
        await Status(owner,"/api/products/supplements/missing/comments",HttpMethod.Post,new{content="missing"},HttpStatusCode.NotFound,"comments require an existing official product");
        await Status(owner,product+"/comments",HttpMethod.Post,new{content=new string('a',2001)},HttpStatusCode.BadRequest,"oversized product comment rejected");
        long comment;
        using(var r=await Send(owner,product+"/comments",HttpMethod.Post,new{content="<script>plain text</script>",authorId="fake"})){r.EnsureSuccessStatusCode();comment=(await r.Content.ReadFromJsonAsync<JsonElement>()).GetProperty("id").GetInt64();}
        var comments=await Get(guest,product+"/comments");check(comments.GetProperty("items")[0].GetProperty("author").GetString()=="product.owner"&&!comments.GetProperty("items")[0].GetProperty("canEdit").GetBoolean(),"comment author derives from session and guests cannot edit");
        check((await Get(guest,drug+"/comments")).GetProperty("total").GetInt32()==0,"same product ID in different catalogs does not mix comments");
        await Status(owner,drug+"/comments/"+comment,HttpMethod.Put,new{content="wrong target"},HttpStatusCode.NotFound,"comment cannot be changed through another catalog");
        await Status(other,product+"/comments/"+comment,HttpMethod.Put,new{content="intrusion"},HttpStatusCode.Forbidden,"other member cannot edit product comment");
        await Status(other,product+"/comments/"+comment,HttpMethod.Delete,null,HttpStatusCode.Forbidden,"other member cannot delete product comment");
        await Status(owner,product+"/comments/"+comment,HttpMethod.Put,new{content="edited"},HttpStatusCode.NoContent,"owner can edit product comment");
        await Status(guest,product+"/recommendation",HttpMethod.Put,null,HttpStatusCode.Unauthorized,"recommendation requires login");
        using(var r=await owner.PutAsJsonAsync(product+"/recommendation",new{}))check(r.StatusCode==HttpStatusCode.BadRequest,"recommendation requires CSRF");
        await Status(owner,"/api/products/supplements/missing/recommendation",HttpMethod.Put,null,HttpStatusCode.NotFound,"recommendation requires existing product");
        await Status(owner,drug+"/recommendation",HttpMethod.Put,null,HttpStatusCode.NotFound,"drug recommendation is not introduced as a new legacy feature");
        var votes=await Task.WhenAll(Send(owner,product+"/recommendation",HttpMethod.Put),Send(owner,product+"/recommendation",HttpMethod.Put));foreach(var r in votes){r.EnsureSuccessStatusCode();r.Dispose();}
        var voted=await Get(owner,product+"/recommendation");check(voted.GetProperty("total").GetInt32()==1&&voted.GetProperty("recommended").GetBoolean(),"concurrent duplicate recommendations count once");
        check(!(await Get(other,product+"/recommendation")).GetProperty("recommended").GetBoolean(),"recommendation selection belongs to current user");
        await Status(other,product+"/recommendation",HttpMethod.Put,null,HttpStatusCode.NoContent,"second member can independently recommend");
        var ranked=await Get(guest,"/api/supplements?sort=popular");check(ranked.GetProperty("items")[0].GetProperty("id").GetString()=="one"&&ranked.GetProperty("items")[0].GetProperty("recommendations").GetInt32()==2,"recommendation ranking uses actual unique member votes");
        await Reset();
        check((await Get(owner,product+"/recommendation")).GetProperty("total").GetInt32()==2&&(await Get(owner,product+"/comments")).GetProperty("items")[0].GetProperty("content").GetString()=="edited","product comments and recommendations persist across restart");
        await Status(owner,product+"/recommendation",HttpMethod.Delete,null,HttpStatusCode.NoContent,"member can cancel own recommendation");
        await Status(owner,product+"/recommendation",HttpMethod.Delete,null,HttpStatusCode.NoContent,"repeated recommendation cancellation is idempotent");
        check((await Get(other,product+"/recommendation")).GetProperty("total").GetInt32()==1,"cancelling recommendation preserves other member vote");
        await Status(other,"/api/account/me",HttpMethod.Delete,new{password},HttpStatusCode.NoContent,"member with recommendation can withdraw");
        check((await Get(guest,product+"/recommendation")).GetProperty("total").GetInt32()==0,"withdrawal removes member recommendations");
        await Status(owner,"/api/account/me",HttpMethod.Delete,new{password},HttpStatusCode.NoContent,"product comment author can withdraw");
        check((await Get(guest,product+"/comments")).GetProperty("items")[0].GetProperty("author").GetString()=="탈퇴 회원","withdrawal anonymizes public product comments");
        await Reset(false);
        await Status(guest,product+"/comments",HttpMethod.Get,null,HttpStatusCode.ServiceUnavailable,"missing official dataset reports unavailable instead of empty discussion");
    }
}
