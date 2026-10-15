using MedicalInfo;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.FileProviders;
using Microsoft.Extensions.Logging.Abstractions;

static class MailConfigurationChecks
{
    public static void Run(Action<bool,string> check){
        var environment=new MailEnvironment();
        AccountMailer Create(Dictionary<string,string?> settings)=>new(new ConfigurationBuilder().AddInMemoryCollection(settings).Build(),environment,NullLogger<AccountMailer>.Instance);
        void Rejected(Dictionary<string,string?> settings,string label){try{using var mail=Create(settings);throw new Exception("Expected mail configuration rejection");}catch(InvalidOperationException){check(true,label);}}
        using(var disabled=Create(new()))check(!disabled.Enabled,"email delivery disabled unless explicitly configured");
        Rejected(new(){["Mail:RequireConfirmedEmail"]="true"},"cannot require email confirmation without a delivery service");
        var settings=new Dictionary<string,string?>{["Mail:Mode"]="Pickup",["Mail:From"]="test@example.invalid",["Mail:PublicUrl"]="https://example.invalid"};
        Rejected(settings,"production cannot write recovery messages to development pickup");
        settings["Mail:Mode"]="Smtp";settings["Mail:Host"]="smtp.example.invalid";settings["Mail:PublicUrl"]="http://example.invalid";
        Rejected(settings,"production recovery origin must use HTTPS");
        settings["Mail:PublicUrl"]="https://example.invalid/redirect?next=evil";Rejected(settings,"mail origin cannot contain a redirect path or query");
        settings["Mail:PublicUrl"]="https://user:secret@example.invalid";Rejected(settings,"mail origin cannot embed credentials");
        settings["Mail:PublicUrl"]="https://example.invalid";using(var configured=Create(settings))check(configured.Enabled,"SMTP configuration accepts fixed public HTTPS origin");
    }
    private sealed class MailEnvironment:IWebHostEnvironment
    {
        public string ApplicationName{get;set;}="MedicalInfo.Checks";
        public string EnvironmentName{get;set;}="Production";
        public string ContentRootPath{get;set;}=Path.GetTempPath();
        public IFileProvider ContentRootFileProvider{get;set;}=new NullFileProvider();
        public string WebRootPath{get;set;}=Path.Combine(Path.GetTempPath(),"wwwroot");
        public IFileProvider WebRootFileProvider{get;set;}=new NullFileProvider();
    }
}
