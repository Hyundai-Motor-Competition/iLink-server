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
| `JWT_SECRET` | 필수 | 필수 |

`JWT_SECRET`은 access 토큰 서명 키(HS256)다. 32바이트 이상 임의 값을 Base64로 인코딩해 넣는다. 예: `openssl rand -base64 32`. 테스트와 `bootTestRun`은 실행할 때 임의 키를 만들어 쓰므로 넣지 않아도 된다.

Firebase 키 · 전화번호 암호화 키는 해당 도메인을 만들 때 여기에 더한다.

## dev DB 띄우기

```bash
docker run -d --name pickup-dev-db -p 5432:5432 \
  -e POSTGRES_DB=pickup -e POSTGRES_USER=pickup -e POSTGRES_PASSWORD=<비밀번호> \
  postgis/postgis:17-3.7
```

dev는 `ddl-auto: update`라 컬럼 삭제 · NOT NULL 해제 같은 변경은 자동으로 반영되지 않는다. 그런 변경이 들어오면 dev DB 컨테이너를 지우고 다시 띄운다(`docker rm -f pickup-dev-db` 후 위 명령). 예: `idempotency_key.response_status`가 null 허용으로 바뀌었으므로 그 전에 만든 dev DB는 다시 띄우거나 `ALTER TABLE idempotency_key ALTER COLUMN response_status DROP NOT NULL;`을 실행한다.

## 실행

```bash
SPRING_PROFILES_ACTIVE=dev DB_PASSWORD=<비밀번호> JWT_SECRET=<Base64 키> ./gradlew bootRun
```

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; $env:DB_PASSWORD="<비밀번호>"; $env:JWT_SECRET="<Base64 키>"; .\gradlew.bat bootRun
```

DB를 따로 띄우지 않고 Testcontainers PostGIS로 실행: `./gradlew bootTestRun` (dev 프로필로 뜬다)

## 운영(prod) 배포 순서

prod는 `ddl-auto: validate`라 서버가 테이블을 만들지 않는다. 그런데 `spring.sql.init.mode: always`라 서버가 뜰 때 `src/main/resources/schema.sql`(유니크 인덱스 등)을 실행하므로, 그 대상 테이블이 먼저 있어야 한다.

1. 새 테이블 · 컬럼 DDL을 DB에 직접 실행한다(예: `idempotency_key` 테이블). 컬럼 정의는 `docs/03_공유모듈-shared.md` 「테이블」 절과 각 도메인 설계서를 따른다.
2. `schema.sql`의 인덱스는 `IF NOT EXISTS`라 서버가 뜰 때 자동으로 만들어진다. 미리 실행해도 된다.
3. 환경 변수 `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`을 넣고 서버를 띄운다.

## dev 서버 · CI/CD (홈서버 Jenkins + k3s)

`main`에 push하면 Jenkins 잡 `ilink-deploy`가 `Jenkinsfile`대로 돈다: `./gradlew test` → `bootJar` → 이미지 `harbor.chisanw.com/ilink/api:{커밋 12자리}` 푸시 → k3s `ilink-dev` 네임스페이스에 배포. 롤아웃이 실패하면 직전 버전으로 되돌린다.

| 항목 | 값 |
|---|---|
| 주소 | `https://ilink-api-dev.chisanw.com` (Cloudflare Tunnel → 홈서버 nginx → ingress-nginx) |
| 매니페스트 | `ops/k8s/dev/api.yaml`(파이프라인이 매번 적용), `postgres.yaml` · `deployer.yaml`(운영자가 한 번 적용) |
| DB | `ilink-dev`의 StatefulSet `postgres`(`postgis/postgis:17-3.7`), DB · 사용자 `pickup` |
| 비밀값 | Secret `api`(`DB_PASSWORD` · `JWT_SECRET` · `PHONE_ENCRYPTION_KEY` · `KAKAO_REST_API_KEY`). 홈서버에서 `ilink-secrets`로 편집 · 반영(`ops/scripts/ilink-secrets.sh`) |
| Jenkins credentials | `ilink-harbor`(Harbor 로봇 계정), `ilink-kubeconfig`(`ilink-deployer` 토큰) |

ConfigMap(비밀이 아닌 환경 변수)을 바꾸려면 `ops/k8s/dev/api.yaml`을 고쳐 `main`에 올린다. 파드가 자동으로 다시 뜬다.

## 테스트

```bash
./gradlew test
```

DB가 닿는 테스트는 공통 기반 클래스 `common.fixture.IntegrationTestSupport`를 상속한다. 이 클래스가 `@SpringBootTest` · `@ActiveProfiles("dev")` · `PostgisTestcontainersConfiguration`(`postgis/postgis:17-3.7` 컨테이너, 실행마다 임의 `pickup.jwt.secret`) · `AccessTokenFixture`(테스트용 access 토큰)를 묶는다.

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
