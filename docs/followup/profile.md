# FOLLOWUP · 프로필사진 (profile)

- 생성: 2026-11-13 22:01 KST · tdd-guard
- 상태: 완료 (체크 39/39 · 완료 기준 4/4)
- 테스트: ● GREEN · 2687 passed · 전체 · 135s · .\gradlew.bat test --console=plain
- 설계서: C:/pickup/docs/10_프로필사진-profile.md

## 완료 항목
- [x] #1 [기반] ProfileImage.of는 JPEG · PNG · WEBP 매직 넘버를 판정해 형식과 확장자(jpg · png · webp)를 정한다 ← ProfileImageTest.ProfileImage.of는 매직 넘버만 있는 최소 길이 파일도 형식을 판정한다
- [x] #2 [기반] ProfileImage.of는 매직 넘버가 JPEG · PNG · WEBP가 아니면 ProfileImageInvalidException을 던진다 ← ProfileImageTest.[8] "RIFF WAVE"
- [x] #3 [기반] ProfileImage.of는 5,242,880바이트를 넘으면 ProfileImageTooLargeException을 던지고 딱 5,242,880바이트는 받는다 ← ProfileImageTest.ProfileImage.of는 5,242,880바이트를 넘으면 ProfileImageTooLargeException을 던진다
- [x] #4 [기반] ProfileImage.newKey는 profiles/{UUID}.{ext} 형식의 키를 만든다 ← ProfileImageTest.ProfileImage.newKey는 부를 때마다 다른 키를 만든다
- [x] #5 [기반] 테스트 픽스처 InMemoryProfileImageStorage는 put한 키와 바이트 · Content-Type을 기록하고 presignedUrl로 키가 담긴 URL을 돌려준다 ← InMemoryProfileImageStorageTest.put한 키 · 바이트 · Content-Type을 그대로 기록한다
- [x] #6 [persistence] User의 profileImageKey는 NULL로 저장되고 값을 넣으면 다시 읽었을 때 같은 값이다 ← UserPersistenceAdapterTest.User의 profileImageKey에 값을 넣어 저장하면 다시 읽었을 때 같은 값이다
- [x] #7 [서비스] 멤버 등록은 사진이 있으면 검증을 모두 통과한 뒤 저장소에 올리고 User.profileImageKey에 그 키를 저장한다 ← RegisterMemberServiceTest.멤버 등록은 사진이 있으면 검증을 모두 통과한 뒤 저장소에 올리고 User.profileImageKey에 그 키를 저장한다
- [x] #8 [서비스] 멤버 등록이 권한 · 멤버 한도 검증에서 실패하면 저장소에 아무것도 올리지 않는다 ← RegisterMemberServiceTest."권한", RegisterMemberServiceTest."멤버 한도"
- [x] #9 [서비스] 멤버 등록은 저장소 업로드가 실패하면 StorageUnavailableException으로 끝나고 User를 만들지 않는다 ← RegisterMemberServiceTest.멤버 등록은 저장소 업로드가 실패하면 StorageUnavailableException으로 끝나고 User를 만들지 않는다
- [x] #10 [서비스] 멤버 등록은 사진이 없으면 저장소를 부르지 않고 profileImageKey를 비워 둔다 ← RegisterMemberServiceTest.멤버 등록은 사진이 없으면 저장소를 부르지 않고 profileImageKey를 비워 둔다
- [x] #11 [서비스] 보호자 생성(LINK-04 NEW)은 사진이 있으면 업로드한 키를 보호자 User에 저장한다 ← CreateGuardianServiceTest.보호자 생성(LINK-04 NEW)은 사진이 있으면 차량 검증 뒤 저장소에 올리고 업로드한 키를 보호자 User에 저장한다
- [x] #12 [서비스] 연결 승인이 코드 · 동의 검증에서 실패하면 저장소에 아무것도 올리지 않는다 ← ApproveLinkServiceTest.연결 승인이 코드 검증에서 실패하면 사진이 있어도 보호자 생성 · 사진 변경을 부르지 않아 저장소에 아무것도 올리지 않는다
- [x] #13 [서비스] 연결 승인 RELINK에 사진이 있으면 보호자의 profileImageKey를 새 키로 바꾸고 이전 객체를 지운다 ← ChangeProfileImageServiceTest.연결 승인 RELINK에 사진이 있으면 보호자의 profileImageKey를 새 키로 바꾸고 이전 객체를 지운다
- [x] #14 [서비스] 연결 승인 RELINK에 사진이 없으면 보호자의 profileImageKey를 그대로 둔다 ← ChangeProfileImageServiceTest.연결 승인 RELINK에 사진이 없으면 보호자의 profileImageKey를 그대로 둔다
- [x] #15 [서비스] 저장소 설정이 없을 때 사진이 있는 등록 요청은 StorageUnavailableException으로 끝난다 ← RegisterMemberServiceTest.저장소 설정이 없을 때 사진이 있는 등록 요청은 StorageUnavailableException으로 끝난다
- [x] #16 [서비스] 가족 조회는 profileImageKey가 있는 멤버에게 그 키의 서명 URL을, 없는 멤버에게 기본 아바타 서명 URL을 준다 ← GetMyFamilyServiceTest
- [x] #17 [서비스] 가족 조회는 저장소 설정이 없으면 모든 멤버의 profileImageUrl을 null로 준다 ← GetMyFamilyServiceTest.가족 조회는 저장소 설정이 없으면 모든 멤버의 profileImageUrl을 null로 준다
- [x] #18 [web] POST /v1/families/me/members는 multipart의 request 파트와 profileImage 파트를 받아 201과 userId를 돌려준다 ← FamilyControllerTest.POST /v1/families/me/members는 multipart의 request 파트와 profileImage 파트를 받아 201과 userId를 돌려준다
- [x] #19 [web] POST /v1/families/me/members는 profileImage 파트 없이 multipart로 보내도 201을 돌려준다 ← FamilyControllerTest.POST /v1/families/me/members는 profileImage 파트 없이 multipart로 보내도 201을 돌려준다
- [x] #20 [web] POST /v1/families/me/members는 지금처럼 application/json 본문도 받아 201을 돌려준다 ← FamilyControllerTest.POST /v1/families/me/members는 지금처럼 application/json 본문도 받아 201을 돌려준다
- [x] #21 [web] POST /v1/families/me/members의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다 ← FamilyControllerTest.POST /v1/families/me/members의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다
- [x] #22 [web] POST /v1/families/me/members의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를 돌려준다 ← FamilyControllerTest.POST /v1/families/me/members의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를 돌려준다
- [x] #23 [web] POST /v1/families/me/members는 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다 ← FamilyControllerTest.POST /v1/families/me/members는 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다
- [x] #24 [web] POST /v1/vehicle-links는 multipart의 request 파트와 profileImage 파트를 받아 201과 토큰을 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links는 multipart의 request 파트와 profileImage 파트를 받아 201과 토큰을 돌려준다
- [x] #25 [web] POST /v1/vehicle-links는 지금처럼 application/json 본문도 받아 201을 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links는 지금처럼 application/json 본문도 받아 201을 돌려준다
- [x] #26 [web] POST /v1/vehicle-links의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다 ← VehicleLinkCodeControllerTest.POST /v1/vehicle-links의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다
- [x] #27 [web] GET /v1/families/me는 members[]마다 profileImageUrl을 돌려준다 ← FamilyControllerTest.GET /v1/families/me는 members[]마다 profileImageUrl을 돌려준다
- [x] #28 [web] multipart는 POST /v1/vehicle-links와 POST /v1/families/me/members에서만 받고 다른 API에 multipart를 보내면 415 UNSUPPORTED_MEDIA_TYPE을 돌려준다 ← FamilyControllerTest
- [x] #29 [web] 요청 전체가 multipart 한도(6MB)를 넘어 컨트롤러 전에 거절돼도 413 PROFILE_IMAGE_TOO_LARGE 오류 본문을 쓴다 ← GlobalExceptionHandlerRealServerTest
- [x] #30 [통합] multipart 요청의 Idempotency 해시는 경계 문자열이 달라도 request 파트와 사진 바이트가 같으면 같다 ← RequestHasherTest
- [x] #31 [통합] multipart 요청의 Idempotency 해시는 사진 바이트가 다르면 달라진다 ← RequestHasherTest
- [x] #32 [통합] 같은 Idempotency-Key로 경계만 다른 multipart 재시도는 처음 응답을 돌려주고 사진을 다시 올리지 않는다 ← IdempotencyKeyFilterMultipartTest
- [x] #33 [통합] MinioProfileImageStorage.presignedUrl은 MINIO_PUBLIC_ENDPOINT 주소로 X-Amz-Expires가 url-ttl 초인 URL을 만든다 ← MinioProfileImageStorageTest
- [x] #34 [통합] MinioProfileImageStorage는 MINIO_ENDPOINT가 비면 configured()가 false이고 presignedUrl · defaultUrl이 비어 있다 ← MinioProfileImageStorageTest
- [x] #35 [통합] 실제 서블릿 컨테이너(RANDOM_PORT)에서 Idempotency 필터를 거친 POST /v1/families/me/members multipart 요청이 request · profileImage 파트를 읽어 201을 돌려준다 ← FamilyControllerRealServerTest
- [x] #36 [서비스] 연결 승인 RELINK의 이전 사진 객체는 트랜잭션이 커밋된 뒤에 지우고 커밋이 실패하면 지우지 않는다 ← ChangeProfileImageServiceTransactionTest
- [x] #37 [web] POST /v1/families/me/members multipart에 request 파트가 없거나 JSON이 틀리면 400 INVALID_REQUEST를, request 파트 검증에 실패하면 422 VALIDATION_FAILED를 돌려준다 ← FamilyControllerTest
- [x] #38 [web] POST /v1/vehicle-links multipart의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를, 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다 ← VehicleLinkCodeControllerTest
- [x] #39 [통합] multipart 요청의 Idempotency 해시는 profileImage 파트가 없거나 0바이트면 사진 자리에 "-"를 넣어 계산한다 ← RequestHasherTest

