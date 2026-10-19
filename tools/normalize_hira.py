"""Normalize the official HIRA quarterly workbook bundle; requires openpyxl."""
import argparse
import collections
import hashlib
import io
import json
import math
from pathlib import Path
import zipfile
import openpyxl

def normalize(source, output, period):
    output.mkdir(parents=True, exist_ok=False)
    departments = collections.defaultdict(set)
    details = {}
    counts = collections.Counter()
    identifiers = set()
    with zipfile.ZipFile(source) as archive:
        if archive.testzip() is not None:
            raise ValueError("Archive checksum failed")
        def records(prefix):
            names = [n for n in archive.namelist() if n.split('/')[-1].startswith(prefix + '.') and n.endswith('.xlsx')]
            if len(names) != 1:
                raise ValueError(f"Expected one workbook for {prefix}")
            book = openpyxl.load_workbook(io.BytesIO(archive.read(names[0])), read_only=True, data_only=True)
            try:
                iterator = iter(book.active.values)
                headers = next(iterator)
                for values in iterator:
                    yield dict(zip(headers, values))
            finally:
                book.close()
        for row in records('5'):
            if row.get('진료과목코드명'):
                departments[str(row['암호화요양기호'])].add(str(row['진료과목코드명']))
        for row in records('4'):
            key = str(row['암호화요양기호'])
            if key in details:
                raise ValueError("Duplicate detail identifier")
            details[key] = row
        target = output / 'facilities.jsonl.partial'
        with target.open('w', encoding='utf-8', newline='\n') as stream:
            for prefix in ('1', '2'):
                for row in records(prefix):
                    key = str(row['암호화요양기호'])
                    if not key or key == 'None' or key in identifiers:
                        raise ValueError("Invalid or duplicate facility identifier")
                    identifiers.add(key)
                    lat, lng = row.get('좌표(Y)'), row.get('좌표(X)')
                    valid = all(isinstance(v, (int, float)) and math.isfinite(v) for v in (lat, lng)) and 33 <= lat <= 39.5 and 124 <= lng <= 132
                    item = {'ID': key, 'NAME': row['요양기관명'], 'CATEGORY_NAME': row['종별코드명'],
                            'ADDRESS': row.get('주소'), 'PHONE': row.get('전화번호'),
                            'DEPT_NAME': sorted(departments[key]), 'SOURCE': 'HIRA', 'SOURCE_PERIOD': period,
                            'RAW_BASIC': row, 'RAW_DETAIL': details.get(key)}
                    if prefix == '2' and not item['DEPT_NAME']:
                        item['DEPT_NAME'] = ['약국']
                    if valid:
                        item.update(LAT=lat, LNG=lng, geoLocation={'lat':lat,'lon':lng})
                    else:
                        counts['missingOrInvalidCoordinates'] += 1
                    counts['records'] += 1
                    counts['hospitals' if prefix == '1' else 'pharmacies'] += 1
                    if item['RAW_DETAIL']:
                        counts['withDetail'] += 1
                    stream.write(json.dumps(item, ensure_ascii=False, default=str) + '\n')
        target.rename(output / 'facilities.jsonl')
    manifest = {'source': 'https://opendata.hira.or.kr/op/opc/selectOpenData.do?sno=11925',
                'provider':'건강보험심사평가원', 'period':period, 'license':'공공누리 제1유형 (출처표시)',
                'counts':dict(counts), 'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),
                'sha256':hashlib.sha256((output/'facilities.jsonl').read_bytes()).hexdigest()}
    (output/'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2),encoding='utf-8')
    print(json.dumps(dict(counts)),flush=True)

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--period',required=True)
    args=parser.parse_args()
    normalize(args.source,args.output,args.period)
