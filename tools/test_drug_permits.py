import io
import json
from pathlib import Path
import tempfile
import unittest
import urllib.error
import urllib.parse
from unittest.mock import patch
import collect_drug_permits as collector
import enrich_drug_permits as enrichment
from configure_portable import configure


class DrugPermitDataChecks(unittest.TestCase):
    def test_full_pages_explicit_otc_and_original_variants(self):
        rows=[{'ITEM_SEQ':str(200000000+i//2),'ITEM_NAME':'fixture','SPCLTY_PBLC':'일반의약품' if i%3 else '전문의약품','ITEM_INGR_NAME':str(i),'CANCEL_NAME':'취하'} for i in range(101)]
        def response(url,**kwargs):
            page=int(urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['pageNo'][0]);start=(page-1)*100
            return io.BytesIO(json.dumps({'header':{'resultCode':'00'},'body':{'totalCount':101,'items':rows[start:start+100]}}).encode())
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'data'
            with patch.object(collector.urllib.request,'urlopen',side_effect=response),patch.object(collector.time,'sleep'):
                result=collector.collect('fixture',output)
            kept=[json.loads(line) for line in (output/'permits.jsonl').read_text(encoding='utf-8').splitlines()]
            self.assertEqual(result['sourceRows'],101);self.assertEqual(result['recordCount'],67)
            self.assertGreater(result['duplicateProductRows'],0)
            self.assertTrue(all(row['SPCLTY_PBLC']=='일반의약품' and row['CANCEL_NAME']=='취하' for row in kept))
            self.assertEqual(result['sha256'],collector.digest(output/'permits.jsonl'))
            with self.assertRaises(FileExistsError):collector.collect('fixture',output)

    def test_incomplete_page_and_forbidden_request_do_not_publish_or_retry(self):
        cases=[io.BytesIO(b'{"header":{"resultCode":"00"},"body":{"totalCount":101,"items":[]}}'),urllib.error.HTTPError('https://official.invalid/?key=fixture-secret',403,'forbidden',None,None)]
        for case in cases:
            with tempfile.TemporaryDirectory() as temporary:
                output=Path(temporary)/'data'
                with patch.object(collector.urllib.request,'urlopen',side_effect=[case]) as fetch,patch.object(collector.time,'sleep'):
                    with self.assertRaises((ValueError,RuntimeError)) as error:collector.collect('fixture',output)
                self.assertEqual(fetch.call_count,1)
                self.assertNotIn('fixture-secret',str(error.exception))
                self.assertFalse((output/'manifest.json').exists());self.assertFalse((output/'permits.jsonl').exists())

    def test_optional_permits_mount_requires_hash_and_safe_metadata(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)/'MedicalInfo';root.mkdir()
            for directory,filename in [('hira-202606','facilities.jsonl'),('mfds-drugs-20261015-v2','drugs.jsonl'),('mfds-supplements-20261015-v2','supplements.jsonl'),('otc-permits','permits.jsonl')]:
                path=root.parent/'data'/directory;path.mkdir(parents=True);(path/filename).write_bytes(b'{}\n')
                (path/'manifest.json').write_text(json.dumps({'sha256':collector.digest(path/filename)}))
            metadata=root/'portable-data.json';metadata.write_text(json.dumps({'permitsDirectory':'otc-permits'}))
            (root.parent/'data/otc-permits/permits.jsonl').write_bytes(b'changed')
            with self.assertRaises(ValueError):configure(root)
            self.assertFalse((root/'.env').exists())
            (root.parent/'data/otc-permits/permits.jsonl').write_bytes(b'{}\n')
            detail=root.parent/'data/otc-permits/permit-details.jsonl';detail.write_bytes(b'{}\n')
            permit_manifest=root.parent/'data/otc-permits/manifest.json'
            permit_manifest.write_text(json.dumps({'sha256':collector.digest(detail),'detailsSha256':collector.digest(detail)}))
            detail.write_bytes(b'changed')
            with self.assertRaises(ValueError):configure(root)
            self.assertFalse((root/'.env').exists());detail.write_bytes(b'{}\n')
            configure(root);self.assertIn('DRUG_PERMIT_DATA_DIR=',(root/'.env').read_text())
            metadata.write_text(json.dumps({'permitsDirectory':'../escape'}))
            with self.assertRaises(ValueError):configure(root)

    def test_detail_enrichment_joins_only_exact_otc_ids(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);source=root/'source';source.mkdir()
            (source/'permits.jsonl').write_text(json.dumps({'ITEM_SEQ':'200000001','SPCLTY_PBLC':'일반의약품'})+'\n')
            (source/'manifest.json').write_text(json.dumps({'status':'complete','sha256':collector.digest(source/'permits.jsonl')}))
            rows=[{'ITEM_SEQ':key,'ITEM_NAME':'same name','ETC_OTC_CODE':kind,'EE_DOC_DATA':'<DOC>full original</DOC>'} for key,kind in [('200000001','일반의약품'),('200000002','전문의약품')]]
            payload=json.dumps({'header':{'resultCode':'00'},'body':{'totalCount':2,'items':rows}}).encode()
            with patch.object(enrichment.urllib.request,'urlopen',return_value=io.BytesIO(payload)),patch.object(enrichment.time,'sleep'):
                result=enrichment.enrich(source,root/'enriched','fixture')
            detail=(root/'enriched/permit-details.jsonl').read_text(encoding='utf-8').splitlines()
            self.assertEqual(len(detail),1);self.assertEqual(json.loads(detail[0])['ITEM_SEQ'],'200000001')
            self.assertEqual(result['missingDetailIds'],[])
            self.assertEqual((source/'permits.jsonl').read_bytes(),(root/'enriched/permits.jsonl').read_bytes())
            rows[0]['ETC_OTC_CODE']='전문의약품'
            with patch.object(enrichment.urllib.request,'urlopen',return_value=io.BytesIO(json.dumps({'header':{'resultCode':'00'},'body':{'totalCount':2,'items':rows}}).encode())),patch.object(enrichment.time,'sleep'):
                with self.assertRaises(ValueError):enrichment.enrich(source,root/'mismatch','fixture')
            self.assertFalse((root/'mismatch/manifest.json').exists())

    def test_enrichment_resumes_only_verified_checkpoint_pages(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);source=root/'source';source.mkdir()
            (source/'permits.jsonl').write_text('\n'.join(json.dumps({'ITEM_SEQ':key,'SPCLTY_PBLC':'일반의약품'}) for key in ['200000001','200000201'])+'\n')
            (source/'manifest.json').write_text(json.dumps({'status':'complete','sha256':collector.digest(source/'permits.jsonl')}))
            fail=[True]
            def response(url,**kwargs):
                page=int(urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['pageNo'][0])
                if page==3 and fail[0]:raise urllib.error.URLError('fixture failure')
                start=(page-1)*100
                rows=[{'ITEM_SEQ':str(200000001+i),'ITEM_NAME':'fixture','ETC_OTC_CODE':'일반의약품'} for i in range(start,min(start+100,201))]
                return io.BytesIO(json.dumps({'header':{'resultCode':'00'},'body':{'totalCount':201,'items':rows}}).encode())
            output=root/'data'
            with patch.object(enrichment.urllib.request,'urlopen',side_effect=response),patch.object(enrichment.time,'sleep'):
                with self.assertRaises(RuntimeError):enrichment.enrich(source,output,'fixture')
                checkpoint=json.loads((output/'progress.json').read_text())
                self.assertEqual(checkpoint['nextPage'],3);self.assertEqual(checkpoint['processedRows'],200)
                self.assertFalse((output/'manifest.json').exists())
                fail[0]=False;partial=output/'permit-details.jsonl.partial';original=partial.read_bytes();partial.write_bytes(original+b'\n')
                with self.assertRaises(ValueError):enrichment.enrich(source,output,'fixture',resume=True)
                partial.write_bytes(original)
                result=enrichment.enrich(source,output,'fixture',resume=True)
            self.assertEqual(result['withDetails'],2);self.assertFalse((output/'progress.json').exists())


if __name__=='__main__':unittest.main()
