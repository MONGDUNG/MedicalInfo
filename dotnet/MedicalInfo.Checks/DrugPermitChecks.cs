using System.Text.Json;
using MedicalInfo;

static class DrugPermitChecks
{
    public static void Run(string easyPath, Action<bool,string> check)
    {
        var directory=Path.Combine(Path.GetTempPath(),"medicalinfo-permit-checks-"+Guid.NewGuid());Directory.CreateDirectory(directory);
        var permitPath=Path.Combine(directory,"permits.jsonl");var manifest=Path.Combine(directory,"manifest.json");var detailsPath=Path.Combine(directory,"permit-details.jsonl");
        try {
            File.WriteAllText(permitPath,"""
{"ITEM_SEQ":"1","ITEM_NAME":"허가 품목명","ENTP_NAME":"허가 업체","SPCLTY_PBLC":"일반의약품","ITEM_INGR_NAME":"시험성분","ITEM_PERMIT_DATE":"20000101","CANCEL_NAME":"정상"}
{"ITEM_SEQ":"1","ITEM_NAME":"허가 품목명","ENTP_NAME":"허가 업체","SPCLTY_PBLC":"일반의약품","ITEM_INGR_NAME":"다른성분","ITEM_PERMIT_DATE":"20000101","CANCEL_NAME":"취하"}
{"ITEM_SEQ":"2","ITEM_NAME":"시험약","ENTP_NAME":"별도 업체","SPCLTY_PBLC":"일반의약품","ITEM_INGR_NAME":"독립성분","ITEM_PERMIT_DATE":"20200101","CANCEL_NAME":"취하"}
""");
            var catalog=new DrugCatalog(easyPath,permitsPath:permitPath);
            JsonElement Search(string? keyword=null,string? effect=null,string? ingredient=null)=>JsonSerializer.SerializeToElement(catalog.Search(keyword,1,effect,ingredient:ingredient));
            check(catalog.Available&&catalog.PermitDataAvailable&&Search().GetProperty("total").GetInt32()==2,"OTC permit and easy catalogs join by exact ID without counting variants as products");
            check(catalog.Find("1")!.Length==2&&catalog.FindPermits("1").Length==2,"conflicting easy and permit records remain independently preserved");
            check(catalog.Find("2") is {Length:0}&&catalog.FindPermits("2")[0].GetProperty("ITEM_INGR_NAME").GetString()=="독립성분","same-name product with another ID never inherits easy efficacy");
            check(Search(effect:"현재 정보").GetProperty("total").GetInt32()==1,"permit-only products do not invent efficacy descriptions");
            check(Search(ingredient:"독립성분").GetProperty("items")[0].GetProperty("id").GetString()=="2","ingredient filter uses official permit text");
            check(Search(keyword:"독립성분").GetProperty("total").GetInt32()==1&&Search(effect:"현재 정보",ingredient:"독립성분").GetProperty("total").GetInt32()==0,"ingredient keyword search and efficacy intersection remain product specific");
            var summary=Search(ingredient:"독립성분").GetProperty("items")[0];
            check(summary.GetProperty("dateLabel").GetString()=="허가일"&&summary.GetProperty("permitStatuses")[0].GetString()=="취하","permit date is not relabeled as revision and withdrawn status remains visible");
            var permitsOnly=new DrugCatalog(null,permitsPath:permitPath);
            check(permitsOnly.Available&&permitsOnly.Find("1") is {Length:0}&&permitsOnly.Find("unknown") is null,"permit dataset can serve lookup and comments without easy data");
            try { new DrugCatalog(easyPath).Search(null,1,ingredient:"성분");throw new Exception("Expected missing ingredient data rejection"); }
            catch(SearchInputException){check(true,"missing permit dataset is not treated as zero ingredient matches");}
            var content=File.ReadAllBytes(permitPath);
            File.WriteAllText(manifest,JsonSerializer.Serialize(new{status="complete",sha256=Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(content)),collectedAt="2026-10-19T00:00:00Z"}));
            check(new DrugCatalog(null,permitsPath:permitPath).PermitCollectedAt==DateTimeOffset.Parse("2026-10-19T00:00:00Z"),"verified permit snapshot exposes collection time independently from permit date");
            File.WriteAllText(detailsPath,JsonSerializer.Serialize(new{ITEM_SEQ="2",ITEM_NAME="시험약",ETC_OTC_CODE="일반의약품",MAIN_ITEM_INGR="[M000001]한글시험성분",EE_DOC_DATA="<DOC><SECTION><ARTICLE><PARAGRAPH><![CDATA[공식 시험효능]]></PARAGRAPH></ARTICLE></SECTION></DOC>"}));
            void Metadata()=>File.WriteAllText(manifest,JsonSerializer.Serialize(new{status="complete",sha256=Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(content)),collectedAt="2026-10-19T00:00:00Z",detailsSha256=Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(File.ReadAllBytes(detailsPath))),detailCollectedAt="2026-10-19T01:00:00Z"}));
            Metadata();var enriched=new DrugCatalog(easyPath,permitsPath:permitPath);
            var korean=JsonSerializer.SerializeToElement(enriched.Search(null,1,ingredient:"한글시험성분"));
            check(korean.GetProperty("total").GetInt32()==1&&korean.GetProperty("items")[0].GetProperty("id").GetString()=="2","Korean active ingredient search uses exact permit detail identity");
            check(JsonSerializer.SerializeToElement(enriched.Search(null,1,effect:"공식 시험효능")).GetProperty("items")[0].GetProperty("id").GetString()=="2","permit-only product efficacy filter uses official document text");
            check(enriched.FindPermitDetails("1").Length==0&&enriched.FindPermitDetails("2").Length==1,"permit documents are never shared across same-name products");
            check(DrugCatalog.DocumentText("<!DOCTYPE DOC [<!ENTITY x SYSTEM 'file:///not-read'>]><DOC>&x;</DOC>")==""&&DrugCatalog.DocumentText("<DOC>")=="","DTD and malformed documents do not enter efficacy search");
            File.AppendAllText(detailsPath,"\n");
            try{_=new DrugCatalog(null,permitsPath:permitPath);throw new Exception("Expected detail checksum rejection");}
            catch(InvalidDataException){check(true,"tampered detail snapshot fails startup");}
            foreach(var invalid in new[]{new{ITEM_SEQ="unknown",ITEM_NAME="fixture",ETC_OTC_CODE="일반의약품"},new{ITEM_SEQ="2",ITEM_NAME="fixture",ETC_OTC_CODE="전문의약품"}}){
                File.WriteAllText(detailsPath,JsonSerializer.Serialize(invalid));Metadata();
                try{_=new DrugCatalog(null,permitsPath:permitPath);throw new Exception("Expected invalid OTC detail identity rejection");}
                catch(InvalidDataException){check(true,"unknown or non-OTC detail identity is rejected");}
            }
            File.Delete(detailsPath);
            File.AppendAllText(permitPath,"\n");
            try{_=new DrugCatalog(null,permitsPath:permitPath);throw new Exception("Expected tampered permit data rejection");}
            catch(InvalidDataException){check(true,"permit manifest checksum mismatch fails startup");}
            File.Delete(manifest);
            foreach(var invalid in new[]{"""{"ITEM_SEQ":"1","ITEM_NAME":"전문약","SPCLTY_PBLC":"전문의약품"}""","""{"ITEM_SEQ":"1","SPCLTY_PBLC":"일반의약품"}""","[]"}) {
                File.WriteAllText(permitPath,invalid);
                try{_=new DrugCatalog(null,permitsPath:permitPath);throw new Exception("Expected invalid OTC data rejection");}
                catch(InvalidDataException){check(true,"non-OTC or malformed permit dataset rejected at startup");}
            }
        } finally { File.Delete(permitPath);File.Delete(manifest);File.Delete(detailsPath);Directory.Delete(directory); }
    }
}
