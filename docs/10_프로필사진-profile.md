# 10. 프로필 사진 (profile)

보호자 · 자녀 등 가족 멤버의 프로필 사진. 가입(LINK-04)과 멤버 등록(FAM-02) 때 사진을 선택으로 함께 받고, 안 보내면 기본 아바타를 쓴다. 사진은 MinIO 비공개 버킷에 두고, 가족 조회(FAM-01) 때 몇 분짜리 서명 URL로 보여 준다(자녀 얼굴 사진이 들어갈 수 있어 공개 URL을 쓰지 않음, 사람 결정 2026-11-10).

| 항목 | 내용 |
|---|---|
| 범위 | User.profileImageKey 컬럼. LINK-04 · FAM-02를 multipart로도 받음(JSON도 그대로 받음). FAM-01 members[].profileImageUrl. 사진 검증 · 저장 · 서명 URL(ProfileImageStoragePort, MinIO 어댑터). multipart 요청의 Idempotency 해시 |
| 의존 | 03_공유모듈-shared(오류 형식 · Idempotency-Key · JSON 전용 설정), 04_사용자-user(User · FAM-01 · FAM-02), 05_인증-auth(LINK-04) |

## 구조

```text
user
├─ adapter
│  ├─ in/web               FamilyController FAM-02 multipart 받기, FAM-01 응답에 profileImageUrl
│  └─ out/storage          MinioProfileImageStorage(ProfileImageStoragePort 구현), ProfileImageProperties
├─ application/port/in     RegisterMemberCommand · CreateGuardianCommand에 ProfileImage(선택, null이면 사진 없음). 웹 어댑터가 ProfileImage.of로 413 · 422를 먼저 판정해 넘긴다
├─ application/port/in     ChangeProfileImageUseCase(LINK-04 RELINK용 사진 교체)
├─ application/port/out    ProfileImageStoragePort(put · delete · presignedUrl · defaultUrl)
└─ domain                  User.profileImageKey

auth
├─ adapter/in/web          VehicleLinkCodeController LINK-04 multipart 받기 → ApproveLinkCommand에 사진 전달
└─ application/service     ApproveLinkService: NEW는 CreateGuardianCommand에, RELINK는 ChangeProfileImageUseCase에 사진을 넘긴다

common
├─ image                   ProfileImage · ImageFormat · ProfileImageInvalidException · ProfileImageTooLargeException · StorageUnavailableException(형식 · 크기 검증, 확장자 판정). auth와 user 웹이 같이 쓰므로 common에 둔다(auth는 user.domain을 import하지 않는 구조 규칙)
├─ idempotency             RequestHasher multipart 해시(아래 「Idempotency 해시」)
└─ web · error             ProfileImageParts.toProfileImage(MultipartFile)(null · 0바이트 → 사진 없음, 아니면 ProfileImage.of), multipart 허용(두 API만), 413 · 422 · 503 오류 코드

test
└─ fixture                 InMemoryProfileImageStorage(테스트용 ProfileImageStoragePort, @Primary)
```

## 테이블

사용자 (User) — 컬럼 추가

| 컬럼 | 타입 | 키 · 제약 | 설명 |
|---|---|---|---|
| profileImageKey | VARCHAR(200) | NULL | MinIO 객체 키 `profiles/{UUID}.{jpg·png·webp}`. NULL이면 기본 아바타 |

- 테이블 이름은 `users`(UserEntity). dev는 ddl-auto update라 자동으로 붙고, prod는 `ALTER TABLE users ADD COLUMN profile_image_key VARCHAR(200) NULL;`을 기동 전에 실행한다(ddl-auto validate).

## API 명세

| ID | Method · 경로 | 호출 | Idempotency-Key | 요청 → 응답 | 오류 |
|---|---|---|---|---|---|
| LINK-04 | POST /v1/vehicle-links | 보호자 앱(인증 없음) | 필요 | 지금 JSON 그대로, 또는 multipart/form-data: `request`(application/json, 지금 본문과 같음) + `profileImage`(선택, 이미지 파일) → 201 지금과 같음 | 지금 오류 + 413 PROFILE_IMAGE_TOO_LARGE, 422 PROFILE_IMAGE_INVALID, 503 STORAGE_UNAVAILABLE |
| FAM-02 | POST /v1/families/me/members | GUARDIAN · CO_GUARDIAN | 필요 | 지금 JSON 그대로, 또는 multipart/form-data: `request`(application/json) + `profileImage`(선택) → 201 userId | 지금 오류 + 413 PROFILE_IMAGE_TOO_LARGE, 422 PROFILE_IMAGE_INVALID, 503 STORAGE_UNAVAILABLE |
| FAM-01 | GET /v1/families/me | GUARDIAN · CO_GUARDIAN | 불필요 | — → 200 지금 필드 + members[].profileImageUrl(서명 URL 또는 기본 아바타 서명 URL, 저장소 설정이 없으면 null). FAM-03(멤버 수정) 응답은 바꾸지 않는다 | 공통 |

