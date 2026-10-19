"""Normalize official Seoul OTC-store CSV; keep licensed active stores only."""
import argparse
import collections
import csv
import datetime as dt
import hashlib
import json
import math
from pathlib import Path

SOURCE = 'https://data.seoul.go.kr/dataList/OA-16483/S/1/datasetView.do'
NATIONAL_SOURCE = 'https://www.data.go.kr/data/15154791/openapi.do'
API_FIELDS = {'OPN_ATMY_GRP_CD':'개방자치단체코드','MNG_NO':'관리번호','SALS_STTS_CD':'영업상태코드',
              'BPLC_NM':'사업장명','ROAD_NM_ADDR':'도로명주소','LOTNO_ADDR':'지번주소','TELNO':'전화번호',
              'CRD_INFO_X':'좌표정보(X)','CRD_INFO_Y':'좌표정보(Y)'}


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def normalize(source, output, collected_on, national=False):
    from pyproj import Transformer
    transform = Transformer.from_crs('EPSG:5174', 'EPSG:4326', always_xy=True, allow_ballpark=False)
    dt.date.fromisoformat(collected_on)
    output.mkdir(parents=True, exist_ok=False)
    counts = collections.Counter()
    identifiers = set()
    temporary = output / 'facilities.jsonl.partial'
    with source.open(encoding='utf-8' if national else 'cp949', newline='') as stream, temporary.open('w', encoding='utf-8', newline='\n') as target:
        reader = (json.loads(line) for line in stream) if national else csv.DictReader(stream)
        required = {'관리번호', '개방자치단체코드', '영업상태코드', '사업장명', '좌표정보(X)', '좌표정보(Y)'}
        if not national and not required.issubset(reader.fieldnames or []):
            raise ValueError('Unexpected official CSV schema')
        for original in reader:
            row = {API_FIELDS.get(key,key) if national else key: str(value if value is not None else '').strip() for key, value in original.items()}
            if not required.issubset(row):
                raise ValueError('Unexpected official API schema')
            counts['sourceRows'] += 1
            if row['영업상태코드'] != '01':
                counts['excludedInactive'] += 1
                continue
            if not row['관리번호'] or not row['개방자치단체코드'] or not row['사업장명']:
                raise ValueError('Active store lacks a stable identifier or name')
            identifier = 'mois-store:' + row['개방자치단체코드'] + ':' + row['관리번호']
            if identifier in identifiers:
                raise ValueError('Duplicate active store identifier')
            identifiers.add(identifier)
            item = {'ID': identifier, 'NAME': row['사업장명'], 'CATEGORY_NAME': '안전상비의약품 판매업소',
                    'ADDRESS': row.get('도로명주소') or row.get('지번주소') or '', 'PHONE': row.get('전화번호', ''),
                    'DEPT_NAME': [], 'SOURCE': 'MOIS_OTC' if national else 'SEOUL_OTC', 'SOURCE_PERIOD': collected_on,
                    'RAW_BASIC': original, 'RAW_DETAIL': None}
            try:
                x, y = float(row['좌표정보(X)']), float(row['좌표정보(Y)'])
                if not all(math.isfinite(v) and v != 0 for v in (x, y)):
                    raise ValueError('Invalid projected coordinates')
                lng, lat = transform.transform(x, y, errcheck=True)
                # Seoul dataset: reject misplaced coordinates, retaining the list entry.
                if not ((33 <= lat <= 39.5 and 124 <= lng <= 132) if national else (37.4 <= lat <= 37.75 and 126.7 <= lng <= 127.25)):
                    raise ValueError('Coordinate outside dataset region')
                item.update(LAT=lat, LNG=lng, geoLocation={'lat': lat, 'lon': lng})
                counts['withCoordinates'] += 1
            except (ValueError, OverflowError):
                counts['missingOrInvalidCoordinates'] += 1
            counts['records'] += 1
            target.write(json.dumps(item, ensure_ascii=False) + '\n')
    if not counts['records']:
        raise ValueError('No active stores; do not replace the current dataset')
    filename = output / 'facilities.jsonl'
    temporary.rename(filename)
    manifest = {'source': NATIONAL_SOURCE if national else SOURCE, 'provider': '행정안전부' if national else '서울특별시', 'scope': '전국' if national else '서울특별시',
                'license': '이용허락범위 제한 없음' if national else '공공누리 제1유형 (출처표시)', 'collectedOn': collected_on,
                'sourceCrs': 'EPSG:5174', 'targetCrs': 'EPSG:4326', 'counts': dict(counts),
                'sourceSha256': digest(source), 'sha256': digest(filename), 'status': 'complete',
                'note': 'Portal provides data with a three-day delay. Collection date is not every record revision date. License status does not prove current stock or opening hours.'}
    (output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--collected-on', required=True)
    args = parser.parse_args()
    print(json.dumps(normalize(args.source, args.output, args.collected_on)['counts']))
