using MedicalInfo;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

static class UpgradeChecks
{
    public static async Task Run(Action<bool,string> check) {
        var directory=Path.Combine(Path.GetTempPath(),"medicalinfo-upgrade-"+Guid.NewGuid());Directory.CreateDirectory(directory);
        await using var db=new MemberDatabase(new DbContextOptionsBuilder<MemberDatabase>().UseSqlite("Data Source="+Path.Combine(directory,"members.db")+";Foreign Keys=True").Options);
        // Populate the schema shipped before administrator support, then upgrade it in place.
        await db.GetService<IMigrator>().MigrateAsync("20261015071535_InquiriesAndAttachments");
        await db.Database.ExecuteSqlRawAsync("""
            INSERT INTO AspNetUsers (Id,UserName,NormalizedUserName,DisplayName,CreatedAt,EmailConfirmed,PhoneNumberConfirmed,TwoFactorEnabled,LockoutEnabled,AccessFailedCount)
            VALUES ('upgrade-user','upgrade.user','UPGRADE.USER','Existing member','2026-01-01',0,0,0,1,0);
            INSERT INTO Posts (Id,Title,Content,Category,AuthorId,CreatedAt) VALUES (1,'Existing post','Existing body','자유','upgrade-user','2026-01-01');
            INSERT INTO Inquiries (Id,Title,Content,IsPrivate,AuthorId,CreatedAt) VALUES (1,'Existing private inquiry','Existing private body',1,'upgrade-user','2026-01-01');
            INSERT INTO Attachments (Id,PostId,InquiryId,Name,ContentType,Length,Data) VALUES ('11111111-1111-1111-1111-111111111111',NULL,1,'tiny.png','image/png',3,X'010203');
            """);
        await db.Database.MigrateAsync();
        var member=await db.Users.SingleAsync();
        check(member.Id=="upgrade-user" && member.TierId is null && member.SuspendedUntil is null,"administrator migration preserves existing members with safe defaults");
        check(await db.Posts.AnyAsync(x=>x.AuthorId==member.Id) && await db.Inquiries.AnyAsync(x=>x.AuthorId==member.Id && x.IsPrivate) && (await db.Attachments.SingleAsync()).Data.SequenceEqual(new byte[]{1,2,3}),"schema upgrade preserves posts, private inquiries, attachments and foreign keys");
        db.Users.Remove(member);await db.SaveChangesAsync();
        check((await db.Posts.SingleAsync()).AuthorId is null && !await db.Inquiries.AnyAsync() && !await db.Attachments.AnyAsync(),"foreign-key cascades still work after SQLite table rebuild");
    }
}
