using System.Net;
using System.Net.Mail;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Channels;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace MedicalInfo;

public sealed class AccountLink
{
    public string Hash { get; set; } = "";
    public string MemberId { get; set; } = "";
    public string Purpose { get; set; } = "";
    public string Stamp { get; set; } = "";
    public string Email { get; set; } = "";
    public DateTime IssuedAt { get; set; }
    public DateTime ExpiresAt { get; set; }
}
public record RecoveryRequest(string? Email);
public record RedeemLink(string? Token, string? Purpose, string? NewPassword);
public record AccountMail(string To, string Subject, string Body);

public sealed class AccountMailer : BackgroundService
{
    private readonly IConfiguration configuration;
    private readonly ILogger<AccountMailer> logger;
    private readonly Channel<AccountMail> queue = Channel.CreateBounded<AccountMail>(100);
    private readonly string? pickup;
    public string PublicUrl { get; }
    public bool Enabled { get; }
    public AccountMailer(IConfiguration configuration,IWebHostEnvironment environment,ILogger<AccountMailer> logger) {
        this.configuration=configuration;this.logger=logger;
        PublicUrl=configuration["Mail:PublicUrl"]?.TrimEnd('/')??"";
        var mode=configuration["Mail:Mode"]??"Disabled";
        if(mode=="Disabled"){
            if(configuration.GetValue<bool>("Mail:RequireConfirmedEmail"))throw new InvalidOperationException("Email confirmation cannot be required while mail delivery is disabled.");
            return;
        }
        if(mode is not ("Smtp" or "Pickup"))throw new InvalidOperationException("Mail:Mode must be Disabled, Smtp or Pickup.");
        if(!Uri.TryCreate(PublicUrl,UriKind.Absolute,out var url)||!string.IsNullOrEmpty(url.UserInfo)||!string.IsNullOrEmpty(url.Query)||!string.IsNullOrEmpty(url.Fragment)||url.AbsolutePath!="/" ||
            (url.Scheme!="https" && !(environment.IsDevelopment()&&url.Scheme=="http"&&url.IsLoopback)))
            throw new InvalidOperationException("Mail:PublicUrl must be a fixed HTTPS origin (loopback HTTP is allowed only in Development).");
        if(!MailAddress.TryCreate(configuration["Mail:From"],out _))throw new InvalidOperationException("Mail:From must be configured.");
        if(mode=="Pickup"){
            if(!environment.IsDevelopment())throw new InvalidOperationException("Mail pickup is Development-only.");
            var state=Path.GetFullPath(configuration["State:Path"]??Path.Combine(environment.ContentRootPath,"App_Data"));
            pickup=Path.Combine(state,"mail-pickup");Directory.CreateDirectory(pickup);
            var webRoot=Path.GetFullPath(environment.WebRootPath??Path.Combine(environment.ContentRootPath,"wwwroot"));
            if(pickup.StartsWith(webRoot+Path.DirectorySeparatorChar,StringComparison.OrdinalIgnoreCase))throw new InvalidOperationException("Mail pickup must not be under the web root.");
        } else if(string.IsNullOrWhiteSpace(configuration["Mail:Host"]) || configuration.GetValue("Mail:Port",587) is <1 or >65535)
            throw new InvalidOperationException("SMTP host and port must be configured.");
        Enabled=true;
    }
    public bool Enqueue(AccountMail mail)=>queue.Writer.TryWrite(mail);
    protected override async Task ExecuteAsync(CancellationToken stoppingToken) {
        await foreach(var mail in queue.Reader.ReadAllAsync(stoppingToken)){
            try {
                using var message=new MailMessage(configuration["Mail:From"]!,mail.To,mail.Subject,mail.Body){BodyEncoding=Encoding.UTF8,SubjectEncoding=Encoding.UTF8};
                using var smtp=new SmtpClient();
                if(pickup is not null){smtp.DeliveryMethod=SmtpDeliveryMethod.SpecifiedPickupDirectory;smtp.PickupDirectoryLocation=pickup;}
                else {
                    smtp.Host=configuration["Mail:Host"]!;smtp.Port=configuration.GetValue("Mail:Port",587);smtp.EnableSsl=true;smtp.UseDefaultCredentials=false;
                    if(!string.IsNullOrEmpty(configuration["Mail:Username"]))smtp.Credentials=new NetworkCredential(configuration["Mail:Username"],configuration["Mail:Password"]);
                }
                using var timeout=CancellationTokenSource.CreateLinkedTokenSource(stoppingToken);timeout.CancelAfter(TimeSpan.FromSeconds(15));
                await smtp.SendMailAsync(message,timeout.Token);
            } catch(Exception ex) when(ex is SmtpException or IOException or UnauthorizedAccessException or OperationCanceledException or InvalidOperationException){
                // Do not log message bodies, links, credentials, recipient addresses or provider errors.
                logger.LogWarning("Account email delivery failed ({FailureType}). Request a new link after checking mail configuration.",ex.GetType().Name);
            }
        }
    }
}

