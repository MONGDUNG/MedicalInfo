"""Collect approved official emergency/OTC-store APIs without logging credentials."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import hashlib
import json
import math
from pathlib import Path
import time
import urllib.parse
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET
from normalize_stores import normalize, digest

ENDPOINTS = {
    'emergency': 'https://apis.data.go.kr/B552657/ErmctInfoInqireService/getEgytListInfoInqire',
    'stores': 'https://apis.data.go.kr/1741000/over_the_counter_medicine_stores/info',
}


def decode(payload):
    if payload.lstrip().startswith(b'<'):
        if b'<!DOCTYPE' in payload.upper():
            raise ValueError('DTD not accepted')
        root = ET.fromstring(payload)
        if root.findtext('./header/resultCode') not in ('0', '00'):
            raise ValueError('API request rejected')
        return int(root.findtext('./body/totalCount')), [{e.tag: e.text or '' for e in row} for row in root.findall('./body/items/item')]
    result = json.loads(payload)
    envelope = result.get('response', result)
    if str(envelope.get('header', {}).get('resultCode')) not in ('0', '00'):
        raise ValueError('API request rejected')
    body = envelope['body']
    items = body.get('items') or []
    if isinstance(items, dict):
        items = items.get('item') or []
    if isinstance(items, dict):
        items = [items]
    if not isinstance(items, list):
        raise ValueError('Invalid item collection')
    return int(body['totalCount']), items


def emergency_item(row, collected_on):
    key = str(row.get('hpid') or '').strip()
    if not key or not row.get('dutyName'):
        raise ValueError('Missing emergency institution identifier/name')
    item = {'ID': 'nmc-emergency:' + key, 'NAME': row['dutyName'], 'ADDRESS': row.get('dutyAddr') or '',
            'PHONE': row.get('dutyTel3') or row.get('dutyTel1') or '', 'CATEGORY_NAME': '응급의료기관',
            'DEPT_NAME': [], 'SOURCE': 'NMC', 'SOURCE_PERIOD': collected_on, 'RAW_BASIC': row, 'RAW_DETAIL': None}
    try:
        lat, lng = float(row.get('wgs84Lat') or ''), float(row.get('wgs84Lon') or '')
        if not math.isfinite(lat) or not math.isfinite(lng) or not (33 <= lat <= 39.5 and 124 <= lng <= 132):
            raise ValueError('Invalid coordinate')
        item.update(LAT=lat, LNG=lng, geoLocation={'lat': lat, 'lon': lng})
    except (ValueError, TypeError):
        pass
    return item


def collect(kind, key, output, collected_on, resume=False):
    dt.date.fromisoformat(collected_on)
    output.mkdir(parents=True, exist_ok=resume)
    if any((output/name).exists() for name in ('facilities.jsonl','manifest.json','official.jsonl')):
        raise ValueError('Completed or normalized output cannot be resumed/overwritten')
    raw = output / 'official.jsonl.partial'
    endpoint = ENDPOINTS[kind]
    def fetch(page):
        parameters = {'serviceKey': urllib.parse.unquote(key.strip()), 'pageNo': page, 'numOfRows': 100}
        if kind == 'stores':
            parameters.update(returnType='JSON', **{'cond[SALS_STTS_CD::EQ]': '01'})
        for attempt in range(3):
            try:
                with urllib.request.urlopen(endpoint + '?' + urllib.parse.urlencode(parameters), timeout=45) as response:
                    body = response.read(4_000_001)
                if len(body) > 4_000_000:
                    raise ValueError('Oversized response')
                return decode(body)
            except urllib.error.HTTPError as error:
                if error.code not in (429, 500, 502, 503, 504) or attempt==2:
                    raise RuntimeError(f'Official API HTTP {error.code} on page {page}; check approval/network. Partial output is incomplete.') from None
            except (urllib.error.URLError, TimeoutError):
                if attempt==2: raise RuntimeError(f'Official API network failure on page {page}; partial output is incomplete.') from None
            except Exception:
                raise RuntimeError(f'Official API response rejected on page {page}; partial output is incomplete.') from None
            time.sleep(1 + attempt)
    count = 0
    expected, items = fetch(1)
    if expected < 1:
        raise RuntimeError('Empty official dataset; current data preserved')
    if resume and raw.exists():
        first=[]
        with raw.open(encoding='utf-8') as previous:
            for line in previous:
                record=json.loads(line)
                if not record.get('hpid' if kind=='emergency' else 'MNG_NO'):
                    raise ValueError('Partial file schema mismatch')
                if count<100:first.append(record)
                count+=1
        if (count%100 and count!=expected) or count>expected or first!=items[:len(first)]:
            raise ValueError('Partial data does not match current source/page boundaries')
    start_page=count//100+1 if count<expected else (expected+99)//100+1
    with raw.open('a' if resume else 'w', encoding='utf-8', newline='\n') as target, ThreadPoolExecutor(max_workers=2) as executor:
        pages = (expected + 99) // 100
        pending = {}
        for page in range(start_page, pages + 1):
            if page > 1:
                total, items = pending.pop(page).result() if page in pending else fetch(page)
                if total != expected:
                    raise RuntimeError('Official total changed during collection; use a fresh output directory')
            for upcoming in range(page + 1, min(page + 2, pages) + 1):
                if upcoming not in pending:
                    pending[upcoming] = executor.submit(fetch, upcoming)
            if not items or len(items) > 100 or page < pages and len(items) != 100:
                raise RuntimeError('Incomplete official page')
            for item in items:
                target.write(json.dumps(item, ensure_ascii=False) + '\n')
                count += 1
            if page % 25 == 0 or page == pages:
                print(f'{kind}: {count}/{expected} records', flush=True)
            time.sleep(0.3)
    if count != expected:
        raise RuntimeError('Official count mismatch')
    source = output / 'official.jsonl'
    raw.rename(source)
    if kind == 'stores':
        normalized = output / 'normalized'
        manifest = normalize(source, normalized, collected_on, national=True)
        for name in ('facilities.jsonl', 'manifest.json'):
            (normalized / name).rename(output / name)
        normalized.rmdir()
    else:
        ids = set()
        with source.open(encoding='utf-8') as records, (output / 'facilities.jsonl.partial').open('w', encoding='utf-8', newline='\n') as target:
            for line in records:
                item = emergency_item(json.loads(line), collected_on)
                if item['ID'] in ids:
                    raise RuntimeError('Duplicate emergency institution ID')
                ids.add(item['ID'])
                target.write(json.dumps(item, ensure_ascii=False) + '\n')
        (output / 'facilities.jsonl.partial').rename(output / 'facilities.jsonl')
        manifest = {'source': 'https://www.data.go.kr/data/15000563/openapi.do', 'provider': '국립중앙의료원',
                    'scope': '전국', 'license': '이용허락범위 제한 없음', 'collectedOn': collected_on,
                    'recordCount': count, 'status': 'complete', 'sha256': digest(output / 'facilities.jsonl')}
    manifest['rawSha256'] = digest(source)
    manifest['endpoint'] = endpoint
    manifest['note'] = 'Collection is an institution/license directory snapshot, not live availability, medicine stock or opening status.'
    (output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--kind', choices=ENDPOINTS, required=True)
    parser.add_argument('--key-file', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--collected-on', required=True)
    parser.add_argument('--resume', action='store_true', help='Resume a complete-page official.jsonl.partial after validating its first page against the source')
    args = parser.parse_args()
    try:
        result = collect(args.kind, args.key_file.read_text(encoding='utf-8-sig'), args.output, args.collected_on,args.resume)
        print(json.dumps(result.get('counts', {'records': result.get('recordCount')})))
    except RuntimeError as error:
        print(str(error))
        raise SystemExit(1)
    except Exception:
        print('Collection failed; check approval, schema and output. Credentials/response details suppressed. Existing datasets are unchanged.')
        raise SystemExit(1)
