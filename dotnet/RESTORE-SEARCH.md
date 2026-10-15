# 집 PC로 검색 데이터 이동

## 검증된 백업

- Elasticsearch 원본 버전: 8.17.3, analysis-nori 플러그인 포함
- 인덱스: `item`, 문서 103,179건
- 스냅샷: `item-20261015`, 글로벌 상태 제외
- 파일: `medicalinfo-item-snapshot-20261015.zip` (18,352,999 bytes)
- 생성 결과: SUCCESS, 실패 shard 0
- 같은 복원용 서버의 별도 인덱스 `item-restore-check-20261015`로 복원 후 103,179건 확인
- 새 PC/새 컨테이너에서의 복원은 아직 검증하지 않음

원본 실행 폴더 ZIP은 보관용으로 유지합니다. 앞으로의 이전에는 정식 snapshot을 사용합니다. ZIP과 `snapshot-sha256.json`을 함께 집 PC로 복사하세요. 백업 데이터는 GitHub에 업로드하지 않았습니다.

## 집 PC에서 복원

1. Elasticsearch 8.17.3과 analysis-nori 플러그인을 준비합니다. 첫 복원은 같은 버전을 사용하고, 업그레이드는 별도 작업으로 합니다.
2. 스냅샷 ZIP을 폴더 하나에 압축 해제합니다. 그 폴더 바로 아래에 `index-0`, `index.latest`, `indices` 등이 있어야 합니다.
3. `elasticsearch.yml`의 `path.repo`에 해당 스냅샷 폴더의 **부모 경로**를 등록하고 서버를 시작합니다. Docker라면 폴더를 컨테이너에 마운트하고 컨테이너 내부 경로를 설정합니다.
4. 아래 예제에서 `$repositoryPath`를 ES 서버가 볼 수 있는 압축 해제 폴더 경로로 바꿉니다. 기존 `item` 인덱스가 있는 서버에는 덮어쓰지 말고 별도 서버 또는 다른 인덱스 이름으로 복원하세요.

```powershell
$base = 'http://127.0.0.1:9200'
$repositoryPath = 'D:/MedicalInfo/backups/medicalinfo-portable'
$repository = @{ type='fs'; settings=@{ location=$repositoryPath; readonly=$true } }
Invoke-RestMethod "$base/_snapshot/medicalinfo_portable" -Method Put `
  -ContentType 'application/json' -Body ($repository | ConvertTo-Json -Depth 5)

Invoke-RestMethod "$base/_snapshot/medicalinfo_portable/item-20261015"
$restore = @{ indices='item'; include_global_state=$false; include_aliases=$false }
Invoke-RestMethod "$base/_snapshot/medicalinfo_portable/item-20261015/_restore?wait_for_completion=true" `
  -Method Post -ContentType 'application/json' -Body ($restore | ConvertTo-Json)
Invoke-RestMethod "$base/item/_count"
```

인증을 켠 서버에서는 API 키 등 해당 서버 인증 헤더를 추가해야 합니다. 개발용 무인증 서버는 루프백 주소에만 바인딩합니다.

단일 노드에서는 replica를 배치할 두 번째 노드가 없어서 yellow일 수 있습니다. 검증 서버도 primary 1개가 정상이고 replica 1개만 미할당이었습니다. 이는 primary 손실 상태와 구분해야 합니다. 운영 replica/백업 정책은 배포 환경에 맞춰 별도 구성합니다.

## 앱 연결

저장소의 `.env`에 `ELASTICSEARCH_URL`, `ELASTICSEARCH_INDEX=item`을 설정하고 Docker Compose로 앱을 실행합니다. 앱 컨테이너의 localhost는 호스트 PC가 아니므로 Docker Desktop에서는 호스트 ES 주소로 `http://host.docker.internal:9200` 등을 사용합니다.

이 스냅샷은 검색 인덱스만 포함합니다. 회원·게시판·병원 상세·진료시간 등 관계형 DB를 복원하지는 않습니다. 데이터의 수집 시점과 이용 허락, 실제 병원 운영 정보는 공개 운영 전에 별도로 확인해야 합니다.
