package com.example.pickupservice.user.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.user.adapter.in.web.CallModeResponse;
import com.example.pickupservice.user.adapter.in.web.ChangeCallModeRequest;
import com.example.pickupservice.user.adapter.in.web.ChangeChildLocationRequest;
import com.example.pickupservice.user.adapter.in.web.ChildLocationResponse;
import com.example.pickupservice.user.adapter.in.web.FamilyResponse;
import com.example.pickupservice.user.adapter.in.web.MemberResponse;
import com.example.pickupservice.user.adapter.in.web.RegisterMemberRequest;
import com.example.pickupservice.user.adapter.in.web.RegisterMemberResponse;
import com.example.pickupservice.user.adapter.in.web.UpdateMemberRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "가족", description = "가족 멤버 · 합류 안내 방식 · 자녀 위치 사용")
public interface FamilyApiDoc {

	@Operation(summary = "내 가족 조회 (FAM-01)", description = "GUARDIAN · CO_GUARDIAN만. 연결 중인 차량이 없으면 vehicle은 null")
	@ApiResponse(responseCode = "200", description = "가족 · 차량 · 멤버 목록. members[]는 해제된 멤버를 빼고 userId 오름차순이며 grade(없으면 null)를 담는다. members[]마다 profileImageUrl(사진이 있으면 그 사진의 서명 URL, 없으면 기본 아바타 서명 URL, 저장소 설정이 없을 때만 null)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	FamilyResponse getMyFamily(@Parameter(hidden = true) AuthPrincipal principal);

	@Operation(summary = "멤버 등록 (FAM-02)", description = "GUARDIAN · CO_GUARDIAN만. role은 CHILD · CO_GUARDIAN · DELEGATE · TEACHER. name은 1~20자(이모지 · 제어문자 불가). phone은 국제 형식이고 CHILD만 생략 가능. grade는 선택이고 1~20자")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "등록된 멤버의 userId")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (공동 보호자가 공동 보호자를 추가)")
	@ApiResponse(responseCode = "409", description = "IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 · Idempotency-Key 없음, grade 0자 · 21자 이상이면 fields grade) · MEMBER_LIMIT_EXCEEDED(자녀 외 멤버가 이미 5명)")
	RegisterMemberResponse registerMember(@Parameter(hidden = true) AuthPrincipal principal, RegisterMemberRequest request);

	@Operation(summary = "멤버 등록 · 프로필 사진 포함 (FAM-02)", description = "multipart/form-data. request 파트는 JSON 등록 본문과 같고, profileImage 파트(JPEG · PNG · WEBP, 5MB 이하)는 선택이다. 0바이트 파트는 사진 없음으로 본다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "등록된 멤버의 userId")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (공동 보호자가 공동 보호자를 추가)")
	@ApiResponse(responseCode = "409", description = "IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED · MEMBER_LIMIT_EXCEEDED · PROFILE_IMAGE_INVALID(이미지가 아님)")
	@ApiResponse(responseCode = "413", description = "PROFILE_IMAGE_TOO_LARGE (사진이 5MB 초과)")
	@ApiResponse(responseCode = "503", description = "STORAGE_UNAVAILABLE (사진 저장소 업로드 실패)")
	RegisterMemberResponse registerMemberWithProfileImage(
			@Parameter(hidden = true) AuthPrincipal principal,
			@Parameter(description = "등록 본문(JSON)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) RegisterMemberRequest request,
			@Parameter(description = "프로필 사진(선택)") MultipartFile profileImage) throws IOException;

	@Operation(summary = "멤버 수정 (FAM-03)", description = "GUARDIAN · CO_GUARDIAN만. name · phone · role · grade 모두 선택이고 보낸 필드만 바뀐다(null도 그대로 둔다). grade는 1~20자. role 변경은 GUARDIAN만, GUARDIAN 자신의 role은 바꿀 수 없다")
	@ApiResponse(responseCode = "200", description = "수정된 멤버(grade 포함, 없으면 null)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN이 아닌 사람이 role 변경)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없거나 다른 가족의 멤버, 해제된 멤버)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (이름 · 전화번호 형식 오류, grade 0자 · 21자 이상이면 fields grade, GUARDIAN 자신의 role 변경, role GUARDIAN 지정, phone 없는 CHILD를 자녀 외 role로 변경) · MEMBER_LIMIT_EXCEEDED(CHILD를 자녀 외 role로 바꿀 때 자녀 외 멤버가 이미 5명)")
	MemberResponse updateMember(@Parameter(hidden = true) AuthPrincipal principal, long userId, UpdateMemberRequest request);

	@Operation(summary = "멤버 해제 (FAM-07)", description = "GUARDIAN만. 해제하면 그 멤버의 기기와 토큰도 폐기된다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "204", description = "해제됨")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없거나 다른 가족의 멤버, 이미 해제된 멤버)")
	@ApiResponse(responseCode = "409", description = "MEMBER_IN_ACTIVE_PICKUP · IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "CANNOT_REMOVE_GUARDIAN · VALIDATION_FAILED (Idempotency-Key 없음)")
	void removeMember(@Parameter(hidden = true) AuthPrincipal principal, long userId);

	@Operation(summary = "합류 안내 방식 변경 (SET-06)", description = "GUARDIAN · CO_GUARDIAN만. joinCallMode는 AUTO · GUARDIAN_CONFIRM. 바꾸면 settingsVersion이 1 오르고 진행 중인 세션에도 바로 적용된다")
	@ApiResponse(responseCode = "200", description = "바뀐 joinCallMode와 settingsVersion")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (joinCallMode 없음)")
	CallModeResponse changeCallMode(@Parameter(hidden = true) AuthPrincipal principal, ChangeCallModeRequest request);

	@Operation(summary = "자녀 위치 사용 변경 (SET-11)", description = "GUARDIAN · CO_GUARDIAN만. enabled가 true일 때만 픽업 중(호출 뒤) 자녀 위치를 받는다. 바꾸면 바로 적용되고 settingsVersion이 1 오른다")
	@ApiResponse(responseCode = "200", description = "바뀐 childLocationEnabled와 settingsVersion")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (enabled 없음)")
	ChildLocationResponse changeChildLocation(@Parameter(hidden = true) AuthPrincipal principal, ChangeChildLocationRequest request);

}
