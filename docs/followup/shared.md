# FOLLOWUP · 공유모듈 (shared)

- 생성: 2026-11-11 11:48 KST · tdd-guard
- 상태: 진행 중 (체크 57/57 · 완료 기준 0/4)
- 테스트: ● GREEN · 275 passed · *, RealServerStartupTest · 165s · .\gradlew.bat test --tests com.example.pickupservice.common.* --tests com.example.pickupservice.RealServerStartupTest --console=plain
- 설계서: C:/ilink/docs/03_공유모듈-shared.md

## 완료 항목
- [x] #1 [기반] BaseTimeEntity를 상속한 엔티티를 저장하면 createdAt과 updatedAt이 채워진다
- [x] #2 [기반] BaseTimeEntity를 상속한 엔티티를 수정해 저장하면 updatedAt만 바뀌고 createdAt은 그대로다
- [x] #3 [기반] TimeUtils.toKst는 UTC 2026-11-03T09:12:00Z를 2026-11-03T18:12:00+09:00으로 바꾼다
- [x] #4 [기반] TimeUtils.toUtc는 2026-11-03T18:12:00+09:00을 UTC 2026-11-03T09:12:00Z로 바꾼다
- [x] #5 [기반] TimeUtils.todayKst는 UTC 2026-11-05T15:30:00Z 시점에 한국 날짜 2026-11-06을 돌려준다
- [x] #6 [기반] 서버가 뜨면 JVM 기본 시간대와 DB 연결 시간대가 UTC다
- [x] #7 [기반] GeoPoints.toPoint는 lat 37.501234 · lng 127.039876을 SRID 4326 Point로 바꾸고 toLatLng로 같은 값을 돌려준다
- [x] #8 [기반] 공통 테스트 기반 클래스는 Testcontainers postgis/postgis 컨테이너에서 geography(Point,4326) 컬럼을 만들 수 있다
- [x] #9 [기반] 공통 테스트 픽스처로 VEHICLE 주체와 USER 주체(familyId · role 지정)의 테스트용 access 토큰을 만들 수 있다
- [x] #10 [기반] 비동기 설정은 @EnableAsync와 이벤트 리스너용 스레드 풀 빈을 등록한다
- [x] #11 [web] JSON 형식이 틀린 본문을 보내면 400 INVALID_REQUEST를 돌려준다
- [x] #12 [web] Bean Validation에 실패하면 422 VALIDATION_FAILED와 fields[].field · fields[].reason을 돌려준다
- [x] #13 [web] 모든 오류 응답 본문은 error.code · error.message · error.retryAfterSeconds 키를 담는다
- [x] #14 [web] 공통 예외에 retryAfterSeconds 30을 담아 던지면 429 응답의 error.retryAfterSeconds가 30이다
- [x] #15 [web] Authorization 헤더 없이 인증이 필요한 API를 부르면 401 UNAUTHORIZED를 돌려준다
- [x] #16 [web] 만료된 access 토큰으로 부르면 401 UNAUTHORIZED를 돌려준다
- [x] #17 [web] 서명이 틀린 토큰으로 부르면 401 UNAUTHORIZED를 돌려준다
- [x] #18 [web] 컨트롤러는 토큰의 주체 종류 · 주체 id · familyId · role을 AuthPrincipal로 받는다
- [x] #19 [web] 허용되지 않은 role로 부르면 403 FORBIDDEN을 돌려준다
- [x] #20 [persistence] idempotency_key에 키 · 요청 해시 · 응답 본문을 저장하고 키로 다시 읽는다
- [x] #21 [web] Idempotency-Key 대상 API에 같은 키 · 같은 본문을 두 번 보내면 두 번째는 첫 응답과 같은 상태 코드 · 본문을 돌려준다
- [x] #22 [web] Idempotency-Key 대상 API에 같은 키로 두 번째 요청이 오면 유스케이스는 한 번만 실행된다
- [x] #23 [web] 같은 Idempotency-Key로 다른 본문을 보내면 409 IDEMPOTENCY_KEY_REUSED를 돌려준다
- [x] #24 [web] Idempotency-Key 대상 API를 헤더 없이 부르면 거절된다
- [x] #25 [web] Idempotency-Key 대상이 아닌 API는 헤더 없이도 처리된다
- [x] #26 [서비스] 만료 키 삭제 작업은 저장한 지 24시간이 지난 idempotency_key 행만 지운다
- [x] #27 [web] 요청 로깅 AOP는 컨트롤러 요청마다 requestId · 호출 주체 · HTTP 메서드 · 경로 · 응답 코드 · 걸린 시간을 한 줄로 남긴다
- [x] #28 [web] 요청 로깅 AOP는 예외로 끝난 요청의 로그에 오류 코드를 남긴다
- [x] #29 [web] 요청 로깅 AOP는 요청 본문의 전화번호 · 위치 · 토큰 · 연결 코드 값을 로그에 그대로 남기지 않는다
- [x] #30 [web] 요청 로깅 AOP는 SSE 스트림 요청을 연결 때와 종료 때만 남긴다
- [x] #31 [web] 요청 로깅 AOP는 GET /v1/vehicle-link-codes/{codeId}/status 폴링을 응답 status가 직전과 다를 때만 남긴다
- [x] #32 [web] dev 프로필에서는 springdoc API 문서가 열린다
- [x] #33 [web] prod 프로필에서는 springdoc API 문서가 꺼져 있다
- [x] #34 [web] 컨트롤러 파라미터 제약과 @Valid 본문이 함께 있어 메서드 검증으로 처리될 때도 본문 필드 오류의 fields[].field는 본문 필드 이름이다
- [x] #35 [web] @RequestParam · @RequestHeader 값 검증 실패의 fields[].field는 애너테이션에 적은 이름(예: Idempotency-Key)이다
- [x] #36 [web] 따로 처리하지 않은 Spring MVC 요청 예외(빠진 요청 헤더 · 받을 수 없는 Accept · ResponseStatusException)는 500이 아니라 그 예외의 4xx 상태와 공통 오류 본문으로 돌려준다
- [x] #37 [web] 서명은 맞지만 주체 클레임이 빠졌거나 틀린 토큰(principalType 없음 · 숫자가 아닌 sub · 없는 role 값)으로 부르면 500이 아니라 401 UNAUTHORIZED를 돌려준다
- [x] #38 [web] Accept application/xml로 불러도 성공 · 오류 응답은 XML이 아니라 JSON이다(의존성으로 들어온 jackson-dataformat-xml 컨버터를 쓰지 않음)
- [x] #39 [web] ResponseStatusException에 reason을 담아 던지면 그 reason이 error.message로 나간다
- [x] #40 [web] familyId 클레임이 없는 VEHICLE access 토큰으로 부르면 401이 아니라 AuthPrincipal.familyId가 null로 들어온다
- [x] #41 [web] 스웨거 doc 인터페이스를 구현한 컨트롤러의 파라미터에 검증 제약(@Positive 등)을 달아도 500(HV000151)이 아니라 422 VALIDATION_FAILED를 돌려준다
- [x] #42 [web] 인증 없이 부르는 Idempotency-Key 대상 API도 같은 키 · 같은 본문의 두 번째 요청에 첫 응답과 같은 상태 코드 · 본문을 돌려준다
- [x] #43 [통합] 실제 웹 서버(RANDOM_PORT)로 애플리케이션 컨텍스트가 뜨고 GET /actuator/health가 200 UP이다
- [x] #44 [web] 같은 주체가 같은 Idempotency-Key로 두 요청을 동시에 보내면 유스케이스는 한 번만 실행되고 나머지 요청은 409 IDEMPOTENCY_KEY_REUSED를 받는다
- [x] #45 [web] Idempotency-Key 예약 저장에서 유니크 위반이 아닌 DB 오류(NOT NULL 위반 등)가 나면 409 IDEMPOTENCY_KEY_REUSED가 아니라 500 공통 오류로 끝난다
- [x] #46 [web] Idempotency-Key 대상 API가 4xx · 5xx로 끝나면 키를 저장하지 않아 같은 키 · 같은 본문으로 다시 보내면 유스케이스가 다시 실행된다
- [x] #47 [web] 요청 로그의 requestId는 안전한 X-Request-Id 헤더 값(영숫자 · - · _ 64자 이하)을 그대로 쓰고 헤더가 없거나 그 밖의 값이면 새로 만든다
- [x] #48 [web] @ResponseStatus(CREATED)를 단 컨트롤러 메서드의 요청 로그 status는 200이 아니라 201이다
- [x] #49 [web] @PreAuthorize 권한 거절로 403이 난 요청의 로그는 status=200이 아니라 status=403 errorCode=FORBIDDEN이다
- [x] #50 [web] 요청 로그의 path는 실제 경로 변수 값이 아니라 매핑 패턴(예: /v1/invites/{token})으로 남아 토큰 · 연결 코드 값이 로그에 없다
- [x] #51 [web] 같은 요청 동안 남는 모든 로그 줄(500 처리기 error 로그 포함)에 requestId가 찍힌다
- [x] #52 [web] exp 클레임이 없는 access 토큰은 서버 키로 서명돼 있어도 401 UNAUTHORIZED를 받는다
- [x] #53 [web] Idempotency-Key 예약 단계에서 DataAccessException이 아닌 DB 오류(트랜잭션 시작 실패 등)가 나도 500 공통 오류 본문으로 끝난다
- [x] #54 [web] DB 제약 위반이 나도 로그에 PostgreSQL 오류 상세(Failing row contains 행 값)가 남지 않는다
- [x] #55 [web] 각 도메인이 PublicEndpoints 빈으로 등록한 메서드 · 경로는 토큰 없이 부를 수 있고 만료된 Bearer 토큰을 붙여도 401이 아니라 그대로 처리된다
- [x] #56 [web] dev API 문서(/v3/api-docs)는 Bearer JWT 보안 스키마를 담는다
- [x] #57 [web] 공통 오류 코드(ErrorCode)로 BusinessException을 만들어 던지면 그 코드의 HTTP 상태 · code · 문구로 응답한다(429는 retryAfterSeconds 포함)

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [ ] C1 설계서의 테스트 케이스가 모두 GREEN
- [ ] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [ ] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [ ] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-11 11:48 KST · 도메인 리뷰어 · 결론 조건부 통과 · 상 0 · 중 8 · 하 22 · 리뷰 시점 체크 57/57
- 리뷰 시점 테스트: ● GREEN · 275 passed · *, RealServerStartupTest · 165s · .\gradlew.bat test --tests com.example.pickupservice.common.* --tests com.example.pickupservice.RealServerStartupTest --console=plain
  - 결론: 조건부 통과
  - [중] 필터 단계에서 끝난 요청은 요청 로그 줄이 남지 않음(Idempotency 재응답 · 409 · 422 키 없음 · 예약 500 · Security 401). 지난 리뷰의 [중]이고 "05 시작 전 사람 결정"이었는데 05 · 08 · 09가 끝난 지금도 결정 기록이 없음 — IdempotencyKeyFilter.java:57-60 · 73-96(체인을 부르지 않고 끝남), RequestLoggingConfig.java:12(인터셉터는 DispatcherServlet 안에서만 돎), 02 「로그」 203행 "요청 하나에 INFO 한 줄", FOLLOWUP shared.md:183 (2) → 사람 결정
  - [중] 5xx BusinessException이 서버 로그에 아무것도 남기지 않음. 이제 실제 사용처가 생김: SearchAcademyService.java:24-25가 예상 못 한 RuntimeException을 ScheduleErrors.java:13-17(502, initCause)로 바꾸는데, GlobalExceptionHandler.java:74-77이 로그 없이 응답만 써서 원인 스택이 사라짐. 03 102행 "예외 내용은 서버 로그에만"과 어긋남 → 항목 추가(5xx BusinessException은 원인과 함께 ERROR 로그)
  - [중] 스케줄러 테스트 격리가 03 205행 결정과 다름. TestPostgisConfiguration.java:71-74는 idempotency cleanup만 P1D로 둠. AutoCompleteBoardingScheduler.java:38(PT10S)은 테스트에서도 돎(AutoCompleteBoardingSchedulerTest.java:119가 10초를 단언함). ResendUndeliveredScheduler.java:17은 fixedDelay=60_000L로 고정돼 설정으로 끌 수 없음. 둘 다 컨텍스트마다 기동 직후 공유 테스트 DB에서 실행되고, IdempotencyKeyCleanerTest.java:116-124처럼 고정 Clock(2026-01-15)을 쓰는 컨텍스트에서도 그 시계로 돎 …
  - [중] @Scheduled 실행 스레드가 1개인데 작업은 4개(만료 키 삭제 · 10초 자동 완료 · 60초 재발송 · 30초 SSE ping). ResendUndeliveredService.java:83이 FCM을 같은 스레드에서 동기로 보내므로 자동 완료(AutoCompleteBoardingScheduler.java:38)와 ping(SessionStreamRegistry.java:86)이 늦어질 수 있음. application.yml에 spring.task.scheduling.pool.size 없음. 03 확인 필요 341행 "08 시작 때 정함"은 아직 미결 → 사람 결정
  - [중] 03의 오류 형식 · ErrorCode · BusinessException 절이 코드와 다름. 코드에만 있는 것: tooManyRequests(ErrorCode,int)(BusinessException.java:47), invalidField(52), stateVersionConflict(64), withDetail · details(70-80), 오류 본문 추가 키 · 최상위 session(ErrorResponse.java:13 · 41-46), ErrorCode의 SESSION_ALREADY_FINISHED · CALL_ALREADY_REVOKED · RESEND_TOO_SOON(ErrorCode.java:21-22 · 25). RESEND_TOO_SOON은 notification만 써서 03 …
  - [중] 도메인 429 응답 모양이 네 가지임: AuthErrors.java:26 · 41(retryAfterSeconds 없음), PickupErrors.java:88-90(retryAfterSeconds 없이 추가 키 nextAllowedAt), ResendNotificationService.java:61(ErrorCode + retryAfterSeconds), LinkStatusPollGuard.java:37(TOO_MANY_REQUESTS + retryAfterSeconds). 02 183행은 "429는 retryAfterSeconds에 다시 시도할 초"를 요구함. 03 340행 "auth 전에 정함"은 미결 → 사람 결정(도메인 429 팩토리 하나로)
  - [중] 마감이 지난 열린 질문이 03에 그대로 남음: access 토큰 폐기(02 174행 "폐기면 401" vs 05 315행 "만료까지 유효"), 주체 종류별 필수 클레임(03 327행), JWT 키 길이 검사(03 328행), 인증 없는 공용 Idempotency 공간(03 · 05 321행), /error permitAll(03 323행) → 사람 결정
  - [하] IdempotencyKeyEntity 6인자 생성자가 남음(IdempotencyKeyEntity.java:45-53, 테스트 9곳). 팀장 결정 "다음 shared 정리 때 삭제"가 아직 안 됨 → 리팩터링
  - [하] 3a 응답 저장 실패 경로 테스트가 없음. release가 실패하면 원래 예외가 덮이고, 필터 밖으로 나간 예외 로그에는 requestId가 없음(IdempotencyKeyFilter.java:62-70) → note
  - [하] 만료 삭제 경계 "정확히 24시간인 행은 남김"(03 169행) 테스트가 없음. 25시간 · 23시간만 확인함(IdempotencyKeyCleanerTest.java:25-27) → 항목 추가
  - [하] withDetail 예약 키에 overlapScheduleIds가 없어 같은 키가 JSON에 두 번 나갈 수 있음(BusinessException.java:14 vs ErrorResponse.java:40) → 리팩터링
  - [하] 03 구조 표 누락: common.text(NameRule · PlateNumberMasker) 패키지 전체, PollStateCaptureAdvice · PollStateStore · RequestLoggingConfig · RequestLogAttributes, ErrorCodeAttribute, IdempotencyKeyStore · IdempotencyKeyRequiredMatcher · RequestHasher, CurrentAuthPrincipal, JpaAuditingConfig. 02 11행 책임 목록도 같음 → note
  - [하] common/text 테스트가 user · auth 소스 파일을 글자로 훑는 구조 검사임(NameRuleTest.java:110-126, PlateNumberMaskerTest.java:50-64). 02 80 · 214행 "아키텍처 규칙은 테스트로 하지 않는다"와 어긋나고, user · auth 파일 이름을 바꾸면 common 테스트가 깨짐 → 사람 결정(삭제 또는 해당 도메인으로 이동)
  - [하] 테스트 코드 9개 파일에 Javadoc · 설명 주석이 있음(TestPostgisConfiguration.java:11-14, TestPostgis.java:6-14, EmbeddedPostgis.java:26-29, PostgisContainer.java:12-16, GeoOffsets.java:5-16 등). 02 78행 위반 → 리팩터링
  - [하] toKst(null)을 호출 쪽에서 막는 코드가 3곳에 중복됨(KstTime.java:13, NotificationItemResponse.java:31-33, ReceiveVehicleEventService.java:66). Instant를 KST 날짜로 바꾸는 코드도 중복됨(CorrectOutcomeService.java:25 · 38) → 사람 결정(TimeUtils에 null 허용 변환 · KST 날짜 변환 추가)
  - [하] JWT 키 만드는 코드가 3곳(JwtDecoderConfig.java:25, JwtTokenIssuer.java:40, AccessTokenFixture.java:100)이고 클레임 이름은 문자열로 중복됨(JwtTokenIssuer.java:68-76). auth가 더한 deviceId 클레임이 03 122행 클레임 표에 없고, CurrentDevice.java:10-17은 AuthPrincipal이 아니라 Jwt를 직접 읽음 → note(03 표 갱신) · 리팩터링
  - [하] 배포가 JVM에 TZ=Asia/Seoul을 줌(Dockerfile:16, ops/k8s/dev/api.yaml:14). 03 343행의 대안 "배포 때 TZ=UTC"와 반대 방향이라 TimeZoneConfig 빈 순서에 기댐. #6 테스트가 KST로 뜬 뒤 UTC로 바뀌는 것은 확인함 → 사람 결정
  - [하] 좌표 6자리 반올림 가정(03 316행)이 구현되지 않음. GeoPoints.java:16-18은 값을 그대로 쓰고 반올림 코드가 0건임 → 사람 결정
  - [하] 실제로는 정해졌는데 03에 반영되지 않은 것: 스웨거 자물쇠(auth doc 5곳의 @SecurityRequirements), 도메인 ResponseStatusException 사용 0건, SSE 타임아웃 없음(SessionStreamRegistry.java:32 NO_TIMEOUT), SIG-02 eventId(08 420행) → note
  - [하] 진행 기록이 서로 맞지 않음: FOLLOWUP shared.md:4-6은 "완료 기준 4/4 · C:/pickup 경로 · 191 passed"인데 TODO.md:12-20 · REVIEW.md:8은 "리뷰 없음 · 0/4". #8 문장은 "Testcontainers 컨테이너에서"인데 Windows에서는 내장 서버로 돎(TestPostgis.java:48). 증거 인용 #14 · #31이 지금 테스트 이름과 다름 → note
  - 지난 리뷰(2026-11-07 15:37) 지적 확인
  - [중] 필터 단계 요청 로그 — 남음(위 첫 [중])
  - [하] 증거 인용 #14 · #31 — 남음(note)
  - [하] FOLLOWUP 설계서 사본 — 남음(shared.md:96-135에 conflictWithOverlap 행 없음)
  - [하] IdempotencyKeyEntity 6인자 — 남음
  - [하] 5xx 로그 · null 방어 — 남음, 502 사용처가 생겨 [중]으로 올림
  - [하] 3a 테스트 — 남음
  - [하] ErrorCode STATE_VERSION_CONFLICT — 해소(ErrorCode.java:18)
  - [하] prod CREATE TABLE 스크립트 — 남음(README.md:72는 안내만 있음)
  - [하] 03 구조 표 PollState* — 남음(누락 범위가 더 커짐)
  - [하] 연결 전 기기 권한 — 해소(PreLinkTokenInterceptor.java:21-27)
  - 완료 기준
  - C1 충족 — 57/57 체크, common 범위 GREEN 275 passed, 전체 GREEN 2261 passed
  - C2 미충족 — 03이 BusinessException · ErrorCode · 오류 본문 추가 키 · common.text를 담지 않음, 스케줄러 테스트 격리가 03 205행과 다름, 좌표 반올림 가정 미구현, 필터 단계 요청 로그 미결
  - C3 사람 확인 필요 — 규칙 어긋남 3건: IdempotencyKeyEntity 6인자(팀장 예외, 삭제 약속 미이행), 테스트 주석 9개 파일, common/text 소스 훑기 테스트
  - C4 충족 — 03 확인 필요 표와 FOLLOWUP 후속 42건에 기록됨. 다만 "05 전 · 08 전" 마감 항목 다수가 미결
  - 후속 과제
  - toKst(null) 처리 — 설계 결정 필요(호출 쪽 3곳 중복)
  - toKst 1초 미만 버림 — 설계 결정 필요
  - BaseTimeEntity 보조 테스트 · merge 때 createdAt null — 남음
  - Logback 로그 시각 +09:00 — 설계 결정 필요(배포 TZ=Asia/Seoul로 굳어짐)
  - JVM 시간대 고정 방식 — 설계 결정 필요
  - prod 시간대 · Clock 빈 위치 · toUtc Z 허용 — Clock 빈 해소(ClockConfig), 나머지 남음
  - JWT 클레임 이름 05 반영 — 해소(05 318행, JwtTokenIssuerTest 계약 테스트). deviceId는 03 표에 없음
  - Role enum 중복 — 해소(02 70행, Role은 common.security 하나)
  - 키 길이 미검사 · 키 생성 중복 · 좌표 반올림 · Nimbus 60초 — 60초만 해소, 키 생성은 3곳으로 늘어 남음, 반올림 남음
  - 이벤트 풀 임시값 · applicationTaskExecutor — 설계 결정 필요
  - 422 파라미터 field 이름 — 해소(#35), 클래스 단위 · 중첩 표기는 남음
  - 이벤트 풀 core=max · AbortPolicy · 본문 형식 오류 400 — 설계 결정 필요
  - FieldError 이름 · AsyncConfig @Value · 405/415 형식 — 405/415 해소, 나머지 남음
  - 테스트 DB 연결 수 — 해소(풀 3, max_connections 200)
  - 키 없음 400 vs 422 · /error · Allow 헤더 — 422 해소, /error와 Allow는 남음
  - access 토큰 폐기 — 설계 결정 필요
  - BusinessException 4인자 · null 방어 · WWW-Authenticate · 500 로그 requestId — 4인자와 requestId 해소, 나머지 남음
  - familyId null · 클레임 상수 위치 — familyId 해소, 상수 위치 남음
  - #19 role 검사 방식 — 해소
  - 소수 familyId · role 없는 USER — 설계 결정 필요
  - 4xx code=HttpStatus 이름 · charset — RSE는 도메인 사용 0건으로 사실상 해소(03 갱신 필요), charset 남음
  - #38 JSON 전용 — 해소
  - doc 인터페이스 HV000151 — 해소(#41)
  - NULLS NOT DISTINCT · SIG-02 eventId · 6인자 — 앞 둘 해소, 6인자 남음
  - 인증 없는 키 null 저장 — 해소(#42)
  - Idempotency 대상 · 해시 · 2xx만 저장 — 해소
  - jsonb 재응답 · 쿼리 문자열 · 빈 조기 생성 — 빈 조기 생성 해소(#43), 나머지 남음
  - 기동 실패 · 동시 요청 — 해소(#43 · #44)
  - #44 구현 메모(duplicate-key 로그 · 처리 중 행 409) — 남음
  - 3a 저장 실패 · 남은 처리 중 행 · charset — 설계 결정 필요
  - 인증 없는 공용 공간 보안 — 설계 결정 필요
  - prod sql.init · 유니크 두 개 · prod DDL — 앞 둘 해소, prod DDL 스크립트 남음
  - 만료 삭제 경계 — 해소(경계 24시간 테스트는 없음)
  - 스케줄러 테스트 격리 · 03 반영 — 03 반영 해소, 격리는 남음(위 [중])
  - 요청 로그 형식 — 해소
  - 인터셉터로 이동 — 해소(테스트 클래스 이름 RequestLoggingAspectTest는 남음)
  - 예외 메시지 개인정보 규칙 — 해소(02 199행)
  - SSE 로그 — 해소, 타임아웃 등의 종료 줄은 남음
  - 폴링 로그 — 해소(VehicleLinkCodeController.java:45)
  - springdoc 경로 — 해소(#33)
  - 리뷰 후 사람 결정(path 패턴 · Role · PublicEndpoints · exp) — 해소. SSE 503 · 스케줄 스레드 1개는 남음
  - 재리뷰 후 결정(6인자 · 필터 단계 로그 · 증거 인용) — 6인자 남음, 필터 단계 로그는 설계 결정 필요, 증거 인용 남음

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| common을 별도 설계서로 둠 | 소스 2.2 패키지 트리에 common이 도메인 6개와 나란히 있고 테스트할 동작(Idempotency-Key, 오류 형식, 시간 변환)이 있어 따로 둠. 6개 도메인보다 먼저 구현 |
| idempotency_key 컬럼 이름 · 타입 | 해소: principal_type · principal_id · idempotency_key VARCHAR(255) · request_hash char(64)(SHA-256 16진수) · response_status INT NULL · response_body JSONB(테이블 절) |
| 처음 응답의 HTTP 상태 코드 저장 | 해소: response_status 컬럼에 저장. null은 처리 중(예약만 된 행) |
| Idempotency-Key 범위 | 해소: 주체별(principal_type, principal_id, 키). 인증 없는 요청은 주체를 null로 저장해 모든 호출자가 함께 쓰는 공간(NULLS NOT DISTINCT 유니크 인덱스, 팀장 결정) |
| Idempotency-Key 헤더가 빠졌을 때 오류 코드 | 해소: 422 VALIDATION_FAILED, fields[0].field Idempotency-Key |
| 같은 키 요청이 동시에 두 개 들어올 때 | 해소: 실행 전에 행을 예약(response_status null)하고 유니크 인덱스로 두 번째 예약을 막음. 처리 중인 키로 온 요청은 409 IDEMPOTENCY_KEY_REUSED(팀장 결정) |
| 409 STATE_VERSION_CONFLICT 본문에 세션 뷰를 싣는 필드 이름 | 소스에 없음. error와 나란히 session 필드로 가정 |
| 좌표 변환을 common에 둠 | 소스의 common 목록에는 없으나 schedule · place · pickup이 같이 써서 재사용 규칙(2.2)을 적용 |
| 좌표 소수점 6자리 초과 입력 | 반올림할지 422로 거절할지 소스에 없음. 반올림으로 가정. 반올림 위치(GeoPoints 또는 요청 DTO)도 정하지 않음 |
| JWT 서명 방식 · 키 형식 | 해소: HS256, 설정 pickup.jwt.secret(Base64, 32바이트 이상), 환경 변수 JWT_SECRET. 클레임은 sub · principalType · familyId · role · iat · exp이고 exp는 필수(없으면 401, 사람 결정). 핵심 로직 「인증」 |
| SSE · LINK-02 로그 예외를 AOP가 아는 방법 | 해소: 경로 기준이 아님. SSE는 비동기 처리 시작(afterConcurrentHandlingStarted), 폴링은 @PollingLog(stateField)로 구분. 로깅도 AOP가 아니라 RequestLoggingInterceptor(팀장 결정) |
| 요청 로그 path에 경로 변수 값 | 해소: path는 매핑 패턴, 패턴이 없으면 "unknown". 폴링 직전 값 키만 메모리에서 실제 URI(사람 결정 2026-11-07 리뷰 후) |
| 500 로그 · Idempotency 저장 실패 로그의 requestId | 해소: logging.pattern.correlation "[%X{requestId:-}] "로 모든 로그 줄에 찍음 |
| DB 제약 위반 로그의 행 값 | 해소: pgjdbc logServerErrorDetail=false(spring.datasource.hikari.data-source-properties). 02 「예외를 만드는 쪽이 지킨다」는 그대로 둠(팀장 결정) |
| 인증 없는 API를 여는 방법 | 해소: 도메인별 PublicEndpoints 빈(PublicEndpoint(HttpMethod, pattern))을 SecurityConfig가 모아 permitAll, 그 경로는 Bearer 토큰을 읽지 않음(사람 결정). 핵심 로직 「인증 없는 API」 |
| /error 경로 인증 | /error는 permitAll이 아님. 컨트롤러 밖 예외가 서블릿 컨테이너의 /error로 넘어가면 인증에 걸려 401이 될 수 있음. 지금 공통 오류는 GlobalExceptionHandler · ErrorResponseWriter가 직접 씀. /error를 permitAll로 열지 정함 |
| 스웨거 Bearer 보안 스키마 | 해소: common.swagger.SwaggerConfig OpenAPI 빈(bearerAuth, 전역 보안 요구, 팀장 결정). 전역 요구라 인증 없는 API도 문서에 자물쇠가 붙음. doc 인터페이스에서 그 API만 보안 요구를 뺄지 정함 |
| access 토큰 「폐기」 기준 | 02는 「만료 · 폐기면 401」인데 common은 서명 · exp만 검사하고 05의 폐기는 Device.revokedAt(refresh)뿐. 폐기한 기기의 access 토큰이 만료까지 통과함. 「폐기는 refresh 기준」으로 문서를 고칠지, 요청마다 기기 폐기를 확인할지 auth 전에 정함(05 「AUTH-04 뒤 이미 발급된 access 토큰」과 같이) |
| Role enum 중복 | 해소: 02에 「common 순수 타입(Role · PrincipalType · LatLng)은 domain에서 써도 된다」 예외를 둠. Role은 common.security.Role 하나만 두고 user/domain에 따로 만들지 않음(사람 결정 2026-11-07 리뷰 후) |
| 주체 종류별 필수 클레임 | 지금 role 없는 USER 토큰은 role null로 통과하고, 소수 familyId(12.5)는 잘려 12가 됨. 05 토큰 형식과 함께 정함 |
| JWT 키 검사 · 키와 클레임 이름의 위치 | 키가 32바이트보다 짧아도 기동되고 모든 인증이 401. 기동 때 막을지 정함. 키 만드는 코드가 JwtDecoderConfig · AccessTokenFixture 두 곳이고 클레임 이름 상수는 AuthPrincipalJwtConverter 안에만 있음. auth 발급기가 생길 때 SecretKey 빈 · 상수 위치를 하나로 모을지 정함 |
| 인증 없는 Idempotency-Key 공용 공간의 보안 | 다른 기기가 같은 키 · 같은 본문을 보내면 첫 기기의 응답(토큰 포함 가능, AUTH-02 · LINK-04 · FAM-06)이 그대로 감. 키가 무작위 UUID이고 본문에 1회용 코드가 있어 위험은 낮다고 봄. 토큰을 돌려주는 인증 없는 API는 재응답 대신 409로 할지 05에서 정함 |
| 응답 저장(완료) 실패 때 처리 | 컨트롤러가 2xx로 끝났는데 응답 저장이 실패하면 지금은 행을 지우고 2xx를 보냄. 같은 키로 다시 보내면 유스케이스가 다시 실행될 수 있음. 대안: 행을 남겨 24시간 409 / 처리 중 행에 짧은 만료 |
| 남은 처리 중 행 | 처리 도중 서버가 꺼지거나 Error로 끝나 남은 처리 중 행은 만료 삭제 전까지 같은 키를 409로 막음. 행 삭제가 실패하면 원래 예외가 덮임. 그대로 둘지 처리 중 행만 짧게 지울지 정함 |
| Idempotency 재응답 본문의 같음 기준 | jsonb로 저장해 키 순서 · 공백이 첫 응답과 글자 단위로 다름. JSON이 아닌 2xx 본문은 저장에 실패함. JSON 값이 같으면 된다고 가정. 글자까지 같아야 하면 response_body를 text로 바꿈 |
| Idempotency 해시 · 재응답 범위 | 해시 경로에 쿼리 문자열이 들어가지 않음, 재응답에 헤더(Location 등)를 되살리지 않음, 본문 크기 제한 없음, 비동기 반환 API면 빈 200이 저장됨, 255자를 넘는 키 처리. 각각 그대로 둘지 정함 |
| 오류 응답 Content-Type charset | 필터 단계 오류(401 · 403 · 409)만 charset=UTF-8이 붙고 나머지는 없음(본문 바이트는 같음). 한쪽으로 맞출지 정함 |
| 405 · 401 · 415 응답 헤더 | 405에 Allow, 401에 WWW-Authenticate: Bearer, 415에 Accept 헤더가 없음. 붙일지 정함 |
| 본문 형식 오류의 코드 | JSON 값 타입 · enum · 시각 형식 오류와 빠진 본문이 모두 400 INVALID_REQUEST(fields 없음). 유지할지 422로 바꿀지 정함. INVALID_REQUEST 문구 「요청 형식을 확인해 주세요」는 구현자가 정함 |
| 도메인 코드의 ResponseStatusException | Spring 4xx 예외의 code는 HttpStatus 이름(CONFLICT 등)으로 나감. 도메인 코드에서 금지하고 BusinessException만 쓸지 정함. reason 없는 404 예외와 매핑 없는 경로의 문구가 다름 |
| 422 fields 표기 범위 | 클래스 단위 제약 오류는 fields에 안 담김. 중첩 · 배열 필드 이름 형식(a.b, a[].b 또는 children[0].name), 한 필드 여러 위반, List 본문 인덱스, 이름이 다른 @PathVariable은 정하지 않음 |
| BusinessException 생성자 | 해소: 공개 생성자 없음. 정적 팩토리 of(ErrorCode) · of(HttpStatus, String code, String message) · tooManyRequests(int retryAfterSeconds), ErrorCode에 TOO_MANY_REQUESTS 추가(팀장 결정). conflictWithOverlap(HttpStatus, String code, String message, List<Long> overlapScheduleIds) 추가(일정 SET-02 · 03의 overlapScheduleIds용, 06 결정). IdempotencyKeyEntity 6인자 생성자, status · code null 방어, 5xx BusinessException 로그는 다음 리뷰 때 다시 봄 |
| 도메인 코드 429의 retryAfterSeconds | of(HttpStatus, code, message)에는 retryAfterSeconds가 없고 tooManyRequests는 code가 TOO_MANY_REQUESTS로 고정됨. 05의 429 LINK_CODE_REISSUE_LIMIT · LINK_CODE_ATTEMPT_LIMIT에 retryAfterSeconds를 채울지, 채운다면 어떤 팩토리로 할지 auth 전에 정함 |
| 이벤트 스레드 풀 크기 · 포화 정책 | 소스에 없음. 임시값 core 2 · max 4 · queue 100(pickup.async.event.*). 큐가 찬 뒤에야 core를 넘어 늘어 실제 동시 실행은 2개(제안: core = max 4). 포화 때 기본 AbortPolicy라 AFTER_COMMIT 이벤트가 버려짐(재발송 스케줄러가 보완) → 유지 또는 CallerRunsPolicy. 직접 만든 Executor 빈 때문에 Boot의 applicationTaskExecutor가 빠져 MVC 비동기가 SimpleAsyncTaskExecutor를 쓸 수 있음(SES-08 때 확인). @Scheduled 실행 스레드는 기본 1개(08 시작 때 정함) |
| SSE 종료 줄 · 다른 비동기 응답 로그 | emitter 타임아웃 · 연결 끊김 · completeWithError 때 stream=closed 줄의 status · errorCode, Callable · DeferredResult 응답의 로그는 정하지 않음. SSE 타임아웃(AsyncRequestTimeoutException, 503)이 지금 500 + ERROR 로그로 처리됨. pickup SES-08 때 정함 |
| JVM 시간대 고정 방식 | 지금은 TimeZoneConfig 생성자에서 바꿈(빈 순서에 기대므로 Logback 등 먼저 뜬 부품은 +09:00 유지). 대안: main에서 run 전 설정 / 배포 때 TZ=UTC · -Duser.timezone=UTC / EnvironmentPostProcessor. 테스트 JVM을 build.gradle jvmArgs로 고정할지, 운영 로그 시각을 UTC로 찍도록 로그 패턴에 시간대를 넣을지도 함께 정함 |
| TimeUtils 경계 값 | toKst(null)을 null로 돌려줄지 호출 쪽에서 막을지, toKst가 1초 미만을 버려도 되는지, toUtc가 +09:00 말고 Z 등도 받을지 422로 거절할지 정하지 않음. prod 프로필 시간대 설정은 테스트 없이 설정 검토로만 확인함 |
| BaseTimeEntity 보조 테스트 | TIMESTAMPTZ 컬럼 테스트가 체크리스트 밖 보조 테스트로 있음. 테스트 케이스로 올릴지 정함. createdAt이 updatable=false라 merge 저장 때 돌려받은 엔티티의 createdAt이 null일 수 있음(04~09 변환 때 주의) |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인: 사용자 (04_사용자-user.md). 시작: /tdd domain user

## 후속 과제
- [설계 확인] TimeUtils.toKst(null) 처리: null을 그대로 돌려줄지 호출 쪽에서 막을지 미정. 응답에 null 가능 시각(unlinkedAt 등)이 많음
- [설계 확인] toKst 형식 yyyy-MM-dd'T'HH:mm:ssXXX는 1초 미만 값을 버림. 응답에서 버려도 되는지 확인 필요
- BaseTimeEntityTest의 TIMESTAMPTZ 컬럼 테스트는 체크리스트 밖 보조 테스트(공통 규칙 근거). 설계서 테스트 케이스로 올릴지 결정 필요. createdAt updatable=false라 merge 저장 시 반환 엔티티의 createdAt이 null이 되는 점은 04~09 도메인↔엔티티 변환에서 주의
- Logback 로그 시각이 시작 때 읽은 JVM 시간대(+09:00)로 찍힘. 운영 로그를 UTC로 남기려면 로그 패턴에 시간대 지정 필요(#27 요청 로깅 때 함께 검토)
- [설계 확인·중] JVM 시간대 고정 방식: 지금은 TimeZoneConfig 생성자(빈 순서에 기대어 Logback 등 먼저 뜬 부품은 +09:00 유지). 대안: main에서 run 전 설정(+@SpringBootTest useMainMethod=ALWAYS) / 배포 시 TZ=UTC·-Duser.timezone=UTC / EnvironmentPostProcessor. 테스트 JVM을 build.gradle jvmArgs -Duser.timezone=UTC로 고정할지도 함께 결정
- [설계 확인] prod 프로필 시간대 설정은 설정 검토로만 확인됨(테스트는 dev만). todayKst(Clock)에 넘길 Clock 빈 위치 미정(설계서 common 구조에 없음). toUtc가 +09:00 외 표기(Z 등)를 받을지 422로 거절할지 미정
- [설계 확인] JWT 클레임 · 설정 이름을 #9에서 정함(설계서에 없음): sub(주체 id 문자열) · principalType(VEHICLE|USER) · familyId(숫자) · role(USER만) · iat/exp(30분), HS256, 설정 키 pickup.jwt.secret(Base64, 32바이트 이상, 환경 변수 JWT_SECRET). auth 발급기 · #18 변환기가 같은 이름을 써야 함 → 05_인증-auth.md 확인 필요 사항에 반영 필요. Role enum을 common.security에 둠(user 도메인 Role과 중복 가능성)
- [설계 확인] Role enum 중복: common.security.Role(#9)과 04_사용자-user.md의 user/domain Role. 공통 규칙 "domain은 어떤 패키지도 모른다" 때문에 user.domain이 common Role을 못 씀 → (a) 두 enum + 변환 (b) common 순수 타입은 domain에서 허용하는 예외 (c) AuthPrincipal role을 다른 형태로. user 도메인 시작 전에 결정
- [후속] JwtDecoderConfig는 키 길이를 검사하지 않음(32바이트 미만이면 기동은 되고 모든 인증이 401) → 기동 때 막을지 결정. 키 생성 코드(Base64+HmacSHA256)가 JwtDecoderConfig · AccessTokenFixture 두 곳 → auth 발급기 생길 때 SecretKey 빈 하나로 모으기. 좌표 소수점 6자리 반올림 위치(GeoPoints vs 요청 DTO) 미정. Nimbus exp 검증 기본 60초 여유 → #16 만료 토큰은 60초보다 과거로
- [후속] 이벤트 스레드 풀 기본값 core 2 / max 4 / queue 100은 임시값(application.yml pickup.async.event.*) → 리뷰에서 확정. 직접 만든 Executor 빈 때문에 Boot 자동 applicationTaskExecutor가 빠질 수 있음 → MVC 비동기(Callable 등)가 SimpleAsyncTaskExecutor를 쓰게 됨. SSE(#30, pickup SES-08) 때 MVC 비동기 실행기 설정 필요 여부 확인
- [후속] 422 파라미터 오류의 field는 Java 파라미터 이름(-parameters)을 씀 → @RequestParam("childId") long id처럼 이름이 다르면 id가 나감. 애너테이션 이름을 쓰도록 테스트+구현 보강 필요. 클래스 단위 제약 오류(getGlobalErrors)는 fields에 안 담김. 중첩·배열 필드 이름 형식(a.b, a[].b vs children[0].name)과 한 필드 다중 위반 표기 미정
- [설계 확인] 이벤트 풀: ThreadPoolTaskExecutor는 큐가 찬 뒤에만 core를 넘어 늘어나 지금 실제 동시 실행 2개(제안: core=max 4). 풀 포화 시 기본 AbortPolicy라 AFTER_COMMIT 이벤트가 버려짐(재발송 스케줄러가 보완) → 유지 또는 CallerRunsPolicy. JSON 본문 타입·enum·시각 형식 오류와 빠진 본문이 모두 400 INVALID_REQUEST(fields 없음) → 유지 또는 422. INVALID_REQUEST 문구 "요청 형식을 확인해 주세요"는 구현자가 정함
- [리뷰 제안·하] ErrorResponse.FieldError 이름이 Spring FieldError와 같아 헷갈림(ErrorField 등으로 변경 제안). AsyncConfig의 @Value 3개 생성자 → @ConfigurationProperties("pickup.async.event") record 제안. 405·415·파라미터 타입 오류·500은 아직 Boot 기본 /error 형식이고 /error도 인증에 걸림(#13에서 다룸). 도메인 오류 코드는 설계서대로 BusinessException(status·code 문자열·retryAfterSeconds)로 각 도메인이 던지고, ErrorCode enum은 공통 코드만
- [후속·중] 테스트가 PostGIS 컨테이너 1개를 공유(컨텍스트마다 Hikari 10연결, 지금 4×10=40 / max_connections 100). 컨텍스트가 10개를 넘으면 too many clients 가능 → 테스트용 hikari.maximum-pool-size 축소 · max_connections 상향 · 중첩 @TestConfiguration을 공용 설정으로 모으기 중 결정. Docker가 꺼져 있으면 첫 클래스 ExceptionInInitializerError, 이후 NoClassDefFoundError로 보임
- [설계 확인] Idempotency-Key 헤더가 빠졌을 때 400(MissingRequestHeaderException)인지 422 VALIDATION_FAILED(설계서 가정)인지 → #24에서 결정. SecurityConfig에서 /error를 허용할지(필터 단계 예외가 /error로 갈 때 인증에 걸림, MockMvc로는 확인 불가 → RANDOM_PORT 테스트 필요). 405 응답에 Allow 헤더, 415에 Accept 헤더 없음(HTTP 규격상 405는 Allow 권장)
- [설계 확인·중] access 토큰 「폐기」: 공통 규칙은 만료·폐기면 401인데 common은 서명·exp만 검사, 05 auth의 폐기는 Device.revokedAt(refresh)뿐 → 폐기 기기의 access 토큰이 만료까지 최대 30분 통과. 「폐기는 refresh 기준」으로 문서 수정 vs 요청마다 기기 폐기 확인 중 결정(auth 전)
- [리뷰 지적·중] BusinessException 4인자 생성자가 파라미터 3개 규칙 위반 → 정적 팩토리(tooManyRequests 등) 또는 ErrorCode 기반 생성자로 바꾸고 공통 코드(NOT_FOUND·TOO_MANY_REQUESTS·FORBIDDEN)를 ErrorCode와 잇기. status·code null 방어 없음, 5xx BusinessException은 로그 안 남음. 401에 WWW-Authenticate: Bearer 헤더 없음. 500 로그에 메서드·경로·requestId 없음(#27에서 보완). 진행 순서: #36(4xx 유지)을 #24(Idempotency-Key 헤더)보다 먼저
- [설계 확인] AuthPrincipal.familyId는 Long(null 가능): familyId 클레임이 없는 토큰(연결 전 차량 등)을 null로 받을지 401로 막을지 05 auth와 #37에서 결정. JWT 클레임 이름 상수(principalType·familyId·role)가 AuthPrincipalJwtConverter 안에만 있고 AccessTokenFixture는 문자열을 따로 적음 → auth 발급기와 함께 쓸 common 상수 위치 결정
- [팀장 결정] #19 role 검사 방식: Spring 표준(authorities에 ROLE_{role}, 주체 종류는 PRINCIPAL_{type}) + @EnableMethodSecurity + 컨트롤러 @PreAuthorize("hasRole(...)"), 403 본문은 AccessDeniedHandler. 설계서에 방식이 없어 정함. AuthPrincipalAuthenticationToken.getName()이 AuthPrincipal.toString()을 돌려줌 → #27 호출 주체 로그 형식(예: USER:101)과 함께 결정
- [팀장 결정] familyId 클레임이 아예 없는 토큰은 401이 아니라 AuthPrincipal.familyId=null로 받음(Long 타입 + 05 auth의 연결 전 차량 토큰 가정). 숫자가 아닌 familyId는 401. [설계 확인] 소수 familyId(12.5)는 longValue로 잘림, role 없는 USER 토큰은 role=null로 통과 → 주체 종류별 필수 클레임을 05 auth 토큰 형식과 함께 결정
- [갱신] 앞 메모의 「405·415·파라미터 타입 오류·500은 Boot 기본 /error 형식」은 #13·#36으로 해결됨. 테스트 컨텍스트는 이제 7개(×Hikari 10 = 70연결) → 테스트 풀 크기 축소로 대응. [설계 확인] Spring 4xx 예외 code를 HttpStatus 이름(CONFLICT 등)으로 냄 → 도메인 코드에서 ResponseStatusException을 금지하고 BusinessException만 쓸지 결정. 401·403 응답만 Content-Type에 charset=UTF-8이 붙음(본문 바이트는 같음). reason 없는 404 RSE와 매핑 없는 경로의 NOT_FOUND 문구가 다름. VEHICLE 토큰에 role 클레임이 있으면 AuthPrincipal.role은 채워지나 ROLE_ 권한은 없음
- [팀장 결정] #38: Accept가 xml만이면 406 NOT_ACCEPTABLE(JSON 오류), json·*/*가 섞이면 200 JSON. XML 컨버터는 WebMvcConfigurer(common.web.JsonOnlyWebMvcConfig)로 제거하고 의존성은 그대로(google-cloud-storage가 XML 라이브러리 사용). 설계서 common 구조에 web 패키지가 없어 새로 둠 → 03 구조 표에 반영 필요. String을 돌려주는 엔드포인트는 StringHttpMessageConverter(*/*) 때문에 Accept xml에 application/xml로 나갈 수 있음(응답 DTO는 record 규칙이라 범위 밖)
- [설계 확인·상] doc 인터페이스(Controller implements ...ApiDoc) 구조에서 구현 메서드 파라미터에 @Min·@Positive·@Size 등을 달면 HV000151(ConstraintDeclarationException) → 지금 500. 테스트 컨트롤러가 인터페이스를 구현하지 않아 안 드러남. #24와 04~09 전 도메인 영향. 선택지: (a) 검증 애너테이션을 doc 인터페이스에 둠(02 "doc에는 스웨거만" 규칙 수정) (b) hibernate.validator.allow_parameter_constraint_override=true (c) 파라미터 제약 금지, DTO로만 검증. [하] reason 없는 4xx RSE 문구, List 본문 필드 인덱스 없음, @PathVariable 이름 다른 경우 테스트 없음, XML 요청 본문 415 테스트 없음. 03 구조 표에 "web: Spring MVC 공통 설정(응답은 JSON만)" 추가 필요
- [사람 결정 2026-11-07] doc 인터페이스 + 파라미터 검증 문제는 hibernate.validator.allow_parameter_constraint_override=true(ValidationConfigurationCustomizer)로 해결. 규칙 "doc에는 스웨거만, 검증은 컨트롤러"는 그대로. [설계 확인] 인증 없는 Idempotency-Key API(POST /v1/vehicle-devices · /v1/vehicle-links · /v1/invites/{token}/acceptance)는 principal이 null → 유니크 제약이 NULL끼리 중복을 못 막음 → schema.sql에 NULLS NOT DISTINCT 유니크 인덱스 등 #21·#24 전에 결정. jsonb 저장 시 키 순서·공백이 바뀜 → 재응답 본문은 JSON 내용 동등으로 비교. 08 SIG-02 eventId를 이 테이블에 넣을지 08에서 재결정. IdempotencyKeyEntity 생성자 인자 6개(파라미터 3개 규칙)
- [팀장 결정] 인증 없는 Idempotency-Key 요청은 principal_type·principal_id를 null로 저장하고, schema.sql에 (principal_type, principal_id, idempotency_key) NULLS NOT DISTINCT 유니크 인덱스를 둔다(VIN 부분 유니크 인덱스와 같은 방식, prod는 같은 SQL을 직접 실행). 항목 #42로 TDD. response_status는 int라 NOT NULL로 만들어짐(먼저 행을 만들고 나중에 응답을 채우는 방식을 쓰면 바꿔야 함)
- [팀장 결정] Idempotency-Key: (1) 대상 표시는 메서드 애너테이션 @IdempotencyKeyRequired(같은 경로라도 HTTP 메서드마다 다름) (2) 처리는 설계서 이름대로 IdempotencyKeyFilter(Security 체인 뒤, HandlerMapping으로 애너테이션 확인, 본문 래핑) (3) 요청 해시 = SHA-256(HTTP 메서드 + 경로 + 본문 바이트) 16진수 64자, JSON 정규화는 하지 않음(#23에서 재확인) (4) 2xx 응답만 저장, 4xx·5xx는 저장하지 않아 같은 키로 재시도 가능 → 03 확인 필요 사항에 반영 필요
- [설계 확인] Idempotency 재응답 본문은 jsonb 저장이라 키 순서·공백이 첫 응답과 글자 단위로 다름(JSON 값은 같음) → 글자까지 같아야 하면 response_body를 text로 바꾸는 설계 변경 필요. JSON이 아닌 2xx 본문은 jsonb 저장 실패. 요청 해시 경로에 쿼리 문자열 미포함(getRequestURI만). 필터가 requestMappingHandlerMapping·저장소를 생성자로 받아 기동 시 빈이 일찍 만들어짐 → 실제 서버 기동 확인 필요(MockMvc로는 문제없음)
- [팀장 결정] (1) 기동 실패: IdempotencyKeyFilter가 Tomcat 기동 단계에서 저장소→EMF→DataSource를 일찍 만들어 Testcontainers 접속 정보 등록 전에 localhost:5432로 연결 실패(bootTestRun · RANDOM_PORT). #43 RANDOM_PORT 테스트로 RED 후 필터의 store 주입을 ObjectProvider/@Lazy로 늦춤. 필터 순서를 @Order로 명시. (2) 동시 요청: 행을 먼저 예약(response_status null = 처리 중)하고 응답을 나중에 채움. 처리 중 키로 온 요청은 409 IDEMPOTENCY_KEY_REUSED(#44). [하] 재응답 시 헤더(Location 등) 미복원, 본문 크기 제한 없음, 비동기 반환 대상이면 빈 200 저장, bootTestRun이 테스트 엔티티 테이블을 dev DB에 만듦, 키 길이 varchar(255) 초과 시 예외(#24와 함께)
- [#44 구현 메모] 예약 INSERT는 저장소 save 트랜잭션에서 바로 커밋(필터는 트랜잭션 밖, open-in-view false). 예약 경쟁에서 진 요청마다 Hibernate duplicate-key 로그가 한 번 남음(ON CONFLICT DO NOTHING 네이티브 쿼리로 줄일 수 있음). SecurityErrorResponseWriter → common.error.ErrorResponseWriter로 옮김(필터 409도 사용). 처리 도중 서버가 죽어 남은 예약 행은 #26 24시간 삭제 전까지 409
- [설계 확인] (A) 컨트롤러가 2xx로 끝났는데 complete 저장이 실패하면 지금은 행을 지우고 2xx를 보냄 → 같은 키 재시도 때 유스케이스 재실행 위험. 대안: 행을 남겨 24시간 409 / 처리 중 행에 짧은 만료. (C) 서버 중단·Error로 남은 처리 중 행의 24시간 409 문제를 #26과 함께 결정. (D) 오류 응답 Content-Type: 401·403·409만 charset=UTF-8이 붙음 → 한쪽으로 통일 결정. release 실패 시 원래 예외가 덮이고 catch가 Throwable이 아님(하). doFilterInternal이 15줄 한도라 #24·#42에서 넘으면 나눠야 함. 03 설계서 핵심 로직에 Idempotency 팀장 결정(2xx만 저장·처리 중 409·해시 입력·주체별·예약 후 완료) 반영 필요
- [설계 확인·보안] 인증 없는 Idempotency-Key는 모든 호출자가 함께 쓰는 공간 → 다른 기기가 같은 키·같은 본문을 보내면 첫 기기의 응답(토큰 포함 가능, 예: LINK-04 · AUTH-02)이 그대로 돌아감. 키가 무작위 UUID이고 본문에 1회용 코드가 들어가 위험은 낮지만 05 auth 설계서에 명시하고, 토큰을 돌려주는 인증 없는 API는 재전송 대신 409로 할지 등 auth에서 결정
- [운영 주의] sql.init.mode always가 공통 application.yml에 있어 prod(ddl-auto validate)에서도 기동 때 schema.sql 실행 → idempotency_key 테이블을 SQL로 먼저 만들어 두지 않으면 기동 실패. README·설계서에 prod 순서(테이블 DDL 먼저 실행) 반영 필요. 같은 세 컬럼에 @UniqueConstraint(NULLS DISTINCT)와 uk_idempotency_key_principal_key(NULLS NOT DISTINCT) 두 유니크가 있음 → 하나 정리. 03 「Idempotency-Key 범위: 주체별」 가정을 「인증 없는 요청은 공용 공간」으로 고쳐야 함
- [팀장 결정] 만료 키 삭제 경계: created_at < now - 24h(엄격히 작음, 정확히 24시간은 남김), response_status와 무관(처리 중 행도 정리). Clock 빈은 common.time.ClockConfig(Clock.systemUTC()) — FOLLOWUP의 「Clock 빈 위치 미정」 해소. 스케줄은 @Scheduled, 주기·지연 기본 PT1H 설정값(서버 1대 전제, 2대 이상이면 ShedLock)
- [팀장 결정·남은 작업] (1) 스케줄러 테스트 격리: 테스트 공통 레지스트라에서 pickup.idempotency.cleanup-interval을 아주 길게(예: P1D) 둠 — 도메인 리뷰 전 정리 패스에서 처리. 04~09의 10초 자동 완료 · 재발송 스케줄러도 같은 방식. (2) 도메인 리뷰 전에 03 설계서 반영: 핵심 로직 Idempotency-Key(2xx만 저장 · 예약 후 완료 · 처리 중 409 · 해시=메서드+경로+본문 · 예약 DB 오류 500 · 인증 없는 요청 공용 공간 · 삭제 created_at < now-24h), 확인 필요 사항 갱신, 구조 표(common.web · time.ClockConfig · async.SchedulingConfig), 테이블 표(컬럼 · NULLS NOT DISTINCT 인덱스), prod DDL에 schema.sql 인덱스 포함. 유니크는 이제 schema.sql 인덱스 하나뿐(@UniqueConstraint 제거)
- [팀장 결정] 요청 로그: INFO 한 줄 key=value(requestId · principal · method · path · status · durationMs), 호출 주체 USER:101 · VEHICLE:77 · ANONYMOUS, requestId는 안전한 X-Request-Id 헤더(영숫자·-·_ 64자 이하)를 쓰고 없으면 UUID, MDC "requestId"에 넣어 500 로그 등에서도 사용. 포인트컷 @within(RestController). AOP 의존성 spring-boot-starter-aspectj 추가
- [팀장 결정] 요청 로깅 위치를 AOP(@Around)에서 HandlerInterceptor.afterCompletion으로 옮김: @Valid 실패는 컨트롤러 호출 전에 나고, 예외 응답 status는 advice가 aspect 뒤에 정하기 때문. 오류 코드는 GlobalExceptionHandler가 요청 속성에 넣고 로깅은 읽기만. 설계서 「요청 로깅 AOP」 이름과 다름 → 03 반영 필요
- [팀장 결정·코딩 규칙] 예외 메시지에 개인정보(전화번호 · 위치 · 토큰 · 연결 코드)를 넣지 않는다. 500 처리기 · Idempotency 저장 실패 로그는 예외 내용을 서버 로그에 남기므로(공통 규칙) 메시지 마스킹은 하지 않음 → 02 공통 규칙 「로그」 절에 이 규칙 추가 필요. 요청 로그는 본문 · 쿼리 · 헤더를 남기지 않음(#29 테스트로 고정)
- [팀장 결정] SSE 로그: 경로가 아니라 비동기 시작(afterConcurrentHandlingStarted)으로 구분. 연결 줄 stream=open, 종료 줄 stream=closed + durationMs(연결 유지 시간), 같은 requestId(요청 속성으로 ASYNC 디스패치에 MDC 복원). 03 확인 필요 사항의 「경로 기준으로 AOP가 구분」 가정 수정 필요. [후속] Callable·DeferredResult 등 다른 비동기 응답, emitter 타임아웃·연결 끊김·completeWithError 때 종료 줄 status·errorCode는 미정(pickup SES-08 때 결정)
- [팀장 결정] 폴링 로그: 경로가 아니라 @PollingLog(stateField) 애너테이션으로 표시, 응답 본문 status 값(enum name)이 같은 요청 경로(codeId별)의 직전 값과 같으면 생략, 4xx·5xx는 항상 기록. 직전 값은 메모리 LRU(크기 제한·만료, 서버 1대 전제). auth LINK-02 컨트롤러에 @PollingLog(stateField = "status")를 달아야 함 → 05 auth 반영. 03 확인 필요 사항 「경로 기준」 수정
- [팀장 결정] springdoc 경로(/v3/api-docs/** · /swagger-ui.html · /swagger-ui/**)는 프로필과 상관없이 permitAll, 문서 존재는 springdoc.*.enabled(공통 off · dev on)로만 결정 → prod에서는 404 NOT_FOUND(401로 경로 존재를 드러내지 않음). prod 프로필 테스트는 ddl-auto=update로 덮어씀(테스트 전용 엔티티 때문에 validate 불가)
- [사람 결정 2026-11-07 도메인 리뷰 후] (1) 요청 로그 path는 매핑 패턴(폴링 직전 값 키는 메모리에서만 실제 URI) (2) Role: common의 순수 타입(enum · 값 객체: Role · PrincipalType · LatLng)은 domain에서 써도 되는 예외를 02에 둠 — Role은 common.security 하나 (3) 인증 없는 API: 도메인별 PublicEndpoints 빈(메서드+경로 패턴)을 common SecurityConfig가 모아 permitAll, 그 경로에서는 Bearer 토큰을 읽지 않음 (4) exp 필수. [팀장 결정] DB 오류 상세는 pgjdbc logServerErrorDetail=false, Swagger에 Bearer 보안 스키마 OpenAPI 빈(common.swagger), BusinessException(ErrorCode)/정적 팩토리로 공통 코드 연결 · 파라미터 3개 규칙 맞춤 · ErrorCode에 TOO_MANY_REQUESTS 추가. [하] 처리: 증거 인용(#39는 reason 있는 테스트로) · 03 낡은 행 · FOLLOWUP 설계서 사본 · 404/401 조건 · /error 행 · README prod DDL · 순환(ERROR_CODE 키를 common.error로) · 테스트 이름 RequestLoggingAspectTest → 정리 패스. SSE 503 · @Scheduled 스레드 1개는 08 시작 때
- [팀장 결정·재리뷰 후] (1) IdempotencyKeyEntity 6인자 생성자는 package-private이고 테스트에서만 쓰므로 이번 도메인에서는 규칙 예외로 둔다 — 04부터 엔티티 생성자도 파라미터 3개 이하(필요하면 값 묶음 · 정적 팩토리). 다음 shared 정리 때 테스트를 IdempotentRequest 생성자 + complete()로 바꾸고 6인자 생성자 삭제. (2) 필터 단계 요청 로그(Idempotency 재응답 · 409 · 422 · 예약 500 · Security 401)는 05 시작 전 사람 결정. (3) 증거 인용 #14 · #31은 테스트 이름이 바뀐 것(동작 동일). [남은 후속] 5xx BusinessException 로그 · null 방어, 3a 저장 실패 테스트, ErrorCode STATE_VERSION_CONFLICT(08), prod CREATE TABLE 스크립트, 03 구조 표 PollState* · RequestLoggingConfig 추가, 연결 전 기기 토큰 권한은 auth에서
