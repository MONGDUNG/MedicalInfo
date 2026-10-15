# MedicalInfo .NET 전환

기존 Java 구현을 기능별로 옮기는 작업 브랜치입니다. **아직 전체 서비스의 대체품이나 운영 출시 버전이 아닙니다.**

## 현재 구현과 검증

- ASP.NET Core 병원 키워드 검색, 주변 검색, 진료과 필터 검색
- 기존 Elasticsearch `item` 인덱스의 대문자 필드와 `geoLocation` 좌표 필드 사용
- 주변 검색은 거리 오름차순 최대 200개. 키워드 검색도 최대 200개이며 페이지 이동은 아직 없음
- 빈 검색어, 좌표·지도 레벨 검증, 요청 제한, 서버 오류 503, 실패한 shard/timeout 거절
- 기본 검색 화면. 지도 SDK 및 병원 상세 화면은 아직 미이식
- 회귀 검사 및 실제 HTTP 서버 통합 검사: `dotnet run --project dotnet/MedicalInfo.Checks`
- 회사 PC에서는 .NET 9 임시 호환 실행으로 28개 검사 통과. GitHub CI에서도 .NET 10 Windows/Linux 검사 및 Docker 이미지 빌드 통과. 컨테이너 HTTP smoke 검사도 CI에 포함

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
| 병원·약국 검색 | 이식, 실제 ES mapping/문서/버전 통합 검증 대기 |
| 지도 SDK | JavaScript 키·허용 도메인 설정 및 화면 이식 필요 |
| 병원 상세·진료시간·응급실 | H2의 MEDINST/MEDINFO/Emergency 등 기초 데이터도 필요 |
| 회원·관리자·메일·소셜 로그인 | ASP.NET Core Identity + PostgreSQL 이식 예정. 현재 새 앱에는 인증 기능 없음 |
| 게시판·댓글·리뷰 | 작성자/관리자 권한 및 CSRF 검증을 포함해 이식 필요 |
| 질병·증상·의약품·건강기능식품 | 관계형 DB 모델, 출처 확인 및 재적재 필요 |
| 뉴스·인기 병원 | 수집 작업과 집계 로직 이식 필요 |
| 첨부파일 | 외부 영속 저장소, 크기/형식/소유권 검증 필요 |

새 앱에서 미구현 경로는 404입니다. 기존 Java 앱으로 자동 우회하지 않습니다.
PostgreSQL은 아직 연결하거나 데이터 스키마를 생성하지 않았습니다.

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