## 미완료 항목
- 없음

## 완료 기준 (공통 규칙 문서)
- [x] C1 설계서의 테스트 케이스가 모두 GREEN
- [x] C2 구현이 설계서의 API 명세 · 핵심 로직과 일치
- [x] C3 리뷰어 검토 완료, 공통 규칙 위반 없음
- [x] C4 설계서의 확인 필요 사항이 해소되거나 FOLLOWUP에 기록됨

## 도메인 리뷰
- 2026-11-10 03:07 KST · 도메인 리뷰어 · 결론 통과 · 상 0 · 중 0 · 하 2 · 리뷰 시점 체크 39/39
- 리뷰 시점 테스트: ● GREEN · 2349 passed · 전체 · 282s · .\gradlew.bat test --console=plain
  - 결론: 통과
  - 이전 리뷰의 중 2건 · 하 10건은 모두 해소됐거나 FOLLOWUP note로 넘어갔다(스웨거 FAM-01 · LINK-04 413 · 503, #37 multipart 400 · 422, #38 LINK-04 413 · 503, #39 해시 「-」, 주석 3곳, 문서 02 · 03 · README · api.yaml, 409 뒤 남는 객체, max-swallow-size).
  - [하] PNG 상수 통합이 일부만 됐다. PNG_IHDR_BYTES와 같은 값의 로컬 상수 3곳이 남음 — VehicleLinkCodeControllerTest:102 PNG_BYTES, FamilyControllerTest:84 PNG_BYTES, RequestHasherTest:24 PNG_A → 리팩터링(선택)
  - [하] FOLLOWUP 후속 과제 목록에 이미 처리한 메모(cause 생성자, 픽스처 주석, AFTER_COMMIT, 스웨거, reset)가 남아 있다 → note(해소 표시)
  - 완료 기준: C1 충족(39/39, GREEN 2349) · C2 충족 · C3 충족 · C4 충족
  - 남은 후속 과제: ProfileImage equals · toString, FAM-03 profileImageUrl(사진 변경 API와 함께 결정), FAM-01 서명 예외 503, PUBLIC_ENDPOINT만 빈 경우, 02 픽스처 문구, TODO.md 진행 기록, PNG 상수 3곳, minio 의존성 하향 · dev 실동작(업로드 · 서명 URL · ingress Host · prod ALTER TABLE)
- 이전 리뷰: 2026-11-10 02:51 KST (조건부 통과)

## 확인 필요 사항 (설계서)
| 항목 | 현재 가정 |
|---|---|
| 사진 변경 · 삭제 API | 이번 범위 밖. 등록(LINK-04 · FAM-02)과 RELINK 때만 바뀐다. FAM-03 · 별도 API는 추후 |
| 커밋 실패로 남은 객체 | 지우지 않는다. 업로드 뒤 같은 트랜잭션의 다음 단계가 실패한 경우(예: LINK-04 NEW에서 saveVehicle이 동시 승인으로 409 VEHICLE_ALREADY_LINKED)도 같다. 업로드는 검증 뒤 · 저장 직전이라 드물다. 버킷 수명 주기 규칙이나 정리 작업은 추후 |
| FAM-07 멤버 해제 때 사진 | 객체를 지우지 않는다(행을 지우지 않는 04 규칙과 같게) |
| 이미지 리사이즈 · EXIF 제거 | 하지 않는다. 앱이 올리기 전에 줄이고 위치 정보를 지운다 |
| FAM-01 말고 사진을 보여 줄 곳(SES-03 · SES-10 · 차량 화면) | 이번 범위 밖 |
| 저장소 | 홈서버 designmodu-dev 네임스페이스의 MinIO를 같이 쓴다(버킷 · 계정은 ilink 전용). 그 MinIO가 없어지면 사진이 안 보인다 |

## 다음 단계
- 커밋 · 푸시: /tdd commit (기능별 분할 커밋) · /tdd push (커밋 후 기능 브랜치 푸시)
- 다음 도메인 없음. 전체 완료 여부를 사람과 확인

## 후속 과제
- StorageUnavailableException에 (Throwable cause) 생성자 추가 — MinIO 어댑터(#33 · #34) 단계에서 연결 · 인증 오류를 감쌀 때
- 테스트 픽스처 InMemoryProfileImageStorage · Configuration · ProfileImageTest의 설명 주석 정리(공통 규칙 「주석은 최대한 달지 않는다」) — 서비스 · 웹 항목이 픽스처를 다 쓴 뒤 리뷰어가 처리. 픽스처 위치는 기존 모듈별 fixture 관행(auth · place · pickup)을 따른다
- ProfileImage는 byte[]를 가진 record라 equals · toString이 참조 기준이다. 테스트에서 값 비교가 필요해지면 equals · hashCode · toString 재정의를 검토
- RELINK 사진 교체: 이전 객체 delete가 saveUser 뒤 같은 트랜잭션 안에서 일어나 커밋이 실패하면 이전 사진이 이미 지워질 수 있다. 커밋 뒤(AFTER_COMMIT) 삭제로 옮길지 검토
- 멤버 수정(FAM-03 UpdateMemberService) 결과의 profileImageUrl은 null로 둔다(이번 범위는 FAM-01만). FAM-03 응답에도 사진 URL을 보일지는 사진 변경 API와 함께 추후 결정
- 스웨거 문서 정리: VehicleLinkCodeApiDoc.approveLinkWithProfileImage에 413 PROFILE_IMAGE_TOO_LARGE · 503 STORAGE_UNAVAILABLE 응답 추가, FamilyApiDoc.getMyFamily 200 설명을 「사진이 없으면 기본 아바타 URL, 저장소 설정이 없을 때만 null」로 고침(동작 변경 없음)
- 테스트의 PNG_BYTES 상수 6곳(두 가지 값) 공통 픽스처로 합칠지 검토. 통합 테스트에서 InMemoryProfileImageStorage @Primary 빈을 쓸 때 @BeforeEach에서 reset()을 부를 것(#32 · #35)
- io.minio:minio 9.0.4가 원하는 jackson 2.21.8 · commons-codec 1.23.0이 Spring Boot BOM 때문에 2.21.7 · 1.21.0으로 내려가고 okhttp 5.5.1 · kotlin-stdlib 2.3.21이 새로 들어온다. dev 배포에서 업로드 · 서명 URL이 실제로 동작하는지 확인
- FAM-01에서 서명 URL 생성이 예외를 던지면 503 STORAGE_UNAVAILABLE이 된다(설계서 FAM-01 오류 열은 공통). 서명은 네트워크 호출이 없어 드물다. 사진만 null로 두고 200을 줄지 추후 결정
- MINIO_ENDPOINT만 있고 MINIO_PUBLIC_ENDPOINT가 빈 경우의 동작(기동 실패 여부) 미검사. dev ConfigMap은 둘 다 넣어 둠
- 공통 규칙 02 「픽스처는 common에 한 번만」 문구와 실제 관행(모듈별 fixture: UserSeed · InMemoryProfileImageStorage 등)이 다르다. 문구를 관행에 맞출지 사람 결정
- docs/followup/TODO.md 진행 기록이 pickup · notification(완료 4/4)과 다르게 「대기 · 완료 기준 0/4」로 나온다(shared~place도 같은 문제, pickup.md에 기록). TODO 수동 정리 또는 리뷰 기록 복원 필요
- dev 배포 뒤 확인: 실제 MinIO 업로드 · 서명 URL 열람, ingress(img-dev.chisanw.com)가 Host 헤더를 그대로 넘겨 서명이 맞는지, prod는 ALTER TABLE users ADD COLUMN profile_image_key VARCHAR(200) NULL 선행
- 해소된 후속 과제(재리뷰 확인): StorageUnavailableException cause 생성자 · 픽스처 주석 정리 · RELINK AFTER_COMMIT 삭제(#36) · 스웨거 문서 정리 · 통합 테스트 reset(). PNG 상수는 TestImages로 합쳤고 FamilyControllerTest · VehicleLinkCodeControllerTest · RequestHasherTest의 같은 값 로컬 상수 3곳만 남음(선택)
- [사람 결정 2026-11-13] D4: 「다음 세션부터 적용」 문구를 코드(매번 최신 값 읽음)에 맞춰 04 · 06 설계서와 FamilyApiDoc · ScheduleApiDoc 문구를 「바로 적용」으로 고친다. D5: 08 SIG-04 403 LOCATION_CONSENT_REQUIRED 행에 「동의 철회 기능 전까지 나지 않음」 표시, 임시 어댑터 유지. D6: spring.task.scheduling.pool.size=4 (shared 항목 추가 필요). SES-11: notBoardedChildUserIds에 세션 밖 childUserId가 오면 404 NOT_FOUND (pickup 항목 추가 필요)
- [사람 결정 2026-11-13] D1: JoinCallMode · DeviceStatus · Platform · ScheduleStatus는 코드 그대로 두고 02 공통 규칙 L70 예외 목록에 추가. D2: ACTIVE_PICKUP_EXISTS · SETUP_INCOMPLETE를 common ErrorCode 하나 · 문구 하나로 합침(응답 code 그대로). D3: 모든 429에 retryAfterSeconds를 채우는 도메인 code용 팩토리 하나로 통일, HELP의 nextAllowedAt은 추가 키로 유지. D7: 02에 「DB FK 없음」 관례 명시, FAM-07 멤버 해제는 실제 삭제 대신 삭제 표시(removedAt)로 바꾸고 조회에서 해제 멤버 제외(user 항목 추가 필요)
- [사람 결정 2026-11-13] D8: 임시 어댑터 체크 항목 5개(04 #64 · 05 #98 · 06 #58 · 07 #44 · 08 #204) 문장을 지금 실제 연동 동작에 맞게 고침 승인. D9: 다른 모듈 호출은 in 포트 직접 주입 · out 포트+adapter/out 둘 다 허용, 02에 적고 02:19 의존 방향 목록을 실제에 맞춤. D10: DRAFT 일정은 SET-09 · SES-02 모두 missing SCHEDULE, 판정 코드 한 곳(schedule 항목 추가 필요). D11: 리뷰가 「사람 확인 필요」로 남긴 완료 기준은 체크하지 않음(pickup C2 · notification C3 체크 해제 검토)
- [팀장 가정 2026-11-13 · 사람 확인 필요] 최종디자인 11/13 반영 항목: (1) 추천 장소 · 여기로 부르기는 서버가 임시 장소(temporary)를 만드는 방식 (2) 여기로 부르기는 장소 변경 + 합류 안내를 서버가 한 번에 (3) 번호판은 디자인(전체 표시)과 달리 지금 규칙대로 가려서 보여줌 — 전체 표시로 바꿀지 확인 (4) 디자인 미완: 부모 일정 · 기록 · 마이 탭, 알림함(종 아이콘, 가족 전체 알림 목록 API 필요 여부), 차량 마무리 화면, 자녀 연결 확인 팝업. 진행 단계 문구 「합류 안내 발송」/「탑승 안내 발송」 통일 필요. C04 화면이 빠져 도움 요청(help-requests)을 쓰는 화면이 없음
- [사람 결정 2026-11-13] 최종디자인 11/13 반영: (1) 추천 장소 · 여기로 부르기는 서버가 임시 장소를 만드는 방식으로 확정 (2) 여기로 부르기는 장소 변경 + 합류 안내를 서버가 한 번에 처리로 확정 (3) 번호판(plateNumber)은 서버가 받지 않음 — 차대번호(VIN) 조회로 나중에 불러올 수 있어 이번에는 생략, 05 auth의 번호판 항목은 삭제함. 연결 확인 화면의 번호판 칸은 당분간 비어 있음
- [사람 결정 2026-11-13] (1) 이름 없는 길가 추천 후보는 name 「이름 없는 길」로 줌(place 항목 추가) (2) 학원은 가족마다 따로 둠: Academy에 familyId, (familyId, externalPlaceId) 유니크(schedule 항목 2건 추가). 기존 academy 행의 familyId 채우기 · 유니크 인덱스 교체는 배포 전 이전 작업 필요 (3) LINK-04 NEW로 만드는 보호자의 phone은 받지 않음 — null 허용으로 확정, 「자녀 외 phone 필수」 규칙의 예외 (4) 알림을 받는 「보호자」는 GUARDIAN · CO_GUARDIAN · DELEGATE 모두(pickup 항목 추가) (5) 최종디자인_11/ · 13(금).png · iLink_Project_Package/는 .gitignore에 넣어 커밋하지 않음 (6) 백업 stash 삭제함. 남은 미결: access · refresh 토큰 유효 기간, 운영 DB 중복 기본 장소 정리 기준
- [사람 결정 2026-11-13] (1) 운영 DB에 같은 가족 · 학원의 기본 장소가 2개 이상이면 가장 최근에 수정한 것만 기본으로 남긴다. 운영 배포 전에 중복을 조회하고 있으면 이 기준으로 정리(정리 SQL은 ops에 둘 것) (2) 토큰 유효 기간: access 30분(PT30M), refresh 14일(P14D). refresh 만료는 아직 구현 안 됨 — auth 항목 추가 필요(설계서 문구 수정 에이전트가 05를 고치는 중이라 끝난 뒤 추가)
- [설계서 문구 수정 2026-11-13 후속] (1) Swagger 「다음 세션부터」 문구가 src에 남음: FamilyApiDoc.java(약 53행) · ScheduleApiDoc.java:38 — 「바로 적용」으로 고칠 것(리뷰어 리팩터링) (2) 04 확인 필요 「멤버 해제 때 User 행 처리 … FK 때문에 못 지움」 근거가 D7(FK 없음 · 삭제 표시)과 다름 — user 삭제 표시 항목 처리 때 갱신 (3) 03 본문 약 102행 「도메인 코드 429 … 확인 필요」가 확인 필요 표(결정됨)와 어긋남 — 429 항목 처리 때 갱신 (4) 04 · 05 · 08 · 09 테이블의 「FK →」 표기는 02 관례(FK 없음)와 다르게 남음 (5) 06 체크 항목 「joinCallMode는 schedule 자체 enum」 — D1로 import가 허용돼 복사본 enum을 지울지 미정 (6) 02:20 의존 방향 17개는 src/main import grep으로 확인함(일치). PickupChildReadinessAdapter.hasLocationConsent는 늘 true임을 코드로 확인함
