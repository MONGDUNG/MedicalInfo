using System.Security.Claims;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;
public sealed class ProductComment
{
    public long Id {get;set;}
    public string Catalog {get;set;}="";
    public string ProductId {get;set;}="";
    public string Content {get;set;}="";
    public string? AuthorId {get;set;}
    public Member? Author {get;set;}
    public DateTime CreatedAt {get;set;}=DateTime.UtcNow;
    public DateTime? UpdatedAt {get;set;}
}
public sealed class ProductVote
{
    public string ProductId {get;set;}="";
    public string MemberId {get;set;}="";
}
public static class ProductCommunity
{
    public static void MapProductCommunity(this WebApplication app,DrugCatalog drugs,DrugCatalog supplements){
        IResult? Exists(string catalog,string id){
            var source=catalog switch{"drugs"=>drugs,"supplements"=>supplements,_=>null};
            if(source is null||id.Length>100)return Results.NotFound();
            if(!source.Available)return Results.Problem(statusCode:503,title:"공식 제품 데이터를 준비 중입니다.");
            return source.Find(id) is null?Results.NotFound():null;
        }
        var group=Inquiries.Protect(app.MapGroup("/api/products/{catalog}/{productId}"));
        group.MapGet("/comments",async(string catalog,string productId,int? page,MemberDatabase db,HttpContext context)=>{
            var failure=Exists(catalog,productId);if(failure is not null)return failure;
            var current=page??1;if(current is <1 or >100000)return Results.BadRequest();
            var query=db.ProductComments.AsNoTracking().Where(x=>x.Catalog==catalog&&x.ProductId==productId);
            var total=await query.CountAsync();var rows=await query.Include(x=>x.Author).OrderByDescending(x=>x.Id).Skip((current-1)*20).Take(20).ToArrayAsync();
            return Results.Ok(new{total,page=current,pageSize=20,items=rows.Select(x=>new{x.Id,x.Content,author=x.Author?.DisplayName??"탈퇴 회원",x.CreatedAt,x.UpdatedAt,canEdit=Inquiries.Owns(context.User,x.AuthorId)})});
        });
        group.MapPost("/comments",async(string catalog,string productId,CommentInput input,MemberDatabase db,HttpContext context)=>{
            var failure=Exists(catalog,productId);if(failure is not null)return failure;
            if(string.IsNullOrWhiteSpace(input.Content)||input.Content.Length>2000)return Results.BadRequest();
            var item=new ProductComment{Catalog=catalog,ProductId=productId,Content=input.Content,AuthorId=context.User.FindFirstValue(ClaimTypes.NameIdentifier)};
            db.ProductComments.Add(item);await db.SaveChangesAsync();return Results.Ok(new{item.Id});
        }).RequireAuthorization();
        group.MapPut("/comments/{id:long}",async(string catalog,string productId,long id,CommentInput input,MemberDatabase db,HttpContext context)=>{
            var item=await db.ProductComments.SingleOrDefaultAsync(x=>x.Id==id&&x.Catalog==catalog&&x.ProductId==productId);
            if(item is null)return Results.NotFound();if(!Inquiries.Owns(context.User,item.AuthorId))return Results.Forbid();
            if(string.IsNullOrWhiteSpace(input.Content)||input.Content.Length>2000)return Results.BadRequest();
            item.Content=input.Content;item.UpdatedAt=DateTime.UtcNow;await db.SaveChangesAsync();return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/comments/{id:long}",async(string catalog,string productId,long id,MemberDatabase db,HttpContext context)=>{
            var item=await db.ProductComments.SingleOrDefaultAsync(x=>x.Id==id&&x.Catalog==catalog&&x.ProductId==productId);
            if(item is null)return Results.NotFound();if(!Inquiries.Owns(context.User,item.AuthorId))return Results.Forbid();
            db.ProductComments.Remove(item);await db.SaveChangesAsync();return Results.NoContent();
        }).RequireAuthorization();
        group.MapGet("/recommendation",async(string catalog,string productId,MemberDatabase db,HttpContext context)=>{
            if(catalog!="supplements")return Results.NotFound();var failure=Exists(catalog,productId);if(failure is not null)return failure;
            var member=context.User.FindFirstValue(ClaimTypes.NameIdentifier);var query=db.ProductVotes.Where(x=>x.ProductId==productId);
            return Results.Ok(new{total=await query.CountAsync(),recommended=member is not null&&await query.AnyAsync(x=>x.MemberId==member)});
        });
        group.MapPut("/recommendation",async(string catalog,string productId,MemberDatabase db,HttpContext context)=>{
            if(catalog!="supplements")return Results.NotFound();var failure=Exists(catalog,productId);if(failure is not null)return failure;
            var member=context.User.FindFirstValue(ClaimTypes.NameIdentifier)!;
            await using var transaction=await db.Database.BeginTransactionAsync();
            if(!await db.ProductVotes.AnyAsync(x=>x.ProductId==productId&&x.MemberId==member)){
                db.ProductVotes.Add(new ProductVote{ProductId=productId,MemberId=member});await db.SaveChangesAsync();
            }
            await transaction.CommitAsync();return Results.NoContent();
        }).RequireAuthorization();
        group.MapDelete("/recommendation",async(string catalog,string productId,MemberDatabase db,HttpContext context)=>{
            if(catalog!="supplements")return Results.NotFound();
            var member=context.User.FindFirstValue(ClaimTypes.NameIdentifier);
            await db.ProductVotes.Where(x=>x.ProductId==productId&&x.MemberId==member).ExecuteDeleteAsync();return Results.NoContent();
        }).RequireAuthorization();
    }
}
