# Dr.Geo - 의료 시각화 통합 플랫폼  
**Dr.Geo**는 사용자의 위치 기반 병원 검색, 질병 자가진단, 의약품 정보 탐색, 리뷰 및 커뮤니티 기능 등을 통합한 의료 정보 시각화 서비스입니다. 비대면 의료 수요 증가와 정보의 접근성 향상이라는 사회적 니즈에 대응하여 개발된 올인원 의료 플랫폼입니다.  

## 🩺 프로젝트 개요  
- **프로젝트명**: Dr.Geo - 의료 시각화 서비스  
- **개발기간**: 약 5주  
- **개발환경**: Spring Boot, Elasticsearch, Redis, H2 DB, Kakao Map API, Java, HTML/CSS/JS  
- **주요 기능**: 위치 기반 병원/약국 검색 및 진료 시간 확인, 질병 정보 탐색 및 자가진단 서비스, 의약품 및 건강기능식품 정보 검색 및 리뷰 작성, 커뮤니티 게시판 운영, 카카오/네이버 로그인, 이메일 인증, 주소 검색 API 등 사용자 인증 시스템  

## 💡 기획 배경  
코로나19 이후 의료기관 정보에 대한 신뢰성과 실시간 접근성에 대한 수요가 증가하였으며, 지도 기반 검색, 데이터 크롤링, 정보 시각화를 활용해 사용자 친화적인 UX를 제공하기 위해 기획되었습니다.  

### 🔍 벤치마킹  
- 네이버 지도: https://map.naver.com/  
- 건강보험심사평가원: https://www.hira.or.kr/  
- 이대목동병원: https://mokdong.eumc.ac.kr/  
- 약학정보원: https://www.health.kr/  
- 똑닥 커뮤니티: https://ddocdoc.com/  

## 🛠 기술 스택  
- **Backend**: Java, Spring Boot, Redis, Elasticsearch, H2 Database  
- **Frontend**: HTML, CSS, JavaScript (Thymeleaf 기반)  
- **외부 API**: Kakao Map API, 네이버/카카오 로그인, 이메일 인증, 주소 검색 API  
- **협업 도구**: GitHub, Google Drive, Google Slides, Google Sheets  

## 🧩 주요 기능 및 화면 구성  
- 병원/약국 검색: 카카오맵 API 기반 위치 표시, 진료과·병원명·지역 필터링, 진료시간 및 응급실 유무 정보 제공  
- 자가진단: 증상 선택 → 관련 질병 추천 및 상세 설명 제공, 질병 DB 기반 정밀 진단  
- 의약품/건강기능식품 정보: 이름 및 효능 기반 검색, 상세 설명, 리뷰 작성, 좋아요 정렬 기능  
- 커뮤니티: 병원/약국 리뷰 작성 및 수정, 자유게시판 운영  
- 회원 인증 시스템: 카카오/네이버 로그인, 이메일 인증, Spring Security 기반 권한 등급 설정  

## 🧬 시스템 아키텍처  
사용자 → Spring Boot Controller → Service Layer → Elasticsearch (검색) / Redis (인기순 캐싱) / H2 Database (기본 데이터) → 외부 API (지도 및 인증)  

## 🧑‍🤝‍🧑 역할 분담  
- 문동규: PL, 통합/거리기반 검색 알고리즘, 지도 API, Elasticsearch, 크롤링/전처리, 통합버전관리
- 조현호: 병원 랭킹, 리뷰 기능, 뉴스 크롤링, 응급실/진료정보, 카카오맵 마커  
- 홍성준: DB 설계, 페이지 레이아웃, 네비게이션바, 오류 처리  
- 이우진: 약국/건기식 검색, 리뷰 기능, 편의점 상비약  
- 강정호: 커뮤니티 게시판 구현  
- 이하신: 로그인/회원가입, 소셜 로그인, 인증 API, 권한 설정  
- 고윤정: 질병 정보 크롤링, 자가진단 페이지 설계 및 DB 정의  

## 📅 산출물  
- DB 정의서, SQL 설계서, 기능 흐름도, UI 시안, 사이트맵, 시연 영상  

## 🚧 보완 및 개선 계획  
- 자가진단: 결과 항목이 광범위함 → 질병 DB 체계화 및 선택 로그 기록 기능 추가  
- 배포: 로컬 서버 한정 → 클라우드 배포 환경 구성 예정  
- 회원관리: 제재 해제 수동 처리 → 스케줄러 기반 자동 해제 기능 도입 예정  

## 📄 참고 자료  
- 서울아산병원: https://www.amc.seoul.kr/asan/main.do  
- 네이버 케어: https://care.naver.com/  
- 서울대병원: https://www.snuh.org/main.do  
- 카카오 로그인 구현: https://ddonghyeo.tistory.com/16  
- 메일 인증 구현: https://every-coding.tistory.com/1

## .NET 의약품 검색 데이터

`tools/collect_drugs.py`는 승인된 식약처 e약은요 API를 수집합니다. Python 3 표준 라이브러리만 필요합니다. 인증키는 저장소 밖의 파일 또는 `DATA_GO_KR_SERVICE_KEY` 환경변수로 전달하세요.

```sh
python tools/collect_drugs.py --key-file /private/data-go-kr.key --output /data/mfds-drugs
```

원본 행은 `drugs.jsonl`, 수집 시각·출처·건수·SHA-256은 `manifest.json`에 저장합니다. 같은 품목번호의 중복 행을 보존하며, 전체 건수가 맞지 않으면 완료 파일을 만들지 않습니다. 재수집은 새 디렉터리에서 실행하세요.

로컬 실행에서는 `Drugs__Path`에 `drugs.jsonl`의 절대 경로를 지정합니다. `/drugs.html`에서 제품명·업체명·품목번호 검색과 공식 설명 조회를 지원합니다. 서로 다른 원본 설명은 수정일과 함께 모두 표시합니다. 이 데이터셋은 e약은요 제공 범위이며 모든 전문의약품을 포함하지 않습니다.

