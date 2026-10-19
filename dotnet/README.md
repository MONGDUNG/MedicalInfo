# MedicalInfo .NET 전환

기존 Java 구현을 기능별로 옮기는 작업 브랜치입니다. **아직 전체 서비스의 대체품이나 운영 출시 버전이 아닙니다.**

## 현재 구현과 검증

- ASP.NET Core 병원 키워드 검색, 주변 검색, 진료과 필터 검색
- 원본 `item` 색인을 보존하고 설정한 통합 색인의 대문자 필드와 `geoLocation` 좌표 필드 사용
- 주변 검색은 거리 오름차순 최대 200개. 키워드 검색도 최대 200개이며 페이지 이동은 아직 없음
- 빈 검색어, 좌표·지도 레벨 검증, 요청 제한, 서버 오류 503, 실패한 shard/timeout 거절
- Leaflet 지도·겹친 위치 펼치기·진료시간·리뷰, 회원·관리자·게시판·문의·첨부·제품 정보·댓글·추천·뉴스·인기 기관 구현
- 회귀 검사 및 실제 HTTP 서버 통합 검사: `dotnet run --project dotnet/MedicalInfo.Checks`
- 2026-10-19 로컬 .NET 9 호환 실행에서 실제 데이터 연결 검사 406개, Python 16개, JavaScript 7개와 브라우저 흐름 61개 통과. 기본 운영 target은 .NET 10이며 Windows/Linux·Docker CI를 별도로 실행합니다. [상세 검증 기록](functional-verification-20261019.json)을 참고하세요.

## 집 PC에서 실행

Docker와 Compose가 있으면 Java, H2, .NET SDK를 따로 설치하지 않아도 됩니다.
저장소 루트에서 `.env.example`을 `.env`로 복사하고 Elasticsearch 주소를 설정합니다.

```sh
docker compose up --build -d
docker compose logs -f web
docker compose down
```

브라우저: http://localhost:8080

- `ELASTICSEARCH_URL`: 컨테이너가 접근할 수 있는 서버 주소. Docker Desktop의 호스트 PC에서 실행하는 ES는 `http://host.docker.internal:9200` 사용 가능. Linux에서는 실제 서버 주소 또는 같은 Docker 네트워크의 서비스 이름 사용.
- `ELASTICSEARCH_INDEX`: 기본 `item`.
- `ELASTICSEARCH_API_KEY`: 인증이 필요한 경우 설정. 소스나 이미지에 넣지 않습니다.
- `ALLOWED_HOSTS`: 공개할 때 실제 도메인을 추가합니다(세미콜론 구분).
- `HTTP_BIND_ADDRESS`: 기본 127.0.0.1. 외부 공개 시 방화벽과 HTTPS 프록시 구성을 먼저 정한 뒤 변경합니다.

컨테이너는 비관리자 사용자, 읽기 전용 파일시스템으로 실행하며 회사 PC 경로나 인증 정보에 의존하지 않습니다.
HTTPS는 배포 환경의 프록시에서 종료합니다. 현재 요청 제한은 연결 IP 기준이라 프록시 뒤에서는 프록시 단위로 합산됩니다. 공개 운영 전에 신뢰할 프록시 주소를 명시한 forwarded-header 설정과 제한 정책을 구성해야 합니다.

컨테이너 healthcheck는 프로세스 생존만 확인합니다. 실제 검색 준비 여부는 `/health/ready`가 ES 응답을 검사합니다. 인덱스 데이터 개수나 mapping 호환성까지 보장하지는 않습니다. `/health/live`는 ES 없이도 200입니다.

## SDK로 개발

.NET 10 SDK:

```sh
dotnet run --project dotnet/MedicalInfo.Checks
dotnet run --project dotnet/MedicalInfo.Web --urls http://localhost:8080
```

ES 설정은 환경변수 `Elasticsearch__Url`, `Elasticsearch__Index`, `Elasticsearch__ApiKey`로 덮어씁니다.
로컬에 .NET 9만 있는 동안은 두 명령에 `-p:MedicalInfoFramework=net9.0`을 추가할 수 있습니다. 운영 이미지와 기본 target은 .NET 10이며 이 임시 옵션은 운영 기준이 아닙니다.

## 남은 기능과 데이터