응답 예시
- FAM-01 members[]: `{"userId":131,"role":"CHILD","name":"민준","phone":null,"deviceStatus":"INVITE_PENDING","profileImageUrl":"https://img-dev.chisanw.com/ilink/profiles/3f6c...webp?X-Amz-Expires=600&X-Amz-Signature=..."}`

## 핵심 로직

### 사진 받기
- 두 API는 `application/json`(지금 클라이언트)과 `multipart/form-data`를 모두 받는다. 그 밖의 Content-Type은 지금처럼 415 UNSUPPORTED_MEDIA_TYPE. multipart는 이 두 API에서만 허용하고 나머지 API는 JSON 전용을 유지한다.
- multipart에서 `request` 파트가 없거나 JSON이 틀리면 지금 JSON 본문 오류와 같게 처리한다(400 INVALID_REQUEST · 422 VALIDATION_FAILED). `profileImage` 파트는 선택이고, 비어 있는(0바이트) 파트는 없는 것으로 본다.
- 크기: 5MB(5,242,880바이트) 이하. 넘으면 413 PROFILE_IMAGE_TOO_LARGE. 요청 전체 한도는 6MB(Spring multipart max-file-size 5MB · max-request-size 6MB). 한도를 넘어 컨트롤러 전에 난 예외도 413 PROFILE_IMAGE_TOO_LARGE로 같은 오류 본문을 쓴다.
- 형식: 파일 앞 바이트(매직 넘버)로 판정한다. JPEG(FF D8 FF) · PNG(89 50 4E 47 0D 0A 1A 0A) · WEBP(RIFF....WEBP). 그 밖이면 422 PROFILE_IMAGE_INVALID. 파일 이름 · 파트 Content-Type은 믿지 않는다.
- 저장 키: `profiles/{UUID}.{jpg|png|webp}`. 객체 Content-Type은 판정한 형식으로 넣는다.

### 저장 순서 · 실패
- LINK-04 · FAM-02의 기존 검증(코드 · 동의 · 이름 · 권한 · 멤버 한도 등)을 모두 통과한 뒤 업로드한다. 검증에서 4xx로 끝나면 저장소에는 아무것도 올리지 않는다.
- 업로드는 서비스 트랜잭션 안에서 User 저장 직전에 한다. 업로드가 실패하면(연결 · 인증 · 버킷 오류) 503 STORAGE_UNAVAILABLE이고 트랜잭션을 되돌려 사용자 · 가족 · 기기가 생기지 않는다.
- 업로드는 됐는데 커밋이 실패해 남은 객체는 지우지 않는다(확인 필요 사항).
- 저장소 설정이 없을 때(MINIO_ENDPOINT가 빔, 로컬 개발): 사진 없는 요청은 지금과 같이 처리하고, 사진이 있는 요청은 503 STORAGE_UNAVAILABLE.
- LINK-04 RELINK: 사진이 있으면 그 보호자의 profileImageKey를 새 키로 바꾸고 이전 객체를 지운다(지우기 실패는 로그만 남김). 사진이 없으면 그대로 둔다.
- 이전 객체 삭제는 트랜잭션이 커밋된 뒤에 한다. 커밋 전에 지우면 뒤 단계(기기 등록 등)나 커밋이 실패해 DB가 이전 키로 되돌아갈 때 사진이 깨진다. 커밋이 실패하면 이전 객체를 지우지 않는다(새로 올린 객체는 남음, 「확인 필요 사항」). 트랜잭션 밖에서 불리면 바로 지운다(팀장 결정 2026-11-10).

