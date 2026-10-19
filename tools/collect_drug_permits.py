"""Collect official MFDS permit records and retain explicitly classified OTC products."""
import argparse
import collections
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import hashlib
import json
from pathlib import Path
import re
import time
import urllib.error
import urllib.parse
import urllib.request

ENDPOINT = 'https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService08/getDrugPrdtPrmsnInq08'
SOURCE = 'https://www.data.go.kr/data/15095677/openapi.do'


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def decode(payload):
    envelope = json.loads(payload)
    envelope = envelope.get('response', envelope)
    if str(envelope.get('header', {}).get('resultCode')) not in ('00', '0'):
        raise ValueError('Official API rejected request')
    body = envelope['body']
    items = body.get('items') or []
    if isinstance(items, dict):
        items = items.get('item') or []
    if isinstance(items, dict):
        items = [items]
    if not isinstance(items, list) or any(not isinstance(row, dict) for row in items):
        raise ValueError('Invalid permit records')
    return int(body['totalCount']), items


def collect(key, output):
    output.mkdir(parents=True, exist_ok=False)
    def fetch(page):
        query = urllib.parse.urlencode({'serviceKey': urllib.parse.unquote(key.strip()), 'pageNo': page, 'numOfRows': 100, 'type': 'json'})
        for attempt in range(3):
            try:
                with urllib.request.urlopen(ENDPOINT + '?' + query, timeout=45) as response:
                    payload = response.read(5_000_001)
                if len(payload) > 5_000_000:
                    raise ValueError('Oversized API response')
                return decode(payload)
            except urllib.error.HTTPError as error:
                if error.code not in (429, 500, 502, 503, 504) or attempt == 2:
                    raise RuntimeError(f'Official permit API HTTP {error.code} on page {page}; check service approval/network. Credentials suppressed.') from None
            except (urllib.error.URLError, TimeoutError):
                if attempt == 2:
                    raise RuntimeError(f'Official permit API network failure on page {page}; incomplete output retained.') from None
            except Exception:
                raise RuntimeError(f'Official permit response rejected on page {page}; incomplete output retained.') from None
            time.sleep(1 + attempt)
    expected, first = fetch(1)
    if expected < 1:
        raise ValueError('Empty permit directory')
    count = kept = duplicates = 0
    ids = set()
    classifications = collections.Counter()
    with (output/'official.jsonl.partial').open('w', encoding='utf-8', newline='\n') as raw, (output/'permits.jsonl.partial').open('w', encoding='utf-8', newline='\n') as target, ThreadPoolExecutor(max_workers=2) as executor:
        pages = (expected + 99) // 100
        pending = {}
        for page in range(1, pages + 1):
            total, items = (expected, first) if page == 1 else pending.pop(page).result()
            if total != expected or len(items) != min(100, expected - count):
                raise ValueError('Permit count/page changed; use a fresh output directory')
            for upcoming in range(page + 1, min(page + 2, pages) + 1):
                if upcoming not in pending:
                    pending[upcoming] = executor.submit(fetch, upcoming)
            for row in items:
                identifier = row.get('ITEM_SEQ')
                classification = row.get('SPCLTY_PBLC')
                if not isinstance(identifier, str) or not re.fullmatch(r'\d{9}', identifier) or not isinstance(row.get('ITEM_NAME'), str) or not row['ITEM_NAME'].strip() or not isinstance(classification, str) or not classification.strip():
                    raise ValueError('Missing or malformed official permit identity/classification')
                line = json.dumps(row, ensure_ascii=False) + '\n'
                raw.write(line)
                count += 1
                classifications[classification] += 1
                if classification not in ('일반의약품', '일반'):
                    continue
                duplicates += identifier in ids
                ids.add(identifier)
                target.write(line)
                kept += 1
            if page % 25 == 0 or page == pages:
                print(f'Permit records: {count}/{expected}; OTC rows: {kept}', flush=True)
            time.sleep(0.3)
    if count != expected or not kept:
        raise ValueError('Incomplete or empty OTC permit data')
    for name in ('official.jsonl', 'permits.jsonl'):
        (output/(name+'.partial')).rename(output/name)
    manifest = {'source': SOURCE, 'endpoint': ENDPOINT, 'provider': '식품의약품안전처',
                'license': '이용허락범위 제한 없음', 'collectedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
                'status': 'complete', 'sourceRows': count, 'recordCount': kept, 'uniqueProductCount': len(ids),
                'duplicateProductRows': duplicates, 'classifications': dict(classifications),
                'sha256': digest(output/'permits.jsonl'), 'rawSha256': digest(output/'official.jsonl'),
                'note': 'Only explicit OTC classifications are served. Cancelled/withdrawn records remain with original status. Permit date is not a revision date; this is not current stock or supply information.'}
    (output/'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--key-file', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    try:
        result = collect(args.key_file.read_text(encoding='utf-8-sig'), args.output)
        print(json.dumps({'recordCount': result['recordCount'], 'uniqueProductCount': result['uniqueProductCount']}))
    except RuntimeError as error:
        print(str(error)); raise SystemExit(1)
    except Exception:
        print('Permit collection failed; check approval, schema and fresh output directory. Credentials/response details suppressed.')
        raise SystemExit(1)
