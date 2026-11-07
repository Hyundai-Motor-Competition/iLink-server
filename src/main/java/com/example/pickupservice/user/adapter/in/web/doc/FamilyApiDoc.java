package com.example.pickupservice.user.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.user.adapter.in.web.CallModeResponse;
import com.example.pickupservice.user.adapter.in.web.ChangeCallModeRequest;
import com.example.pickupservice.user.adapter.in.web.FamilyResponse;
import com.example.pickupservice.user.adapter.in.web.MemberResponse;
import com.example.pickupservice.user.adapter.in.web.RegisterMemberRequest;
import com.example.pickupservice.user.adapter.in.web.RegisterMemberResponse;
import com.example.pickupservice.user.adapter.in.web.UpdateMemberRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "가족", description = "가족 멤버 · 합류 안내 방식")
public interface FamilyApiDoc {

	@Operation(summary = "내 가족 조회 (FAM-01)", description = "GUARDIAN · CO_GUARDIAN만. 연결 중인 차량이 없으면 vehicle은 null")
	@ApiResponse(responseCode = "200", description = "가족 · 차량 · 멤버 목록")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	FamilyResponse getMyFamily(@Parameter(hidden = true) AuthPrincipal principal);

	@Operation(summary = "멤버 등록 (FAM-02)", description = "GUARDIAN · CO_GUARDIAN만. role은 CHILD · CO_GUARDIAN · DELEGATE · TEACHER. name은 1~20자(이모지 · 제어문자 불가). phone은 국제 형식이고 CHILD만 생략 가능")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "등록된 멤버의 userId")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (공동 보호자가 공동 보호자를 추가)")
	@ApiResponse(responseCode = "409", description = "IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (입력값 · Idempotency-Key 없음) · MEMBER_LIMIT_EXCEEDED(자녀 외 멤버가 이미 5명)")
	RegisterMemberResponse registerMember(@Parameter(hidden = true) AuthPrincipal principal, RegisterMemberRequest request);

	@Operation(summary = "멤버 수정 (FAM-03)", description = "GUARDIAN · CO_GUARDIAN만. name · phone · role 모두 선택이고 보낸 필드만 바뀐다. role 변경은 GUARDIAN만, GUARDIAN 자신의 role은 바꿀 수 없다")
	@ApiResponse(responseCode = "200", description = "수정된 멤버")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN이 아닌 사람이 role 변경)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없거나 다른 가족의 멤버)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (이름 · 전화번호 형식 오류, GUARDIAN 자신의 role 변경)")
	MemberResponse updateMember(@Parameter(hidden = true) AuthPrincipal principal, long userId, UpdateMemberRequest request);

	@Operation(summary = "멤버 해제 (FAM-07)", description = "GUARDIAN만. 해제하면 그 멤버의 기기와 토큰도 폐기된다")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "204", description = "해제됨")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없거나 다른 가족의 멤버)")
	@ApiResponse(responseCode = "409", description = "MEMBER_IN_ACTIVE_PICKUP · IDEMPOTENCY_KEY_REUSED")
	@ApiResponse(responseCode = "422", description = "CANNOT_REMOVE_GUARDIAN · VALIDATION_FAILED (Idempotency-Key 없음)")
	void removeMember(@Parameter(hidden = true) AuthPrincipal principal, long userId);

	@Operation(summary = "합류 안내 방식 변경 (SET-06)", description = "GUARDIAN · CO_GUARDIAN만. joinCallMode는 AUTO · GUARDIAN_CONFIRM. 바꾸면 settingsVersion이 1 오르고 다음 세션부터 적용된다")
	@ApiResponse(responseCode = "200", description = "바뀐 joinCallMode와 settingsVersion")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (joinCallMode 없음)")
	CallModeResponse changeCallMode(@Parameter(hidden = true) AuthPrincipal principal, ChangeCallModeRequest request);

}