| 기능 | 상태 / 데이터 의존성 |
|---|---|
| 병원·약국 검색·지도 | ES 8.17.3, 통합 시설 148,116건. 업종·진료과·거리순·클러스터와 같은 좌표 개별 선택 구현 |
| 병원 상세·진료시간·응급실 | 제공기관 일반 진료시간 구현. 실시간 응급환자 수용 여부는 제공하지 않음 |
| 회원·관리자·메일·소셜 로그인 | Identity + SQLite 구현. 개발 메일·모의 OAuth 검증 완료, 실제 SMTP·제공자 앱 검증 필요 |
| 게시판·댓글·리뷰·문의 | 작성자/관리자 권한·비공개 열람·CSRF 포함 구현 및 검증 |
| 질병·증상 | KDCA API 승인·이용 범위 확인과 데이터 연결 필요 |
| 의약품·건강기능식품 | 공식 목록·허가 상세·검색·원문 표·제품 댓글·추천 구현 |
| 뉴스·인기 병원 | 공식 RSS·캐시·누적 상세 조회 집계 구현 |
| 첨부파일·백업 | SQLite BLOB·형식/크기/권한 검사, DB·인증 키 백업과 새 경로 복원 검증 |

새 앱에서 미구현 경로는 404입니다. 기존 Java 앱으로 자동 우회하지 않습니다.
현재 회원·작성물 저장소는 SQLite입니다. 공공데이터 설정과 이동용 묶음·운영 준비 상태는 [저장소 README](../README.md)와 [기능 상태](migration-status.json)를 기준으로 확인하세요.

## 기존 소스 점검 결과

- `SecurityConfig`: `/member/admin` 한 경로만 관리자 제한, 하위 경로와 회원 변경 API 보호 부족. 회원 경로의 CSRF 예외도 재검토 필요.
- `ReviewController` / `ReviewService`: 리뷰 수정·삭제에 현재 사용자와 작성자 비교 없음.
- 여러 삭제 작업이 GET 요청을 사용함. 이식 시 인증된 POST/DELETE와 CSRF 검증 사용.
- `PostService`: classpath 정적 폴더와 `c:/upload` 의존. 배포 환경에서 영속성 보장 불가.
- `News_crawler.py`: 노트북 명령과 특정 사용자 절대 경로가 남아 있어 독립 실행 불가.
- 기존 `application.properties`의 노출된 인증 정보는 환경변수 참조로 교체. Git 이력에 남은 값의 폐기/재발급은 외부 서비스에서 별도로 해야 함.

## 실제 데이터가 필요한 검증

다운로드한 파일이 원본 CSV/JSON인지, Elasticsearch snapshot인지, 노드 data 폴더인지 먼저 확인합니다. 원본은 보존하고 복사본으로 복원합니다. ES 버전이 확인되기 전에는 최신 버전 이미지에 기존 data 폴더를 바로 연결하지 않습니다.
복원 이후 `item` mapping의 `geoLocation: geo_point`, 진료과의 scalar/array 형식, 좌표, 검색 건수와 병원 식별자를 확인해야 합니다. 테스트 fixture는 가상 데이터이며 실제 의료 데이터 검증을 대신하지 않습니다.

2026-10-15 검증: 보관된 ES 8.17.3 실행 폴더의 복사본에서 `item` 103,179건을 조회했습니다. nori 분석기, `geoLocation: geo_point`, 문자열 `DEPT_NAME`을 확인했고 geoLocation 누락 문서는 0건입니다. 한국어 키워드, 한의원 제외 조건, 주변 약국, 반경/거리순 정렬, 진료과 필터 검사를 통과했습니다. 기존 28개 검사와 합계 33개이며 로컬 .NET 9 호환 실행 기준입니다. 데이터의 현재 영업 여부나 의료정보 정확성을 검증한 것은 아닙니다.

실제 데이터 검사는 선택적으로 실행합니다(기본 CI에는 의료 데이터가 필요하지 않음).

```powershell
$env:MEDICALINFO_LIVE_ES_URL = 'http://127.0.0.1:19200'
dotnet run --project dotnet/MedicalInfo.Checks
```

필요하면 `MEDICALINFO_LIVE_ES_INDEX`, `MEDICALINFO_LIVE_ES_API_KEY`를 설정합니다. 실제 데이터 검사는 조회만 수행합니다. 스냅샷 이동 방법은 [복원 안내](RESTORE-SEARCH.md)를 참고하세요.