집 PC 또는 서버에서는 수집 디렉터리를 복사하고 `.env`에 `DRUG_DATA_DIR`을 해당 디렉터리 경로로 설정한 뒤 실행합니다. 검색 서버에는 공공데이터 API 인증키가 필요하지 않습니다.

```sh
docker compose -f compose.yaml -f compose.data.yaml up -d --build
```

데이터는 읽기 전용 볼륨으로 연결됩니다. 데이터 파일이나 인증키를 이미지 또는 Git에 포함하지 마세요. 데이터 파일 변경 후 컨테이너를 재시작하면 새 내용을 읽습니다. 의약품 데이터 경로가 미설정이면 해당 API만 503을 반환하고, 잘못된 파일을 지정하면 시작 시 오류를 반환합니다.

### 일반의약품 허가 목록·유효성분·허가 원문

기존 일반의약품 목록과 대조해 [식약처 의약품 제품 허가정보](https://www.data.go.kr/data/15095677/openapi.do)를 추가했습니다. 목록·상세 조회 활용 권한이 필요하며 Python 3.11 이상 표준 라이브러리만 사용합니다. 수집은 기존 출력물을 덮어쓰지 않습니다.

```sh
python tools/collect_drug_permits.py --key-file /private/data-go-kr.key --output /data/mfds-drug-permits-20261019
python tools/enrich_drug_permits.py --source /data/mfds-drug-permits-20261019 --key-file /private/data-go-kr.key --output /data/mfds-drug-permits-full-20261019
```

2026-10-19 목록 수집본은 전체 허가 42,704행 중 일반의약품으로 명시된 10,776품목입니다. e약은요 4,729품목과 정확히 일치하는 품목번호 4,720개만 연결합니다. 허가 목록에 없는 기존 9품목도 보존하며 합계 10,785품목을 조회합니다. 같은 이름의 다른 품목번호는 합치지 않고 기존 댓글의 품목 연결도 유지합니다. 주성분이 비어 있는 68품목은 임의로 보충하지 않습니다.

상세 수집은 전체 공식 페이지를 검증하며 해당 일반의약품 식별자의 원본만 `permit-details.jsonl`에 저장합니다. 10,776품목 모두 상세정보와 효능·용법·주의사항 원문 32,328건을 연결했습니다. 한글 유효성분은 10,723품목에 제공됩니다. 한글 유효성분·영문 주성분, 효능·효과 원문을 검색할 수 있고, 용법·용량·사용상 주의사항·원료약품 및 분량·저장 방법을 상세에서 확인합니다. e약은요 설명과 허가 원문은 출처별로 구분합니다. 없는 설명은 없는 상태로 표시합니다. 원문 제목·문단·표·셀 병합을 표시하되 공급자 스크립트·이벤트·외부 리소스는 삽입하지 않습니다. 이미지·도표는 식약처의 공식 원문 링크에서도 확인할 수 있습니다.

취소·취하·폐업·유효기간만료 등 상태를 삭제하거나 정상으로 바꾸지 않고 원본대로 표시합니다. 허가일, 원본 변경일, 목록/상세 수집 시각은 서로 다릅니다. 허가 상태만으로 현재 판매·공급 여부를 판단하지 않습니다. 상세파일은 목록에 있는 일반의약품 식별자로만 연결하며, 앱 시작 시 manifest가 있으면 SHA-256·완료 상태·수집 시각을 검증합니다. 상세파일은 유효한 manifest가 필요합니다.

상세 수집은 완료 페이지마다 부분 파일을 flush하고 해시·바이트 수·진행 상태를 기록합니다. 중단되면 같은 명령에 `--resume`을 추가합니다. 원본 목록 해시·현재 건수·첫 페이지·이전 페이지·부분 파일 해시와 페이지 경계가 일치할 때만 이어갑니다. 기록 중 파일이 손상되거나 공급자 페이지가 바뀌면 중단하므로 새 출력 디렉터리에서 수집하세요. 완료본·기존 검색 데이터는 덮어쓰지 않습니다.

로컬 실행은 `DrugPermits__Path`에 `permits.jsonl`의 절대 경로를 지정합니다. 상세파일과 manifest는 같은 디렉터리에 둡니다. Docker에서는 `.env`의 `DRUG_PERMIT_DATA_DIR`에 해당 디렉터리를 지정하고 기존 웹 실행 명령에 `-f compose.permits.yaml`을 추가합니다. 데이터는 읽기 전용이며 웹 서버에 API 키는 필요하지 않습니다. 허가 데이터가 미설정이면 기존 e약은요 범위로 동작하고, 주성분 필터 요청은 503으로 명확하게 알립니다.

이동용 ZIP에는 아래처럼 `--permits-dir`을 추가합니다. 목록·상세 해시를 검증하고 선택한 허가 파일도 포함합니다. 압축을 푼 PC에서 `configure_portable.py`가 `DRUG_PERMIT_DATA_DIR` 경로를 생성합니다. 이 묶음의 웹 실행에는 `-f compose.permits.yaml`도 추가하세요.

```sh
python tools/package_portable.py --data-root /path/to/collected-data --facilities-dir facilities-20261019 --facilities-index item-facilities-20261019 --permits-dir mfds-drug-permits-full-20261019 --output /path/to/new-bundle.zip
```

## 심평원 병원·약국 데이터 갱신

공식 분기 자료: https://opendata.hira.or.kr/op/opc/selectOpenData.do?sno=11925 (건강보험심사평가원, 공공누리 제1유형). 공식 ZIP을 내려받은 뒤 실행합니다. 정규화에는 `openpyxl`이 필요합니다.

```sh
python tools/normalize_hira.py --source /data/hira.zip --output /data/hira-202606 --period 2026-06
python tools/import_facilities.py --url http://localhost:9200 --index item-hira-202606 --source /data/hira-202606/facilities.jsonl
```

새 인덱스에만 적재하며 동일 이름의 인덱스가 이미 있으면 중단합니다. Elasticsearch의 analysis-nori 플러그인이 필요합니다. 인증은 선택적으로 `ELASTICSEARCH_API_KEY` 환경변수를 사용합니다. 단일 노드 검증용으로 replica 0을 설정하므로 운영 클러스터 구성에 맞춰 조정하세요.

병원·약국 기본정보, 진료과목, 제공된 기관의 진료시간·휴진 안내 원본을 보존합니다. 좌표가 없거나 국내 범위를 벗어나는 행은 삭제하지 않고 위치 필드 없이 저장하므로 거리 검색에는 나오지 않습니다. 검색 결과의 진료시간·이용 안내를 펼치면 요일별 진료시간, 점심·접수시간, 휴진 및 주차·위치 안내를 볼 수 있습니다. 정보가 없는 요일은 휴진으로 간주하지 않습니다. 기존 인덱스의 응급실·편의점 자료는 이 심평원 인덱스에 포함되지 않으며 기존 인덱스에 보존됩니다.

검증 후 `.env`의 `ELASTICSEARCH_INDEX`를 새 이름으로 바꾸고 웹 컨테이너를 재생성하세요. 롤백은 이전 이름으로 되돌리면 됩니다. 복구 중인 원본 Elasticsearch 데이터 디렉터리를 직접 덮어쓰지 마세요.

### 전국 응급의료기관·안전상비의약품 판매업소 연결

국립중앙의료원 [전국 응급의료기관 정보](https://www.data.go.kr/data/15000563/openapi.do)의 목록·기본정보와 행정안전부 [전국 안전상비의약품 판매업소](https://www.data.go.kr/data/15154791/openapi.do)를 사용합니다. 두 API 활용 승인이 필요하며 인증키는 저장소 밖에 보관합니다. 제공되는 이용허락범위는 제한 없음입니다. 판매업소 좌표 변환에는 Python 3.11 이상과 `pyproj`가 필요합니다.

```sh
python -m pip install -r tools/requirements-data.txt
python tools/collect_facilities.py --kind stores --key-file /private/data-go-kr.key --output /data/mois-stores-20261019 --collected-on 2026-10-19
python tools/collect_facilities.py --kind emergency --key-file /private/data-go-kr.key --output /data/nmc-emergency-20261019 --collected-on 2026-10-19
python tools/enrich_emergency.py --source /data/nmc-emergency-20261019 --output /data/nmc-emergency-full-20261019 --key-file /private/data-go-kr.key
python tools/merge_facilities.py --source /data/hira-202606 --source /data/mois-stores-20261019 --source /data/nmc-emergency-full-20261019 --output /data/facilities-20261019
python tools/import_facilities.py --url http://localhost:9200 --index item-facilities-20261019 --source /data/facilities-20261019/facilities.jsonl
```

2026-10-19 수집본은 병원·약국 105,532건, 영업 상태 코드 `01`인 판매업소 42,054건, 응급의료기관 530건으로 합계 148,116건입니다. 응급의료기관은 530건 모두 기본정보를 연결했습니다. 판매업소의 EPSG:5174 좌표를 WGS84로 변환하며 좌표가 없거나 유효하지 않은 2,774건은 목록에 보존합니다. 심평원 자료의 11건을 포함해 좌표 없는 2,785건은 지도·거리 검색에서 제외됩니다. 수집일은 원본 전체가 그날 갱신됐다는 의미가 아닙니다.

수집 도중 중단되면 완료 파일을 만들지 않습니다. `official.jsonl.partial`에 완전한 페이지가 남아 있으면 같은 수집 명령에 `--resume`을 추가할 수 있습니다. 첫 페이지·현재 전체 건수·페이지 경계를 대조하고 이어서 수집하지만, 공급자가 수집 중 같은 건수로 행 순서를 바꾸는 것까지 보장하는 스냅샷은 아닙니다. 정규화는 중복 식별자와 건수 불일치를 거부합니다. 완료본 갱신에는 새 출력 디렉터리를 사용합니다. 병합 전 각 파일의 SHA-256을 검증하고 기존 데이터와 색인을 덮어쓰지 않습니다.

심평원 식별자는 유지하고 다른 출처는 `nmc-emergency:`·`mois-store:` 식별자를 사용합니다. 같은 병원이 심평원·응급의료기관 자료에 함께 있어도 이름만으로 합치지 않습니다. 검색 결과에 출처를 표시하며 기존 리뷰의 기관 연결을 유지합니다. `서울 응급실`·`편의점`·`상비약` 검색과 지도 업종 필터를 지원합니다. 상비약 판매점을 선택하면 진료과 필터를 해제합니다.

응급의료기관 상세의 시간은 기관 일반 진료시간이며 응급환자 수용 가능 여부나 실시간 가용 병상이 아닙니다. 판매업소는 등록 자료이며 현재 영업·재고·24시간 운영을 보장하지 않습니다. 해당 안내를 상세 화면에도 표시합니다. 원본 기본정보·수집 메타데이터는 보존하고 가용 병상 등 스냅샷 값을 현재 상태로 노출하지 않습니다. 서울 지역 CSV 대체 수집은 `tools/normalize_stores.py`를 사용하며 전국 자료와 중복 병합하지 않습니다.

### 새 PC에 Elasticsearch까지 함께 구성

Docker Desktop 또는 Docker Engine + Compose가 설치된 PC에서 `.env.example`을 `.env`로 복사하고 아래 값을 지정합니다. 데이터 경로는 압축을 푼 위치의 절대 경로이며 Windows에서도 `/` 구분자를 사용할 수 있습니다.

```dotenv
ELASTICSEARCH_URL=http://elasticsearch:9200
ELASTICSEARCH_INDEX=item-facilities-20261019
HIRA_DATA_DIR=C:/MedicalInfo/data/facilities-20261019
DRUG_DATA_DIR=C:/MedicalInfo/data/mfds-drugs-20261015-v2
```

`HIRA_DATA_DIR`은 기존 설정과의 호환을 위해 유지한 이름이며, 응급의료기관·판매업소까지 병합한 디렉터리도 지정할 수 있습니다.

```sh
docker compose -f compose.yaml -f compose.data.yaml -f compose.search.yaml up -d --build elasticsearch
docker compose -f compose.yaml -f compose.data.yaml -f compose.search.yaml run --rm import-facilities
docker compose -f compose.yaml -f compose.data.yaml -f compose.search.yaml up -d --build web
```

적재 명령은 빈 새 인덱스에 한 번 실행합니다. 이후 웹 재시작에는 마지막 명령만 사용합니다. 웹은 기본 `http://127.0.0.1:8080`, Elasticsearch는 `http://127.0.0.1:19200`에서 접근합니다. 기존 19200 포트 사용 시 `ELASTICSEARCH_LOCAL_PORT`를 변경하세요. 이 단일 노드 구성은 복구 검증과 동일한 Elasticsearch 8.17.3 및 nori 플러그인을 사용합니다. 인증 없는 Elasticsearch 포트를 외부에 공개하지 마세요. 외부 운영 환경에는 인증된 Elasticsearch와 HTTPS 프록시 설정이 필요합니다.

승인된 건강기능식품 상세정보는 같은 수집기로 받을 수 있습니다. 등록일(REGIST_DT)을 최신 수정일로 간주하지 않습니다.

```sh
python tools/collect_drugs.py --supplements --key-file /private/data-go-kr.key --output /data/mfds-supplements
```

건강기능식품 화면은 `/supplements.html`입니다. 로컬 실행은 `Supplements__Path`를 `supplements.jsonl` 경로로 지정합니다. Docker 실행에서는 `SUPPLEMENT_DATA_DIR`을 수집 디렉터리로 설정하고 기존 Compose 명령에 `-f compose.supplements.yaml`을 추가하세요. 제품명·업체명·신고번호 검색 및 공식 기능성·섭취방법·주의사항을 표시하며, 의약품과 구분된 화면을 사용합니다.

## 지도 검색

.NET 첫 화면에서 Leaflet 1.9.4 지도와 병원·약국 위치를 함께 표시합니다. 키워드 검색, 지도 중심 주변 검색, 업종·진료과·반경 필터, 목록에서 위치 보기, 위치 권한을 허용한 사용자의 현재 위치 이동을 지원합니다. 현재 위치는 버튼을 눌렀을 때만 요청하며 운영 환경에서는 HTTPS가 필요합니다.

지도 라이브러리는 소스에 포함되어 별도 지도 API 키가 필요하지 않습니다. 배경 지도는 OpenStreetMap 표준 타일을 사용하고 화면에 저작자 표시를 유지합니다. 타일 연결이 실패해도 목록 검색을 사용할 수 있습니다. 대규모 운영 전에는 [OSM 타일 사용 정책](https://operations.osmfoundation.org/policies/tiles/)에 맞는 제공자를 선택하세요. 표준 타일은 가용성을 보장하지 않으며 오프라인 일괄 다운로드를 지원하지 않습니다.

### 검증된 데이터와 소스 묶음 만들기

소스 변경을 커밋한 뒤 아래 명령으로 묶습니다. 세 데이터셋의 manifest 해시와 압축 무결성을 확인하며 기존 출력 파일을 덮어쓰지 않습니다. 인증키와 로컬 `.env`는 포함하지 않습니다.

```sh
python tools/package_portable.py --data-root /path/to/collected-data --facilities-dir facilities-20261019 --facilities-index item-facilities-20261019 --output /path/to/new-bundle.zip
```

집 PC에서 압축을 푼 뒤 `MedicalInfo` 폴더에서 `python tools/configure_portable.py`를 실행하면 생성된 `portable-data.json`의 색인·시설 디렉터리와 옆의 `data` 폴더 경로를 반영한 `.env`를 생성합니다. 세 데이터 파일의 해시를 다시 확인하며 기존 `.env`가 있으면 보존합니다. 이 단계와 이후 실행에는 API 인증키·좌표 변환 라이브러리가 필요하지 않습니다. 이어서 아래 명령을 순서대로 실행합니다.

```sh
docker compose -f compose.yaml -f compose.data.yaml -f compose.supplements.yaml -f compose.search.yaml up -d --build elasticsearch
docker compose -f compose.yaml -f compose.data.yaml -f compose.supplements.yaml -f compose.search.yaml run --rm import-facilities
docker compose -f compose.yaml -f compose.data.yaml -f compose.supplements.yaml -f compose.search.yaml up -d --build web
```

첫 적재 후에는 마지막 명령으로 재시작합니다. 브라우저에서 `http://127.0.0.1:8080`을 엽니다. 이것은 로컬 실행 구성이며 인터넷 공개 운영은 별도의 HTTPS·도메인 설정 후 진행합니다.

## .NET 회원·게시판·리뷰 이전 현황

디자인 개편은 기능 이전 이후 진행합니다. 기능별 원본 대조 현황과 미완료 항목은 `dotnet/migration-status.json`에서 관리합니다.

- `/account.html`: 아이디 가입, 로그인·로그아웃, 표시 이름 수정, 현재 비밀번호를 확인한 변경·탈퇴. ASP.NET Identity로 비밀번호를 해시 저장하고, 5회 로그인 실패 시 15분 잠금, 변경 후 다른 세션 즉시 무효화, CSRF 검사를 적용합니다. 가입 요청으로 관리자 권한을 지정할 수 없습니다. 이메일 인증/복구와 소셜 로그인은 아래 운영 설정 후 사용할 수 있습니다. 선택 프로필은 본인만 조회·수정하고 암호화 저장합니다. 관리자 회원·등급 관리는 아래 운영 절차를 사용합니다.
- `/community.html`: 자유/질문/공지 목록·검색·페이징, 글과 댓글 작성·수정·삭제. 서버가 작성자 권한을 확인하며 공지사항 작성은 Admin 역할만 허용합니다. 글을 저장한 뒤 상세 화면에서 이미지를 첨부·삭제할 수 있습니다.
- 검색 결과의 리뷰 링크: 별점 1~5점, 기관당 회원 1개, 작성자 수정·삭제, 전체 건수와 평균을 지원합니다. 기관 식별자를 검색 서버에서 확인하며 수정으로 기관을 바꿀 수 없습니다. 리뷰가 0개이면 평균은 null입니다.

탈퇴 시 계정은 삭제되지만 공개 게시글·댓글·리뷰는 작성자 연결을 제거해 ‘탈퇴 회원’으로 남깁니다. 삭제를 원하는 콘텐츠는 탈퇴 전에 직접 삭제합니다. 본인이 작성한 문의는 공개 여부와 관계없이 모든 답변·첨부파일과 함께 삭제됩니다. 다른 회원의 문의에 작성한 답변은 작성자 연결을 제거합니다.

회원·게시글·댓글·리뷰는 SQLite에 저장합니다. 로컬은 `State__Path` 디렉터리(미지정 시 웹 프로젝트의 `App_Data`), Docker는 `member-state` 볼륨의 `/state`를 사용합니다. 시작 시 EF 마이그레이션을 순서대로 적용하며 재시작해도 데이터와 세션 서명 키가 유지됩니다. 이 구성은 단일 웹 인스턴스용이며, 여러 서버를 동시에 운영하려면 공유 DB와 키 저장소로 전환해야 합니다.

`members.db`와 `keys`는 운영 비밀 데이터입니다. Git·이미지·공공데이터 배포 ZIP에 포함하지 않으며, 접근 제한된 볼륨에 보관합니다. 아래 `state_backup.py`는 SQLite backup API로 WAL에 남아 있는 커밋까지 포함한 DB 사본과 인증 키를 함께 저장합니다. 볼륨 삭제 명령은 회원 데이터도 삭제합니다. 공개 서비스는 HTTPS 설정과 미완료 인증/복구 기능 검증을 끝낸 뒤 운영하세요.

## 문의·답변 및 첨부파일

`/inquiries.html`에서 공개·비공개 문의 작성, 수정, 삭제, 검색, 페이징과 답변 작성·수정·삭제를 지원합니다. 비공개가 기본값입니다. 공개 문의에는 로그인한 회원이 답변할 수 있으며, 비공개 문의는 작성자와 Admin 역할 회원만 열람·답변할 수 있습니다. 목록의 건수와 검색, 답변, 첨부 다운로드에도 동일한 접근 제한이 적용됩니다. 답변 수정·삭제는 답변 작성자 또는 관리자만 가능합니다. 공개에서 비공개로 바꾸면 이전 답변 작성자도 문의 작성자나 관리자가 아닌 경우 접근할 수 없습니다.

게시판·문의 첨부는 PNG/JPEG/WebP만 허용하며 확장자와 파일 서명을 검사합니다. 파일당 5MB, 글당 3개이며 업로드와 삭제에 CSRF·작성자 권한 검사를 적용합니다. 원본 파일명은 경로로 사용하지 않습니다. 파일은 SQLite에 저장해 글 삭제 시 함께 삭제하고, 다운로드할 때 현재 문의 공개 범위를 확인합니다. 다운로드 응답은 attachment·nosniff·no-store로 제공합니다. 파일 서명 검사는 악성코드 검사나 이미지 전체 디코딩 검증을 대신하지 않습니다. PDF·SVG·실행 파일은 받지 않습니다.

문의·답변·첨부파일도 기존 `member-state` 볼륨에 포함되므로 동일한 백업 절차를 사용합니다. 이미지가 늘면 DB 용량도 커집니다. 운영 전 디스크 용량과 백업 보관 정책을 정해야 합니다. 최초 관리자 지정은 아래 호스트 명령을 사용하며, 가입 폼에서 관리자 권한을 지정할 수 없습니다.

## 관리자 회원·등급 관리

`/admin.html`에서 회원 검색·10개씩 페이징, 일반/의사/약사/관리자 권한 지정, 기간제 이용 제한(1~365일, 0은 해제), 등급 생성·이름 변경·삭제·배정, 관리 이력을 제공합니다. 계정 화면에는 로그인한 관리자에게만 관리 링크가 표시되며, 모든 관리 API는 서버에서 Admin 역할과 CSRF를 확인합니다. 등급은 분류용으로 권한과 별개입니다. 의사·약사 지정은 운영자가 자격을 확인한 뒤 처리해야 합니다.

최초 관리자는 먼저 정상 가입한 계정을 대상으로 서버 운영자가 지정합니다. 실행 중인 Compose 프로젝트와 동일한 폴더에서 아래 명령을 사용합니다. `YOUR_USERNAME`은 실제 가입한 아이디로 바꿉니다. 비밀번호를 명령줄에 넣지 않습니다.

```sh
docker compose exec web dotnet MedicalInfo.Web.dll --grant-admin YOUR_USERNAME
```

로컬 소스 실행에서는 웹 앱과 동일한 `State__Path` 환경변수를 지정한 터미널에서 `dotnet run --project dotnet/MedicalInfo.Web -- --grant-admin YOUR_USERNAME`을 사용합니다. .NET 9 검증 환경이라면 `--` 앞에 `-p:MedicalInfoFramework=net9.0`을 추가합니다. 명령은 이미 가입한 활성 계정만 지정하고, 관리자가 한 명이라도 있으면 실패합니다. 정상 처리 후 다시 로그인합니다. 이후 관리자는 화면에서 다른 회원에게 권한을 넘깁니다. 사용자 계정을 자동 생성하거나 기존 비밀번호를 바꾸지 않습니다.

권한 변경·이용 제한·해제는 대상 회원의 기존 세션을 무효화합니다. 제한 종료 후에는 다시 로그인할 수 있습니다. 관리자 본인의 권한 변경·이용 제한은 막으며, 관리자를 이용 제한하려면 먼저 다른 관리자가 일반 회원으로 변경해야 합니다. 마지막 관리자는 탈퇴할 수 없습니다. 사용 중인 등급은 삭제할 수 없고, 회원의 배정을 해제한 뒤 삭제해야 합니다.

관리 이력은 DB에 처리자·대상 식별자, 변경 내역, 사유를 저장합니다. API로 이력을 수정·삭제하는 기능은 제공하지 않습니다. DB 자체에 접근할 수 있는 운영자까지 막는 위변조 방지 로그는 아닙니다. 사유에 비밀번호·의료정보 등 민감한 내용을 입력하지 마세요. 이력도 회원 DB 백업에 포함됩니다.

6개월 이상 미접속 회원은 관리자 필터로 확인하며 미접속만으로 계정을 잠그지 않습니다. 이메일 인증·아이디 찾기·비밀번호 복구와 카카오·네이버 로그인은 아래 운영 설정 절차를 사용합니다.

## 이메일 인증·아이디 찾기·비밀번호 복구

`/recover.html`에서 가입한 이메일로 인증·아이디 확인·비밀번호 재설정 링크를 요청합니다. 아이디 찾기와 비밀번호 복구는 **인증된 이메일**에만 발송합니다. 미인증 계정은 먼저 인증 메일을 요청할 수 있습니다. 알 수 없는 주소와 발송 조건을 만족하지 않는 계정에도 동일한 응답을 반환합니다. 이용 제한 계정은 복구로 제한을 우회할 수 없습니다.

링크는 32바이트 난수로 생성하며 DB에는 SHA-256 해시만 저장합니다. 목적·이메일·계정 보안 스탬프에 연결되고 30분 후 만료됩니다. 같은 회원/목적의 재발급은 1분 간격이며 새 발급 시 이전 링크를 폐기합니다. 소비는 DB 트랜잭션으로 처리해 동시 요청도 한 번만 성공합니다. 링크를 여는 GET만으로는 처리하지 않고 확인 버튼을 누른 뒤 CSRF가 있는 POST로 완료합니다. 토큰은 URL fragment로 전달하며 페이지가 즉시 주소창 기록에서 제거하고, 브라우저 저장소에 보관하지 않습니다.

이메일 인증과 비밀번호 변경은 기존 로그인 세션을 무효화합니다. 비밀번호 재설정 후에는 새 비밀번호로 직접 로그인합니다. 정상 비밀번호 변경·관리자 권한 변경 등으로 보안 스탬프가 바뀌면 기존 복구 링크도 사용할 수 없습니다. 비밀번호 재설정은 로그인 실패 잠금을 해제하지만 관리자 이용 제한은 해제하지 않습니다.

### 운영 메일 연결

기본값 `MAIL_MODE=Disabled`에서는 외부 메일을 발송하지 않습니다. 발송 요청은 503을 반환하고 화면에도 설정 준비 중이라고 표시합니다. `.env`의 아래 값을 실제 발송 서비스에 맞게 설정한 뒤 Compose 웹 서비스를 다시 시작합니다.

```dotenv
MAIL_MODE=Smtp
MAIL_PUBLIC_URL=https://your-domain.example
MAIL_FROM=no-reply@your-domain.example
MAIL_HOST=smtp.your-provider.example
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-secret
MAIL_REQUIRE_CONFIRMED_EMAIL=true
```

`MAIL_PUBLIC_URL`은 경로·쿼리·사용자 정보가 없는 실제 HTTPS 원점이어야 합니다. 요청의 Host 헤더로 메일 링크를 만들지 않습니다. SMTP는 STARTTLS를 반드시 사용합니다. 465번 암시적 TLS 전용 서버 대신 STARTTLS를 제공하는 서버(일반적으로 587번)를 사용하세요. [Microsoft SendMailAsync 문서](https://learn.microsoft.com/en-us/dotnet/api/system.net.mail.smtpclient.sendmailasync?view=net-10.0)를 따릅니다. SMTP 자격 증명은 Git·배포 ZIP에 넣지 않습니다. 환경변수를 읽을 수 있는 호스트 운영자는 자격 증명에 접근할 수 있으므로 서버 접근을 제한합니다.

`MAIL_REQUIRE_CONFIRMED_EMAIL=true`이면 미인증 로그인과 기존 미인증 세션도 차단합니다. 이미 가입한 회원은 로그인 전에 복구 화면에서 인증 메일을 요청할 수 있습니다. 메일이 비활성화된 상태에서 인증만 필수로 설정하면 앱은 시작을 거부합니다. 최초 관리자 계정도 이 정책을 따릅니다.

발송은 최대 100개 메모리 대기열에서 순차적으로 처리하고 건당 15초 제한을 둡니다. 요청 접수는 배달 성공을 뜻하지 않습니다. 발송 실패 시 민감한 원문 없이 오류 유형만 기록합니다. 재시작 중 대기 메일은 사라질 수 있으므로 사용자는 1분 후 재요청해야 합니다. 운영 전 실제 수신함·스팸함 도착, 발신 도메인 인증 및 SMTP 실패 로그를 확인하세요. 외부 SMTP 실발송은 자격 증명과 도메인 설정 후 별도 검증해야 합니다.

### 외부 메일 없는 개발 검증

`ASPNETCORE_ENVIRONMENT=Development`, `Mail__Mode=Pickup`, `Mail__From=test@example.invalid`, `Mail__PublicUrl=http://localhost:실행포트`로 실행하면 외부로 보내지 않고 `State__Path/mail-pickup`에 `.eml` 파일을 저장합니다. 운영 환경에서는 Pickup 모드가 거부됩니다. 파일에는 일회용 링크가 있으므로 비밀 데이터로 취급하며 공유하지 말고 테스트 후 정리합니다. 공개 웹 디렉터리 아래의 보관함 경로는 금지합니다. 자동 검증은 임시 회원 DB와 이 보관함을 사용하며 실제 이메일 주소로 보내지 않습니다.

## 제품 필터·댓글·건강기능식품 추천

의약품과 건강기능식품 검색에서 검색어를 비워 전체 목록을 볼 수 있습니다. 제품명·업체명·품목번호 검색과 별도로 공식 효능(`efcyQesitm`) 또는 기능성(`MAIN_FNCTN`) 문구의 부분 일치 필터를 제공합니다. 두 조건은 AND로 결합하며 건수와 페이지가 같은 결과 집합을 사용합니다. 이 필터는 공식 문구 검색이며 새 질환 분류나 적응증을 추론하지 않습니다. 건강기능식품은 회원 추천순 정렬도 제공합니다. 추천 수는 효능·안전성 평가가 아닙니다.

제품 상세 하단에서 회원 댓글을 작성·수정·삭제합니다. 의약품 품목번호와 건강기능식품 신고번호는 별도 영역으로 구분해 값이 같아도 댓글을 공유하지 않습니다. 작성자는 세션으로 결정하고, 수정·삭제는 작성자 또는 관리자만 가능합니다. 공식 데이터가 없으면 503, 해당 제품이 없으면 404로 알려줍니다. 본인 댓글 삭제와 추천 취소는 데이터셋에서 제품이 빠진 경우에도 API로 가능합니다.

건강기능식품 추천은 회원당 제품별 한 번이며 같은 요청을 중복·동시 전송해도 한 번만 반영합니다. 추천 취소도 지원합니다. 변경 요청에는 로그인과 CSRF가 필요하며 GET 요청으로 상태를 바꾸지 않습니다. 댓글과 추천은 회원 SQLite DB에 저장되어 재시작해도 유지됩니다. 탈퇴 시 공개 제품 댓글은 ‘탈퇴 회원’으로 남기고 추천은 삭제합니다.

현재 의약품 데이터는 수집한 공식 e약은요 품목 범위입니다. 원본 일반의약품 별도 목록과 전체 수록 범위·유효성분 등의 추가 필드 대조는 아직 남아 있습니다.

### 보건복지부 뉴스

메인 화면의 `/api/news`는 보건복지부 공식 RSS의 제목·발행일·원문 링크 최대 20개를 표시합니다. 별도 API 키나 Selenium, 고정 PC 경로가 필요하지 않습니다. 10분 동안 메모리에 캐시하며 갱신 실패 시 마지막 성공 목록과 지연 안내를 제공합니다. 최초 수집 실패는 503이며, 실패 후 1분 간격으로 다시 시도합니다. 프로세스 재시작 시 캐시는 비워집니다. 기사 본문은 복제하지 않습니다.

출처: [보건복지부 RSS 안내](https://www.mohw.go.kr/menu.es?mid=a10807000000).

### 회원 상태 백업·복원

웹 앱을 먼저 정지하세요. Python 3.11 이상으로 실제 `State__Path`를 지정합니다. Docker 환경에서는 `member-state` 볼륨에 접근할 수 있는 호스트 또는 유지보수 컨테이너에서 실행하며, 그 컨테이너 안의 마운트 경로를 사용합니다.

```text
python tools/state_backup.py backup --state /path/to/state --archive /private-backups/state.zip --app-stopped
python tools/state_backup.py restore --state /path/to/NEW-restored-state --archive /private-backups/state.zip --app-stopped
```

복원 대상 경로는 존재하지 않아야 합니다. 원본 상태를 덮어쓰지 않고 새 경로로 복원한 후 `State__Path` 또는 볼륨 연결을 새 경로로 변경해 실행하세요. Docker 앱 UID 1654가 DB와 keys를 읽고 쓸 수 있도록 복원 디렉터리 소유권도 맞춰야 합니다. 문제 발생 시 앱을 정지하고 원본 경로로 되돌릴 수 있습니다.

백업에는 회원·게시판·댓글·리뷰·문의·DB에 저장된 이미지·인증 키가 포함됩니다. 공공데이터 JSONL, Elasticsearch, `.env`, SMTP/API 비밀, 개발 메일 보관함은 포함하지 않습니다. 이 파일들은 각자의 보관 절차가 필요합니다. 백업 ZIP은 암호화되지 않으므로 접근 제한된 암호화 저장소에 보관하고 공개 배포 ZIP과 분리하세요. 해시는 손상 검사용이며 출처 인증이 아니므로 신뢰하는 백업만 복원해야 합니다. 복원은 ZIP 내부 경로·중복 항목·SHA-256·SQLite 무결성·외래키를 확인합니다. 현재 복원 크기 한도는 압축 해제 기준 4 GB입니다.

2026-10-19 검증: WAL 커밋과 첨부 BLOB을 포함한 자동 왕복 테스트, 잘못된 ZIP 경로 및 손상 거부, 실제 로컬 상태의 별도 경로 복원 후 앱 시작 및 HTTP 200 확인. 실제 회원의 기존 로그인 쿠키 유지 여부는 아직 검증하지 않았습니다.

### 인기 기관 집계

원본 Redis의 상세 조회 횟수 기준을 SQLite `FacilityPopularities`로 이전했습니다. 메인 화면에서 누적 조회 상위 후보 100곳 중 현재 검색 데이터에 남아 있는 기관 최대 5곳을 표시합니다. 삭제된 후보가 많아 5곳 미만으로 표시될 수 있습니다. 집계가 없을 때 임의의 순위를 만들지 않습니다. 조회는 리뷰·상세 페이지를 열 때 CSRF 검증된 POST로 기록하며, 검색 결과를 받는 것만으로 증가하지 않습니다.

동일 접속 IP·기관은 메모리에서 30분 동안 중복 집계를 억제합니다(최대 10,000개 항목). 키는 프로세스마다 무작위 키를 사용한 HMAC이며 원문 IP나 방문자별 이력을 DB에 저장하지 않습니다. 재시작·메모리 캐시 퇴출 후에는 다시 집계될 수 있습니다. 같은 네트워크를 쓰는 사람은 합쳐질 수 있고 프록시 환경은 신뢰 프록시 설정 전까지 같은 접속 주소로 보이므로, 이 수치는 고유 방문자 수나 진료 품질 지표가 아닙니다. 분산 배포에서의 중복 제거도 아직 지원하지 않습니다. 조회 합계 자체는 재시작 및 회원 상태 백업·복원 후에도 유지됩니다.

### 선택 개인정보와 장기 미접속

회원 화면에서 이름·주소·상세 주소·5자리 우편번호·생년월일·성별·좌표·기저질환 메모를 선택 입력할 수 있습니다. `/api/account/details`는 로그인한 본인의 정보만 읽고 수정하며, 일반 계정 응답·관리자 목록·공개 글에 이 정보를 포함하지 않습니다. 모든 선택 개인정보는 Data Protection으로 암호화한 값 하나로 저장합니다. 따라서 `members.db`와 `keys`를 반드시 함께 백업해야 합니다. 키가 없거나 암호문을 읽지 못하면 503으로 응답하고 기존 값을 덮어쓰지 않습니다. 비워서 저장하면 해당 항목이 삭제되며, 탈퇴하면 암호문도 함께 삭제됩니다. 기존 백업에는 과거 값이 남을 수 있으므로 백업 보관 기간도 별도로 관리해야 합니다.

건강정보 저장은 별도 선택 동의가 필요하며 동의 시각을 함께 보관합니다. 동의를 철회해 저장하면 메모와 동의 시각을 삭제합니다. 건강정보를 진단·추천에 사용하지 않습니다. 주소를 외부 지오코딩 서비스에 자동 전송하지 않으며 좌표는 선택 입력입니다.

원본의 6개월 미접속 강제 휴면 차단은 복귀 절차가 없었습니다. 새 버전은 마지막 로그인(없으면 가입 시각)이 UTC 기준 6개월 전보다 오래된 회원을 관리자 필터로 보여줍니다. 미접속 상태가 권한·등급·이용 제한을 변경하지 않으며, 정상 로그인 시 최근 접속으로 돌아옵니다. 회원을 자동 삭제하거나 차단하지 않습니다.

### 카카오·네이버 로그인

기본 비활성화입니다. 운영자 개발자 콘솔에서 앱을 만들고 다음 콜백 URL을 등록한 후 `.env`에 키를 설정하세요. 카카오는 Client Secret 사용을 활성화해야 합니다. 키는 Git이나 공개 소스/데이터 ZIP에 포함하지 않습니다.

```text
SOCIAL_PUBLIC_URL=https://YOUR-DOMAIN
SOCIAL_KAKAO_ENABLED=true
SOCIAL_KAKAO_CLIENT_ID=YOUR-REST-API-KEY
SOCIAL_KAKAO_CLIENT_SECRET=YOUR-CLIENT-SECRET
SOCIAL_NAVER_ENABLED=true
SOCIAL_NAVER_CLIENT_ID=YOUR-CLIENT-ID
SOCIAL_NAVER_CLIENT_SECRET=YOUR-CLIENT-SECRET
```

콜백은 `https://YOUR-DOMAIN/signin-kakao`, `https://YOUR-DOMAIN/signin-naver`입니다. `SOCIAL_PUBLIC_URL`은 경로·쿼리·사용자 정보가 없는 고정 HTTPS origin이어야 합니다. Development에서만 loopback HTTP를 허용합니다. 리다이렉트와 토큰 교환의 콜백 주소는 이 설정을 사용하며 요청 Host로 만들지 않습니다. 두 제공자 모두 앱 키가 설정된 경우에만 버튼을 표시합니다.

기존 회원은 회원 화면에서 현재 비밀번호를 확인하고 소셜 계정을 연결할 수 있습니다. 연결 후 소셜 로그인은 `(제공자, 제공자 회원 ID)`로 해당 회원을 찾습니다. 이메일이 같다는 이유로 계정을 자동 병합하지 않으며 제공자 이메일을 수집하지 않습니다. 처음 이용하는 사용자는 소셜 인증 후 명시적으로 회원가입하고 복구·탈퇴에 사용할 로컬 비밀번호와 이메일을 정합니다. 이메일 인증 필수 설정은 소셜 로그인에도 적용됩니다. 연결 해제도 현재 비밀번호가 필요하며 현재 기기는 세션을 갱신하고 다른 세션은 보안 스탬프 검증에 따라 무효화됩니다. 제공자 측 앱 동의 철회는 각 계정 관리 화면에서 별도로 처리합니다.

인증 요청은 POST·CSRF 검증을 사용하고, 응답의 암호화된 state와 시작 브라우저의 correlation cookie를 검증합니다. 임시 외부 인증 쿠키는 5분 후 만료됩니다. 액세스 토큰·갱신 토큰을 DB나 쿠키에 저장하지 않습니다. 모의 HTTP 제공자를 사용해 실제 OAuth 미들웨어의 코드/토큰/프로필 흐름·잘못된 state·다른 브라우저의 state 재사용·가입·로그인·연결·해제·이용 제한을 검사했습니다. 실제 카카오·네이버 로그인을 검증한 것은 아닙니다. 운영 도메인·앱 키·실제 동의 화면 검증은 아직 필요합니다.

공식 문서: [카카오 REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api), [네이버 로그인 API](https://developers.naver.com/docs/login/api/api.md), [네이버 프로필 API](https://developers.naver.com/docs/login/profile/profile.md).
