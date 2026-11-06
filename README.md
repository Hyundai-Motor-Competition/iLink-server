# pickup-service

차량 IVI · 보호자 앱 · 자녀 앱이 쓰는 픽업 서비스 백엔드. 설계서는 `docs/`에 있고, 공통 규칙은 `docs/02_공통-규칙-global.md`가 기준이다.

## 기술 스택 (고정 버전)

| 구분 | 버전 |
|---|---|
| Java | 21 (Gradle toolchain) |
| Spring Boot | 4.1.2 (Spring Framework 7.0.10, Spring Security 7.1.2) |
| Gradle | 9.8.2 (wrapper) |
| Hibernate ORM · Hibernate Spatial | 7.4.13.Final |
| PostgreSQL + PostGIS | 17 + 3.7 (`postgis/postgis:17-3.7`) |
| PostgreSQL JDBC | 42.7.14 |
| springdoc-openapi | 3.2.0 |
| Firebase Admin SDK | 9.11.0 |
| Lombok | 1.18.48 |
| Testcontainers | 2.0.5 |
| JUnit Jupiter | 6.0.3 (Spring Boot 4 기본) |

## 준비물

- JDK 21
- Docker (테스트와 dev DB에 필요)

## 환경 변수

비밀값은 설정 파일에 넣지 않고 환경 변수로만 넣는다.

| 이름 | dev | prod |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `prod` |
| `DB_URL` | 없으면 `jdbc:postgresql://localhost:5432/pickup` | 필수 |
| `DB_USERNAME` | 없으면 `pickup` | 필수 |
| `DB_PASSWORD` | 필수 | 필수 |

JWT 키 · Firebase 키 · 전화번호 암호화 키는 해당 도메인을 만들 때 여기에 더한다.

## dev DB 띄우기

```bash
docker run -d --name pickup-dev-db -p 5432:5432 \
  -e POSTGRES_DB=pickup -e POSTGRES_USER=pickup -e POSTGRES_PASSWORD=<비밀번호> \
  postgis/postgis:17-3.7
```

## 실행

```bash
SPRING_PROFILES_ACTIVE=dev DB_PASSWORD=<비밀번호> ./gradlew bootRun
```

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; $env:DB_PASSWORD="<비밀번호>"; .\gradlew.bat bootRun
```

DB를 따로 띄우지 않고 Testcontainers PostGIS로 실행: `./gradlew bootTestRun` (dev 프로필로 뜬다)

## 테스트

```bash
./gradlew test
```

DB가 닿는 테스트는 `common.fixture.PostgisTestcontainersConfiguration`을 `@Import`해 `postgis/postgis:17-3.7` 컨테이너로 돌린다. 테스트는 `@ActiveProfiles("dev")`로 dev 설정을 쓴다.

## 상태 확인 · API 문서

| 경로 | 설명 |
|---|---|
| `GET /actuator/health` | 헬스 체크. 인증 없이 열림 |
| `/swagger-ui.html`, `/v3/api-docs` | dev에서만 켬. 보안 설정에서 열어 주는 작업은 common(03_공유모듈-shared.md)에서 한다 |

## 패키지

기본 패키지는 `com.example.pickupservice`. 도메인(user · auth · schedule · place · pickup · notification)마다 아래 모양을 갖고, 여러 도메인이 쓰는 것은 `common`에 둔다.

```text
{도메인}
├─ adapter/in/web (+ doc)   컨트롤러, 요청 · 응답 DTO, 스웨거 doc 인터페이스
├─ adapter/out/persistence  JPA 엔티티, 저장소, 도메인 ↔ 엔티티 변환
├─ application/port/in · out
├─ application/service
└─ domain
```

도메인별 추가 패키지(sse · scheduler · event · fcm · map · token)는 각 도메인 설계서의 「구조」를 따른다. 의존 방향(adapter → application → domain)은 코드 리뷰로 확인하고 ArchUnit 같은 자동 검사는 쓰지 않는다.
