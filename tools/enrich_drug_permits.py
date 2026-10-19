"""Collect official detail documents for the exact IDs in a verified OTC permit snapshot."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import json
import hashlib
import os
from pathlib import Path
import shutil
import time
import urllib.error
import urllib.parse
import urllib.request
from collect_drug_permits import decode, digest

ENDPOINT='https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService08/getDrugPrdtPrmsnDtlInq08'


def enrich(source, output, key, resume=False):
    manifest=json.loads((source/'manifest.json').read_text(encoding='utf-8'))
    if digest(source/'permits.jsonl')!=manifest['sha256'] or manifest['status']!='complete':
        raise ValueError('OTC directory checksum/status mismatch')
    with (source/'permits.jsonl').open(encoding='utf-8') as stream:
        source_rows=list(map(json.loads,stream))
    if not source_rows or any(row.get('SPCLTY_PBLC') not in ('일반의약품','일반') for row in source_rows):
        raise ValueError('Expected a nonempty explicitly classified OTC directory')
    ids={row['ITEM_SEQ'] for row in source_rows}
    output.mkdir(parents=True,exist_ok=resume)
    if any((output/name).exists() for name in ('permits.jsonl','permit-details.jsonl','manifest.json')):
        raise ValueError('Completed output cannot be resumed or overwritten')
    def fetch(page):
        query=urllib.parse.urlencode({'serviceKey':urllib.parse.unquote(key.strip()),'pageNo':page,'numOfRows':100,'type':'json'})
        for attempt in range(3):
            try:
                with urllib.request.urlopen(ENDPOINT+'?'+query,timeout=60) as response:payload=response.read(64_000_001)
                if len(payload)>64_000_000:raise ValueError('Oversized detail page')
                return decode(payload)
            except urllib.error.HTTPError as error:
                if error.code not in (429,500,502,503,504) or attempt==2:
                    raise RuntimeError(f'Official detail API HTTP {error.code} on page {page}; credentials suppressed.') from None
            except (urllib.error.URLError,TimeoutError):
                if attempt==2:raise RuntimeError(f'Official detail network failure on page {page}; partial output retained.') from None
            except Exception:
                raise RuntimeError(f'Official detail response rejected on page {page}; partial output retained.') from None
            time.sleep(attempt+1)
    expected,first=fetch(1)
    if expected<1:raise ValueError('Empty official detail dataset')
    def page_hash(items):return hashlib.sha256(json.dumps(items,ensure_ascii=False,sort_keys=True).encode()).hexdigest()
    partial=output/'permit-details.jsonl.partial';progress=output/'progress.json'
    count=kept=offset=0;seen=set();hasher=hashlib.sha256();start_page=1
    if resume:
        checkpoint=json.loads(progress.read_text(encoding='utf-8'))
        if checkpoint['sourceSha256']!=manifest['sha256'] or checkpoint['expected']!=expected or checkpoint['firstPageSha256']!=page_hash(first):
            raise ValueError('Checkpoint does not match current official source')
        if partial.stat().st_size!=checkpoint['bytes'] or digest(partial)!=checkpoint['sha256']:
            raise ValueError('Partial output does not match committed checkpoint')
        count=checkpoint['processedRows'];start_page=checkpoint['nextPage'];offset=checkpoint['bytes']
        if count!=min((start_page-1)*100,expected) or not 1<=start_page<=(expected+99)//100+1:
            raise ValueError('Invalid checkpoint page boundaries')
        if start_page>1:
            previous_total,previous=fetch(start_page-1)
            if previous_total!=expected or page_hash(previous)!=checkpoint['lastPageSha256']:
                raise ValueError('Previous source page changed')
        with partial.open('rb') as stream:
            for line in stream:
                row=json.loads(line)
                if row.get('ITEM_SEQ') not in ids or row.get('ETC_OTC_CODE') not in ('일반의약품','일반'):
                    raise ValueError('Partial detail identity mismatch')
                hasher.update(line);kept+=1;seen.add(row['ITEM_SEQ'])
        if kept!=checkpoint['keptRows']:raise ValueError('Partial record count mismatch')
    with partial.open('ab' if resume else 'xb') as target,ThreadPoolExecutor(max_workers=2) as executor:
        pages=(expected+99)//100;pending={}
        for page in range(start_page,pages+1):
            total,items=(expected,first) if page==1 else pending.pop(page).result() if page in pending else fetch(page)
            if total!=expected or len(items)!=min(100,expected-count):raise ValueError('Official detail count/page changed')
            for upcoming in range(page+1,min(page+2,pages)+1):
                if upcoming not in pending:pending[upcoming]=executor.submit(fetch,upcoming)
            for row in items:
                count+=1
                identifier=row.get('ITEM_SEQ')
                if not isinstance(identifier,str) or not row.get('ITEM_NAME'):raise ValueError('Missing detail identity/name')
                if identifier not in ids:continue
                if row.get('ETC_OTC_CODE') not in ('일반의약품','일반'):raise ValueError('OTC detail classification mismatch')
                line=(json.dumps(row,ensure_ascii=False)+'\n').encode('utf-8')
                target.write(line);hasher.update(line);offset+=len(line);kept+=1;seen.add(identifier)
            target.flush();os.fsync(target.fileno())
            checkpoint={'sourceSha256':manifest['sha256'],'expected':expected,'firstPageSha256':page_hash(first),'lastPageSha256':page_hash(items),
                        'nextPage':page+1,'processedRows':count,'keptRows':kept,'bytes':offset,'sha256':hasher.hexdigest()}
            temporary=output/'progress.json.partial';temporary.write_text(json.dumps(checkpoint),encoding='utf-8');temporary.replace(progress)
            if page%25==0 or page==pages:print(f'Detail rows: {count}/{expected}; matched OTC IDs: {len(seen)}/{len(ids)}',flush=True)
            time.sleep(0.3)
    if count!=expected:raise ValueError('Incomplete official detail collection')
    # Missing source details stay explicit rather than creating invented medical content.
    (output/'permit-details.jsonl.partial').rename(output/'permit-details.jsonl')
    shutil.copyfile(source/'permits.jsonl',output/'permits.jsonl')
    manifest.update(detailEndpoint=ENDPOINT,detailCollectedAt=dt.datetime.now(dt.timezone.utc).isoformat(),
                    detailsSha256=digest(output/'permit-details.jsonl'),detailSourceRows=count,detailRecordCount=kept,
                    withDetails=len(seen),missingDetailIds=sorted(ids-seen),
                    detailNote='Official documents and fields retained unmodified. Join only exact OTC ITEM_SEQ; permit and revision dates remain distinct.')
    (output/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    progress.unlink(missing_ok=True)
    return manifest


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,required=True);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--key-file',type=Path,required=True)
    parser.add_argument('--resume',action='store_true',help='Resume only an intact checkpoint matching the current source and previous committed page')
    args=parser.parse_args()
    try:
        result=enrich(args.source,args.output,args.key_file.read_text(encoding='utf-8-sig'),args.resume)
        print(json.dumps({'withDetails':result['withDetails'],'missingDetails':len(result['missingDetailIds'])}))
    except RuntimeError as error:print(str(error));raise SystemExit(1)
    except Exception:print('Permit enrichment failed; check source, API schema and fresh output. Credentials/response details suppressed.');raise SystemExit(1)
