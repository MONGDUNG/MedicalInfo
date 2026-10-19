import io
import json
from pathlib import Path
import tempfile
import unittest
import urllib.parse
from unittest.mock import patch
import collect_drugs

def response(items, total):
    return io.BytesIO(json.dumps({'header':{'resultCode':'00'},'body':{'totalCount':total,'items':items}}).encode())

class CollectorChecks(unittest.TestCase):
    def test_prefetched_pages_are_written_in_order(self):
        def page_response(url, **kwargs):
            page = int(urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['pageNo'][0])
            start = (page - 1) * 100
            return response([{'itemSeq':str(i)} for i in range(start, min(start + 100, 201))], 201)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            with patch.object(collect_drugs.urllib.request, 'urlopen', side_effect=page_response):
                self.assertEqual(collect_drugs.collect('fixture', path), 201)
            identifiers = [json.loads(line)['itemSeq'] for line in (path/'drugs.jsonl').read_text(encoding='utf-8').splitlines()]
            self.assertEqual(identifiers, [str(i) for i in range(201)])

    def test_duplicate_official_rows_are_preserved(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            with patch.object(collect_drugs.urllib.request, 'urlopen', return_value=response([{'itemSeq':'1'}, {'itemSeq':'1'}], 2)):
                self.assertEqual(collect_drugs.collect('fixture', path), 2)
            manifest = json.loads((path/'manifest.json').read_text(encoding='utf-8'))
            self.assertEqual(manifest['uniqueProductCount'], 1)
            self.assertEqual(manifest['duplicateProductRows'], 1)

    def test_incomplete_response_never_marks_complete(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            with patch.object(collect_drugs.urllib.request, 'urlopen', return_value=response([], 2)):
                with self.assertRaises(RuntimeError): collect_drugs.collect('fixture', path)
            self.assertFalse((path/'drugs.jsonl').exists())
            self.assertFalse((path/'manifest.json').exists())

    def test_supplement_envelope_preserves_official_fields(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)
            item = {'STTEMNT_NO':'123', 'PRDUCT':'fixture', 'MAIN_FNCTN':'original'}
            with patch.object(collect_drugs.urllib.request, 'urlopen', return_value=response([{'item':item}], 1)):
                self.assertEqual(collect_drugs.collect('fixture', path, True), 1)
            self.assertEqual(json.loads((path/'supplements.jsonl').read_text(encoding='utf-8')), item)

if __name__ == '__main__': unittest.main()
