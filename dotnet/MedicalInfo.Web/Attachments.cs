using Microsoft.AspNetCore.Http.Features;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class ContentAttachment
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public long? PostId { get; set; }
    public long? InquiryId { get; set; }
    public string Name { get; set; } = "";
    public string ContentType { get; set; } = "";
    public int Length { get; set; }
    public byte[] Data { get; set; } = [];
}

public static class Attachments
{
    private const int MaxSize = 5 * 1024 * 1024;
    private static async Task<(bool Visible, bool Editable)> Access(string kind, long id, MemberDatabase db, HttpContext context) {
        if(kind=="posts") {
            var post=await db.Posts.AsNoTracking().SingleOrDefaultAsync(x=>x.Id==id);
            return (post is not null,post is not null && Inquiries.Owns(context.User,post.AuthorId) && (post.Category!="공지사항" || context.User.IsInRole("Admin")));
        }
        if(kind=="inquiries") {
            var item=await Inquiries.Visible(db,context.User).AsNoTracking().SingleOrDefaultAsync(x=>x.Id==id);
            return (item is not null,item is not null && Inquiries.Owns(context.User,item.AuthorId));
        }
        return (false,false);
    }
    private static IQueryable<ContentAttachment> Parent(MemberDatabase db,string kind,long id) =>
        db.Attachments.Where(x=>kind=="posts" ? x.PostId==id : x.InquiryId==id);
    private static string? Detect(byte[] data,string name) {
        var extension=Path.GetExtension(name).ToLowerInvariant();
        if(extension==".png" && data.AsSpan().StartsWith(new byte[]{137,80,78,71,13,10,26,10})) return "image/png";
        if(extension is ".jpg" or ".jpeg" && data.AsSpan().StartsWith(new byte[]{255,216,255})) return "image/jpeg";
        if(extension==".webp" && data.Length>=12 && data.AsSpan(0,4).SequenceEqual("RIFF"u8) && data.AsSpan(8,4).SequenceEqual("WEBP"u8)) return "image/webp";
        return null;
    }
    public static void MapAttachments(this WebApplication app) {
        var group=Inquiries.Protect(app.MapGroup("/api/attachments"));
        group.MapGet("/{kind}/{parent:long}",async(string kind,long parent,MemberDatabase db,HttpContext context)=>{
            if(!(await Access(kind,parent,db,context)).Visible) return Results.NotFound();
            return Results.Ok(await Parent(db,kind,parent).AsNoTracking().Select(x=>new {x.Id,x.Name,x.Length}).ToArrayAsync());
        });
        group.MapPost("/{kind}/{parent:long}",async(string kind,long parent,MemberDatabase db,HttpContext context)=>{
            var access=await Access(kind,parent,db,context);
            if(!access.Visible) return Results.NotFound();
            if(!access.Editable) return Results.Forbid();
            var limit=context.Features.Get<IHttpMaxRequestBodySizeFeature>();
            if(limit is {IsReadOnly:false}) limit.MaxRequestBodySize=MaxSize+65536;
            if(context.Request.ContentLength>MaxSize+65536) return Results.StatusCode(413);
            if(!context.Request.HasFormContentType) return Results.BadRequest();
            IFormCollection form;
            try { form=await context.Request.ReadFormAsync(new FormOptions { MultipartBodyLengthLimit=MaxSize, ValueCountLimit=4 },context.RequestAborted); }
            catch(InvalidDataException) { return Results.Problem(statusCode:400,title:"파일 크기 또는 업로드 형식이 올바르지 않습니다."); }
            catch(BadHttpRequestException) { return Results.StatusCode(413); }
            if(form.Files.Count!=1 || form.Files[0].Length is <=0 or >MaxSize) return Results.BadRequest();
            var file=form.Files[0];
            var name=file.FileName.Replace('\\','/').Split('/').Last();
            if(string.IsNullOrWhiteSpace(name)||name.Length>120||name.Any(char.IsControl)) return Results.BadRequest();
            using var buffer=new MemoryStream(); await file.CopyToAsync(buffer,context.RequestAborted);
            var data=buffer.ToArray(); var type=Detect(data,name);
            if(type is null) return Results.Problem(statusCode:400,title:"PNG, JPEG, WebP 이미지만 첨부할 수 있습니다.");
            // SQLite write transaction serializes the quota check with the insert.
            await using var transaction=await db.Database.BeginTransactionAsync();
            access=await Access(kind,parent,db,context);
            if(!access.Visible) return Results.NotFound();
            if(!access.Editable) return Results.Forbid();
            if(await Parent(db,kind,parent).CountAsync()>=3) return Results.Problem(statusCode:409,title:"첨부파일은 글당 최대 3개입니다.");
            var item=new ContentAttachment {PostId=kind=="posts"?parent:null,InquiryId=kind=="inquiries"?parent:null,Name=name,ContentType=type,Data=data,Length=data.Length};
            db.Attachments.Add(item);await db.SaveChangesAsync();await transaction.CommitAsync();return Results.Ok(new {item.Id,item.Name,item.Length});
        }).RequireAuthorization();
        group.MapGet("/{id:guid}",async(Guid id,MemberDatabase db,HttpContext context)=>{
            // Check visibility and select bytes in the same query, including privacy changes.
            var visible=Inquiries.Visible(db,context.User);
            var item=await db.Attachments.AsNoTracking().SingleOrDefaultAsync(x=>x.Id==id &&
                ((x.PostId!=null && db.Posts.Any(p=>p.Id==x.PostId)) || visible.Any(q=>q.Id==x.InquiryId)));
            if(item is null)return Results.NotFound();
            context.Response.Headers.ContentSecurityPolicy="sandbox; default-src 'none'";
            context.Response.Headers.XContentTypeOptions="nosniff";
            return Results.File(item.Data,item.ContentType,item.Name,enableRangeProcessing:false);
        });
        group.MapDelete("/{id:guid}",async(Guid id,MemberDatabase db,HttpContext context)=>{
            var item=await db.Attachments.FindAsync(id);if(item is null)return Results.NotFound();
            var access=await Access(item.PostId.HasValue?"posts":"inquiries",item.PostId??item.InquiryId!.Value,db,context);
            if(!access.Visible)return Results.NotFound();if(!access.Editable)return Results.Forbid();
            db.Attachments.Remove(item);await db.SaveChangesAsync();return Results.NoContent();
        }).RequireAuthorization();
    }
}
