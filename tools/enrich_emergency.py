"""Add official institution departments/hours to an NMC directory snapshot."""
import argparse
import collections
import datetime as dt
import json
from pathlib import Path
import time
import urllib.parse
import urllib.request
from collect_facilities import decode
from normalize_stores import digest

ENDPOINT = 'https://apis.data.go.kr/B552657/ErmctInfoInqireService/getEgytBassInfoInqire'


def apply_details(item, details):
    if item['ID'] != 'nmc-emergency:' + details.get('hpid', ''):
        raise ValueError('NMC detail identity does not match list identity')
    result = dict(item)
    result['DEPT_NAME'] = sorted({v.strip() for v in (details.get('dgidIdName') or '').split(',') if v.strip()})
    result['PHONE'] = details.get('dutyTel3') or item['PHONE']
    raw = {}
    for index, day in enumerate(['월요일', '화요일', '수요일', '목요일', '금요일', '토요일', '일요일', '공휴일'], 1):
        for suffix, label in [('s', '진료시작시간_'), ('c', '진료종료시간_')]:
            if details.get(f'dutyTime{index}{suffix}'):
                raw[label + day] = details[f'dutyTime{index}{suffix}']
    result['RAW_DETAIL'] = raw or None
    return result


def enrich(source, output, key):
    manifest = json.loads((source / 'manifest.json').read_text(encoding='utf-8'))
    if digest(source / 'facilities.jsonl') != manifest['sha256']:
        raise ValueError('Directory checksum mismatch')
    output.mkdir(parents=True, exist_ok=False)
    count = 0
    with (source / 'facilities.jsonl').open(encoding='utf-8') as rows, (output / 'facilities.jsonl.partial').open('w', encoding='utf-8', newline='\n') as target, (output / 'details.jsonl.partial').open('w', encoding='utf-8', newline='\n') as raw:
        for line in rows:
            item = json.loads(line)
            if not item['ID'].startswith('nmc-emergency:'):
                raise ValueError('Expected NMC directory only')
            query = urllib.parse.urlencode({'ServiceKey': urllib.parse.unquote(key.strip()), 'HPID': item['ID'].split(':', 1)[1], 'pageNo': 1, 'numOfRows': 1})
            try:
                with urllib.request.urlopen(ENDPOINT + '?' + query, timeout=30) as response:
                    payload = response.read(1_000_001)
                if len(payload) > 1_000_000:
                    raise ValueError('Response too large')
                total, details = decode(payload)
                if total != 1 or len(details) != 1:
                    raise ValueError('Expected one matching institution')
                result = apply_details(item, details[0])
            except Exception:
                raise RuntimeError('Emergency detail collection failed; partial output is not complete. Credential/URL details suppressed.') from None
            target.write(json.dumps(result, ensure_ascii=False) + '\n')
            raw.write(json.dumps(details[0], ensure_ascii=False) + '\n')
            count += 1
            if count % 50 == 0:
                print(f'Emergency details: {count}', flush=True)
            time.sleep(0.15)
    if count != manifest['recordCount']:
        raise ValueError('Directory count mismatch')
    (output / 'facilities.jsonl.partial').rename(output / 'facilities.jsonl')
    (output / 'details.jsonl.partial').rename(output / 'details.jsonl')
    manifest.update(sha256=digest(output / 'facilities.jsonl'), detailsSha256=digest(output / 'details.jsonl'),
                    listSha256=manifest['sha256'], detailEndpoint=ENDPOINT, withDetail=count,
                    detailCollectedAt=dt.datetime.now(dt.timezone.utc).isoformat(),
                    note='Institution outpatient hours are not emergency room availability. Bed counts and admission flags are not exposed to users from this snapshot.')
    (output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Verified {count} emergency institution details', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--key-file', type=Path, required=True)
    args = parser.parse_args()
    try:
        enrich(args.source, args.output, args.key_file.read_text(encoding='utf-8-sig'))
    except Exception:
        print('Enrichment failed; original directory is unchanged. Details and credentials suppressed.')
        raise SystemExit(1)