public static class AccountRecovery
{
    public static void AddAccountRecovery(this WebApplicationBuilder builder){
        builder.Services.AddSingleton<AccountMailer>();builder.Services.AddHostedService(services=>services.GetRequiredService<AccountMailer>());
    }
    private static IResult Accepted()=>Results.Ok(new {message="일치하는 계정이 있고 이메일 인증 등 발송 조건을 충족하면 안내 메일을 보냅니다. 잠시 후 메일함을 확인해주세요."});
    private static IResult Invalid()=>Results.Problem(statusCode:400,title:"링크가 만료되었거나 이미 사용되었습니다. 새 링크를 요청해주세요.");
    private static string Hash(string token)=>Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(token)));
    private static bool EmailValid(string? email)=>email?.Length is >0 and <=254 && MailAddress.TryCreate(email,out var address)&&address.Address==email;
    private static async Task Issue(Member user,string purpose,MemberDatabase db,AccountMailer mail){
        var now=DateTime.UtcNow;
        await using var transaction=await db.Database.BeginTransactionAsync();
        await db.AccountLinks.Where(x=>x.ExpiresAt<=now).ExecuteDeleteAsync();
        if(await db.AccountLinks.AnyAsync(x=>x.MemberId==user.Id && x.Purpose==purpose && x.IssuedAt>now.AddMinutes(-1)))return;
        await db.AccountLinks.Where(x=>x.MemberId==user.Id && x.Purpose==purpose).ExecuteDeleteAsync();
        var token=Convert.ToHexString(RandomNumberGenerator.GetBytes(32));
        db.AccountLinks.Add(new AccountLink{Hash=Hash(token),MemberId=user.Id,Purpose=purpose,Stamp=user.SecurityStamp!,Email=user.Email!,IssuedAt=now,ExpiresAt=now.AddMinutes(30)});
        await db.SaveChangesAsync();
        var label=purpose switch{"confirm"=>"이메일 인증","username"=>"아이디 찾기",_=>"비밀번호 재설정"};
        // Fragment never reaches HTTP access logs; the page submits it only after an explicit action.
        var link=$"{mail.PublicUrl}/recover.html#purpose={purpose}&token={token}";
        if(!mail.Enqueue(new AccountMail(user.Email!,"Dr.Geo · "+label,$"{label} 요청 안내입니다.\n30분 이내에 아래 링크를 열고 확인 버튼을 눌러주세요.\n{link}\n본인이 요청하지 않았다면 무시해주세요.")))return;
        await transaction.CommitAsync();
    }
    public static void MapAccountRecovery(this WebApplication app){
        var group=app.MapGroup("/api/recovery").RequireRateLimiting("recovery");
        group.AddEndpointFilter(async(context,next)=>{
            context.HttpContext.Response.Headers.CacheControl="no-store";
            context.HttpContext.Response.Headers["Referrer-Policy"]="no-referrer";
            if(!HttpMethods.IsGet(context.HttpContext.Request.Method)){
                try{await context.HttpContext.RequestServices.GetRequiredService<IAntiforgery>().ValidateRequestAsync(context.HttpContext);}
                catch(AntiforgeryValidationException){return Results.Problem(statusCode:400,title:"보안 확인이 만료되었습니다. 새로고침해주세요.");}
            }
            return await next(context);
        });
        group.MapGet("/status",(AccountMailer mail)=>Results.Ok(new{enabled=mail.Enabled}));
        group.MapPost("/confirm-request",async(HttpContext context,UserManager<Member> users,MemberDatabase db,AccountMailer mail)=>{
            if(!mail.Enabled)return Results.Problem(statusCode:503,title:"메일 발송 설정을 준비 중입니다.");
            var user=await users.GetUserAsync(context.User);
            if(user is not null&&!user.EmailConfirmed)await Issue(user,"confirm",db,mail);
            return Accepted();
        }).RequireAuthorization();
        // This endpoint also works before login when RequireConfirmedEmail is enabled.
        group.MapPost("/request/{purpose}",async(string purpose,RecoveryRequest input,MemberDatabase db,UserManager<Member> users,AccountMailer mail)=>{
            if(purpose is not ("confirm" or "reset" or "username") || !EmailValid(input.Email))return Results.BadRequest();
            if(!mail.Enabled)return Results.Problem(statusCode:503,title:"메일 발송 설정을 준비 중입니다.");
            var user=await users.FindByEmailAsync(input.Email!);
            if(user is not null && !(user.SuspendedUntil>DateTime.UtcNow)){
                if(purpose=="confirm" ? !user.EmailConfirmed : user.EmailConfirmed)await Issue(user,purpose,db,mail);
            }
            return Accepted();
        });
        group.MapPost("/redeem",async(RedeemLink input,MemberDatabase db,UserManager<Member> users)=>{
            if(input.Token?.Length!=64 || !input.Token.All(Uri.IsHexDigit)||input.Purpose is not ("confirm" or "reset" or "username"))return Invalid();
            if(input.Purpose=="reset" && input.NewPassword?.Length is not (>=12 and <=128))return Results.Problem(statusCode:400,title:"새 비밀번호는 12~128자로 입력해주세요.");
            await using var transaction=await db.Database.BeginTransactionAsync();
            var hash=Hash(input.Token);var link=await db.AccountLinks.SingleOrDefaultAsync(x=>x.Hash==hash);
            if(link is null || link.Purpose!=input.Purpose || link.ExpiresAt<=DateTime.UtcNow)return Invalid();
            var user=await users.FindByIdAsync(link.MemberId);
            if(user is null||user.SecurityStamp!=link.Stamp||user.Email!=link.Email||user.SuspendedUntil>DateTime.UtcNow)return Invalid();
            if(link.Purpose=="confirm"){
                if(user.EmailConfirmed)return Invalid();
                var code=await users.GenerateEmailConfirmationTokenAsync(user);
                if(!(await users.ConfirmEmailAsync(user,code)).Succeeded || !(await users.UpdateSecurityStampAsync(user)).Succeeded)return Invalid();
            } else {
                if(!user.EmailConfirmed)return Invalid();
                if(link.Purpose=="reset"){
                    var code=await users.GeneratePasswordResetTokenAsync(user);
                    if(!(await users.ResetPasswordAsync(user,code,input.NewPassword!)).Succeeded)return Invalid();
                    if(!(await users.ResetAccessFailedCountAsync(user)).Succeeded || !(await users.SetLockoutEndDateAsync(user,null)).Succeeded)return Invalid();
                }
            }
            db.AccountLinks.Remove(link);await db.SaveChangesAsync();await transaction.CommitAsync();
            return Results.Ok(new{message=link.Purpose=="username"?"아이디를 확인했습니다.":"처리했습니다. 다시 로그인해주세요.",username=link.Purpose=="username"?user.UserName:null});
        });
    }
}
