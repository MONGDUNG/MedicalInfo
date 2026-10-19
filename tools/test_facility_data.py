import csv
import io
import json
from pathlib import Path
import tempfile
import unittest
import urllib.parse
from unittest.mock import patch
import collect_facilities
from configure_portable import configure
from enrich_emergency import apply_details
from merge_facilities import merge, digest
from normalize_stores import normalize


class FacilityDataChecks(unittest.TestCase):
    def test_active_stores_projection_and_missing_coordinates(self):
        rows = [dict(zip(['관리번호','개방자치단체코드','영업상태코드','사업장명','좌표정보(X)','좌표정보(Y)'],
                         [key,'3220000',status,'테스트 판매점',x,y])) for key,status,x,y in
                [('one','01','204812.290765599','444824.658097837'),('closed','03','204812','444824'),('missing','01','',''),('zero','01','0','0')]]
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);source=root/'stores.csv'
            with source.open('w',encoding='cp949',newline='') as file:
                writer=csv.DictWriter(file,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
            result=normalize(source,root/'normalized','2026-10-19')
            items=[json.loads(line) for line in (root/'normalized/facilities.jsonl').read_text(encoding='utf-8').splitlines()]
            self.assertEqual(result['counts']['records'],3)
            self.assertEqual(result['counts']['excludedInactive'],1)
            self.assertEqual(result['counts']['missingOrInvalidCoordinates'],2)
            self.assertTrue(37.49<items[0]['LAT']<37.52 and 127.04<items[0]['LNG']<127.08)
            self.assertNotIn('geoLocation',items[1]);self.assertNotIn('geoLocation',items[2])
            self.assertEqual(items[0]['ID'],'mois-store:3220000:one')

    def test_collector_partial_pages_resume_and_ids(self):
        def response(url,**kwargs):
            page=int(urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['pageNo'][0])
            if page==3 and fail[0]:raise OSError('fixture failure')
            start=(page-1)*100
            items=[{'hpid':str(i),'dutyName':'Fixture '+str(i),'wgs84Lat':'37.5','wgs84Lon':'127'} for i in range(start,min(start+100,201))]
            return io.BytesIO(json.dumps({'response':{'header':{'resultCode':'00'},'body':{'totalCount':201,'items':{'item':items}}}}).encode())
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'collected';fail=[True]
            with patch.object(collect_facilities.urllib.request,'urlopen',side_effect=response),patch.object(collect_facilities.time,'sleep'):
                with self.assertRaises(RuntimeError):collect_facilities.collect('emergency','fixture',output,'2026-10-19')
                self.assertFalse((output/'manifest.json').exists());self.assertFalse((output/'facilities.jsonl').exists())
                fail[0]=False
                result=collect_facilities.collect('emergency','fixture',output,'2026-10-19',resume=True)
            self.assertEqual(result['recordCount'],201)
            ids=[json.loads(line)['ID'] for line in (output/'facilities.jsonl').read_text(encoding='utf-8').splitlines()]
            self.assertEqual(ids,['nmc-emergency:'+str(i) for i in range(201)])

    def test_api_xml_json_errors_and_identity(self):
        self.assertEqual(collect_facilities.decode(b'<response><header><resultCode>00</resultCode></header><body><totalCount>1</totalCount><items><item><hpid>A1</hpid></item></items></body></response>'),(1,[{'hpid':'A1'}]))
        with self.assertRaises(ValueError):collect_facilities.decode(b'<!DOCTYPE response><response/>')
        with self.assertRaises(ValueError):collect_facilities.decode(b'{"response":{"header":{"resultCode":"20"}}}')
        item={'ID':'nmc-emergency:A1','PHONE':'123','DEPT_NAME':[]}
        result=apply_details(item,{'hpid':'A1','dgidIdName':'내과, 외과','dutyTime1s':'0900','dutyTime1c':'1800','hvec':'10','dutyEryn':'1'})
        self.assertEqual(result['DEPT_NAME'],['내과','외과'])
        self.assertEqual(result['RAW_DETAIL'],{'진료시작시간_월요일':'0900','진료종료시간_월요일':'1800'})
        with self.assertRaises(ValueError):apply_details(item,{'hpid':'A2'})

    def test_merge_preserves_ids_and_rejects_corruption_or_duplicates(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);sources=[]
            for name,key in [('hira','existing-review-id'),('nmc','nmc-emergency:A1')]:
                path=root/name;path.mkdir();(path/'facilities.jsonl').write_text(json.dumps({'ID':key,'CATEGORY_NAME':name})+'\n',encoding='utf-8')
                (path/'manifest.json').write_text(json.dumps({'sha256':digest(path/'facilities.jsonl')}),encoding='utf-8');sources.append(path)
            result=merge(sources,root/'merged');self.assertEqual(result['recordCount'],2)
            self.assertIn('existing-review-id',(root/'merged/facilities.jsonl').read_text())
            with self.assertRaises(ValueError):merge([sources[0],sources[0]],root/'duplicate')
            self.assertFalse((root/'duplicate/facilities.jsonl').exists())
            (sources[0]/'facilities.jsonl').write_text('tampered')
            with self.assertRaises(ValueError):merge(sources,root/'corrupt')

    def test_portable_combined_configuration_and_secret_preservation(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)/'MedicalInfo';root.mkdir()
            for directory,name in [('facilities-20261019','facilities.jsonl'),('mfds-drugs-20261015-v2','drugs.jsonl'),('mfds-supplements-20261015-v2','supplements.jsonl')]:
                path=root.parent/'data'/directory;path.mkdir(parents=True);(path/name).write_text('{}\n')
                (path/'manifest.json').write_text(json.dumps({'sha256':digest(path/name)}))
            (root/'portable-data.json').write_text(json.dumps({'facilitiesDirectory':'facilities-20261019','elasticsearchIndex':'item-facilities-20261019'}))
            configure(root);self.assertIn('ELASTICSEARCH_INDEX=item-facilities-20261019',(root/'.env').read_text())
            (root/'.env').write_text('PRIVATE=keep-existing\n')
            with self.assertRaises(FileExistsError):configure(root)
            self.assertEqual((root/'.env').read_text(),'PRIVATE=keep-existing\n')
            (root/'portable-data.json').write_text(json.dumps({'facilitiesDirectory':'../escape'}))
            with self.assertRaises(ValueError):configure(root)


if __name__=='__main__':unittest.main()