### 보여 주기 (FAM-01)
- profileImageKey가 있으면 그 객체, 없으면 기본 아바타 객체(`defaults/avatar.png`, 운영자가 미리 올림)의 서명 GET URL을 만든다. 유효 시간은 설정 pickup.profile.url-ttl(기본 PT10M).
- 서명 URL은 공개 주소(MINIO_PUBLIC_ENDPOINT, dev는 https://img-dev.chisanw.com)로 만든다. 업로드는 클러스터 안 주소(MINIO_ENDPOINT)로 한다. 서명은 네트워크 호출 없이 만든다.
- 저장소 설정이 없으면 profileImageUrl은 null이다. 앱은 null이면 앱에 넣어 둔 기본 아바타를 보인다.

### Idempotency 해시 (multipart)
- multipart 경계(boundary)는 요청마다 바뀔 수 있어 본문 바이트를 그대로 해시하면 같은 요청의 재시도가 409 IDEMPOTENCY_KEY_REUSED가 된다.
- multipart 요청의 해시 = SHA-256(HTTP 메서드 + " " + 경로 + "\n" + `request` 파트 바이트 + "\n" + (`profileImage` 파트가 있고 0바이트가 아니면 그 바이트의 SHA-256 16진수, 없거나 0바이트면 "-")). JSON 요청은 지금 규칙 그대로다.
- 같은 키 · 같은 내용의 재시도는 처음 응답을 그대로 돌려주고 사진을 다시 올리지 않는다(필터가 컨트롤러를 부르지 않음).

### 설정 (환경 변수)
| 이름 | 설명 |
|---|---|
| MINIO_ENDPOINT | 업로드용 클러스터 안 주소. dev `http://minio.designmodu-dev.svc.cluster.local:9000`. 비면 저장소 없음 |
| MINIO_PUBLIC_ENDPOINT | 서명 URL용 공개 주소. dev `https://img-dev.chisanw.com` |
| MINIO_BUCKET | 버킷. dev `ilink`(비공개) |
| server.tomcat.max-swallow-size (application.yml) | 10MB. 한도를 넘은 요청의 남은 본문이 이보다 크면 Tomcat이 413을 보내기 전에 연결을 끊는다(기본 2MB). 요청 한도 6MB보다 크게 둔다 |
| MINIO_ACCESS_KEY · MINIO_SECRET_KEY | ilink 버킷에만 권한이 있는 전용 계정(루트 계정을 쓰지 않음). Secret api에 둔다 |

### 시그니처
```java
// user.application.port.out
public interface ProfileImageStoragePort {
    void put(String key, byte[] bytes, String contentType);   // 실패 시 StorageUnavailableException
    void delete(String key);                                   // 실패해도 예외를 던지지 않고 로그만
    Optional<String> presignedUrl(String key);                 // 저장소 설정이 없으면 Optional.empty()
    Optional<String> defaultUrl();                             // defaults/avatar.png 서명 URL
    boolean configured();
}

// user.application.port.in (auth의 LINK-04 RELINK가 부름)
public interface ChangeProfileImageUseCase {
    void changeProfileImage(ChangeProfileImageCommand command);   // 사진이 null이면 아무것도 안 함. put → 키 변경 → saveUser → 이전 키 delete
}
public record ChangeProfileImageCommand(long userId, ProfileImage profileImage) {}

// common.image
public record ProfileImage(byte[] bytes, ImageFormat format) {
    public static ProfileImage of(byte[] bytes);               // 5MB 초과 ProfileImageTooLargeException, 형식 밖 ProfileImageInvalidException
    public String newKey();                                    // profiles/{UUID}.{ext}
}
```

## 테스트 케이스
- [x] [기반] ProfileImage.of는 JPEG · PNG · WEBP 매직 넘버를 판정해 형식과 확장자(jpg · png · webp)를 정한다
- [x] [기반] ProfileImage.of는 매직 넘버가 JPEG · PNG · WEBP가 아니면 ProfileImageInvalidException을 던진다
- [x] [기반] ProfileImage.of는 5,242,880바이트를 넘으면 ProfileImageTooLargeException을 던지고 딱 5,242,880바이트는 받는다
- [x] [기반] ProfileImage.newKey는 profiles/{UUID}.{ext} 형식의 키를 만든다
- [x] [기반] 테스트 픽스처 InMemoryProfileImageStorage는 put한 키와 바이트 · Content-Type을 기록하고 presignedUrl로 키가 담긴 URL을 돌려준다
- [x] [persistence] User의 profileImageKey는 NULL로 저장되고 값을 넣으면 다시 읽었을 때 같은 값이다
- [x] [서비스] 멤버 등록은 사진이 있으면 검증을 모두 통과한 뒤 저장소에 올리고 User.profileImageKey에 그 키를 저장한다
- [x] [서비스] 멤버 등록이 권한 · 멤버 한도 검증에서 실패하면 저장소에 아무것도 올리지 않는다
- [x] [서비스] 멤버 등록은 저장소 업로드가 실패하면 StorageUnavailableException으로 끝나고 User를 만들지 않는다
- [x] [서비스] 멤버 등록은 사진이 없으면 저장소를 부르지 않고 profileImageKey를 비워 둔다
- [x] [서비스] 보호자 생성(LINK-04 NEW)은 사진이 있으면 업로드한 키를 보호자 User에 저장한다
- [x] [서비스] 연결 승인이 코드 · 동의 검증에서 실패하면 저장소에 아무것도 올리지 않는다
- [x] [서비스] 연결 승인 RELINK에 사진이 있으면 보호자의 profileImageKey를 새 키로 바꾸고 이전 객체를 지운다
- [x] [서비스] 연결 승인 RELINK에 사진이 없으면 보호자의 profileImageKey를 그대로 둔다
- [x] [서비스] 저장소 설정이 없을 때 사진이 있는 등록 요청은 StorageUnavailableException으로 끝난다
- [x] [서비스] 가족 조회는 profileImageKey가 있는 멤버에게 그 키의 서명 URL을, 없는 멤버에게 기본 아바타 서명 URL을 준다
- [x] [서비스] 가족 조회는 저장소 설정이 없으면 모든 멤버의 profileImageUrl을 null로 준다
- [x] [web] POST /v1/families/me/members는 multipart의 request 파트와 profileImage 파트를 받아 201과 userId를 돌려준다
- [x] [web] POST /v1/families/me/members는 profileImage 파트 없이 multipart로 보내도 201을 돌려준다
- [x] [web] POST /v1/families/me/members는 지금처럼 application/json 본문도 받아 201을 돌려준다
- [x] [web] POST /v1/families/me/members의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다
- [x] [web] POST /v1/families/me/members의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를 돌려준다
- [x] [web] POST /v1/families/me/members는 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다
- [x] [web] POST /v1/vehicle-links는 multipart의 request 파트와 profileImage 파트를 받아 201과 토큰을 돌려준다
- [x] [web] POST /v1/vehicle-links는 지금처럼 application/json 본문도 받아 201을 돌려준다
- [x] [web] POST /v1/vehicle-links의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다
- [x] [web] GET /v1/families/me는 members[]마다 profileImageUrl을 돌려준다
- [x] [web] multipart는 POST /v1/vehicle-links와 POST /v1/families/me/members에서만 받고 다른 API에 multipart를 보내면 415 UNSUPPORTED_MEDIA_TYPE을 돌려준다
- [x] [web] 요청 전체가 multipart 한도(6MB)를 넘어 컨트롤러 전에 거절돼도 413 PROFILE_IMAGE_TOO_LARGE 오류 본문을 쓴다
- [x] [통합] multipart 요청의 Idempotency 해시는 경계 문자열이 달라도 request 파트와 사진 바이트가 같으면 같다
- [x] [통합] multipart 요청의 Idempotency 해시는 사진 바이트가 다르면 달라진다
- [x] [통합] 같은 Idempotency-Key로 경계만 다른 multipart 재시도는 처음 응답을 돌려주고 사진을 다시 올리지 않는다
- [x] [통합] MinioProfileImageStorage.presignedUrl은 MINIO_PUBLIC_ENDPOINT 주소로 X-Amz-Expires가 url-ttl 초인 URL을 만든다
- [x] [통합] MinioProfileImageStorage는 MINIO_ENDPOINT가 비면 configured()가 false이고 presignedUrl · defaultUrl이 비어 있다
- [x] [통합] 실제 서블릿 컨테이너(RANDOM_PORT)에서 Idempotency 필터를 거친 POST /v1/families/me/members multipart 요청이 request · profileImage 파트를 읽어 201을 돌려준다
- [x] [서비스] 연결 승인 RELINK의 이전 사진 객체는 트랜잭션이 커밋된 뒤에 지우고 커밋이 실패하면 지우지 않는다
- [x] [web] POST /v1/families/me/members multipart에 request 파트가 없거나 JSON이 틀리면 400 INVALID_REQUEST를, request 파트 검증에 실패하면 422 VALIDATION_FAILED를 돌려준다
- [x] [web] POST /v1/vehicle-links multipart의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를, 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다
- [x] [통합] multipart 요청의 Idempotency 해시는 profileImage 파트가 없거나 0바이트면 사진 자리에 "-"를 넣어 계산한다

## 확인 필요 사항

| 항목 | 현재 가정 |
|---|---|
| 사진 변경 · 삭제 API | 이번 범위 밖. 등록(LINK-04 · FAM-02)과 RELINK 때만 바뀐다. FAM-03 · 별도 API는 추후 |
| 커밋 실패로 남은 객체 | 지우지 않는다. 업로드 뒤 같은 트랜잭션의 다음 단계가 실패한 경우(예: LINK-04 NEW에서 saveVehicle이 동시 승인으로 409 VEHICLE_ALREADY_LINKED)도 같다. 업로드는 검증 뒤 · 저장 직전이라 드물다. 버킷 수명 주기 규칙이나 정리 작업은 추후 |
| FAM-07 멤버 해제 때 사진 | 객체를 지우지 않는다(행을 지우지 않는 04 규칙과 같게) |
| 이미지 리사이즈 · EXIF 제거 | 하지 않는다. 앱이 올리기 전에 줄이고 위치 정보를 지운다 |
| FAM-01 말고 사진을 보여 줄 곳(SES-03 · SES-10 · 차량 화면) | 이번 범위 밖 |
| 저장소 | 홈서버 designmodu-dev 네임스페이스의 MinIO를 같이 쓴다(버킷 · 계정은 ilink 전용). 그 MinIO가 없어지면 사진이 안 보인다 |
