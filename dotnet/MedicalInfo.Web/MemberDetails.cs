using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.DataProtection;

namespace MedicalInfo;

public record MemberDetailsInput(string? FullName, string? Address, string? DetailAddress, string? Postcode,
    DateOnly? BirthDate, string? Sex, double? Latitude, double? Longitude, string? UnderlyingCondition, bool HealthConsent);
public record MemberDetailsData(string? FullName, string? Address, string? DetailAddress, string? Postcode,
    DateOnly? BirthDate, string? Sex, double? Latitude, double? Longitude, string? UnderlyingCondition, DateTime? HealthConsentAt);

public sealed class MemberDetailsStore(IDataProtectionProvider provider)
{
    private readonly IDataProtector protector = provider.CreateProtector("MedicalInfo.MemberDetails.v1");
    public MemberDetailsData Read(Member member) => member.ProtectedDetails is null ?
        new(null,null,null,null,null,null,null,null,null,null) :
        JsonSerializer.Deserialize<MemberDetailsData>(protector.Unprotect(member.ProtectedDetails)) ?? throw new CryptographicException();
    public string Protect(MemberDetailsData data) => protector.Protect(JsonSerializer.Serialize(data));
    public static bool Valid(MemberDetailsInput input) =>
        (input.FullName?.Length ?? 0) <= 100 && (input.Address?.Length ?? 0) <= 300 &&
        (input.DetailAddress?.Length ?? 0) <= 200 && (input.UnderlyingCondition?.Length ?? 0) <= 2000 &&
        (string.IsNullOrEmpty(input.Postcode) || System.Text.RegularExpressions.Regex.IsMatch(input.Postcode, "^[0-9]{5}$")) &&
        (input.BirthDate is null || input.BirthDate >= new DateOnly(1900,1,1) && input.BirthDate <= DateOnly.FromDateTime(DateTime.UtcNow)) &&
        input.Sex is null or "" or "female" or "male" or "other" &&
        (input.Latitude is null && input.Longitude is null ||
            input.Latitude is >= -90 and <= 90 && input.Longitude is >= -180 and <= 180) &&
        (string.IsNullOrWhiteSpace(input.UnderlyingCondition) || input.HealthConsent);
    public static string? Clean(string? value) => string.IsNullOrWhiteSpace(value) ? null : value.Trim();
}

public static class MemberDetails
{
    public static bool Inactive(Member member, DateTime now) => (member.LastLoginAt ?? member.CreatedAt) < now.AddMonths(-6);
    public static void MapMemberDetails(this RouteGroupBuilder group)
    {
        group.MapGet("/details", async (HttpContext context, UserManager<Member> users, MemberDetailsStore details) => {
            var user = await users.GetUserAsync(context.User);
            if (user is null) return Results.Unauthorized();
            try { return Results.Ok(details.Read(user)); }
            catch (Exception ex) when (ex is CryptographicException or JsonException) {
                return Results.Problem(statusCode:503,title:"개인정보를 읽을 수 없습니다. 운영자에게 인증 키 복원을 요청해주세요.");
            }
        }).RequireAuthorization();
        group.MapPut("/details", async (MemberDetailsInput input, HttpContext context, UserManager<Member> users, MemberDetailsStore details) => {
            if (!MemberDetailsStore.Valid(input)) return Results.Problem(statusCode:400,title:"입력 길이·생년월일·우편번호·좌표와 건강정보 저장 동의를 확인해주세요.");
            var user = await users.GetUserAsync(context.User);
            if (user is null) return Results.Unauthorized();
            MemberDetailsData previous;
            try { previous = details.Read(user); }
            catch (Exception ex) when (ex is CryptographicException or JsonException) { return Results.Problem(statusCode:503,title:"기존 개인정보를 읽을 수 없어 저장하지 않았습니다. 인증 키 복원이 필요합니다."); }
            var health = MemberDetailsStore.Clean(input.UnderlyingCondition);
            var data = new MemberDetailsData(MemberDetailsStore.Clean(input.FullName), MemberDetailsStore.Clean(input.Address),
                MemberDetailsStore.Clean(input.DetailAddress), MemberDetailsStore.Clean(input.Postcode), input.BirthDate,
                MemberDetailsStore.Clean(input.Sex), input.Latitude, input.Longitude, health,
                health is null ? null : previous.HealthConsentAt ?? DateTime.UtcNow);
            user.ProtectedDetails = details.Protect(data);
            return (await users.UpdateAsync(user)).Succeeded ? Results.Ok(data) : Results.Conflict();
        }).RequireAuthorization();
    }
}
