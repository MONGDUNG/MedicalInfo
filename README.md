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
