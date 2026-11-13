package com.example.pickupservice.auth.adapter.in.web.doc;

import com.example.pickupservice.auth.adapter.in.web.AcceptInviteRequest;
import com.example.pickupservice.auth.adapter.in.web.AcceptInviteResponse;
import com.example.pickupservice.auth.adapter.in.web.InviteInfoResponse;
import com.example.pickupservice.auth.adapter.in.web.InviteResponse;
import com.example.pickupservice.auth.adapter.in.web.IssueInviteRequest;
import com.example.pickupservice.common.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "멤버 초대", description = "초대 발급 · 확인 · 수락")
public interface InviteApiDoc {

	@Operation(summary = "초대 발급 (FAM-04)", description = "GUARDIAN · CO_GUARDIAN. Idempotency-Key 필요. 24시간 · 1회용 초대 링크를 만들고 다시 발급하면 이전 링크는 폐기한다. 본문은 생략할 수 있고 replaceDevice 기본값은 false")
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "inviteUrl · expiresAt")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (GUARDIAN · CO_GUARDIAN이 아님, 대상 멤버가 GUARDIAN)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없거나 다른 가족의 멤버)")
	@ApiResponse(responseCode = "409", description = "DEVICE_ALREADY_CONNECTED (기기가 연결된 멤버에 replaceDevice false)")
	InviteResponse issueInvite(@Parameter(hidden = true) AuthPrincipal principal, long userId, IssueInviteRequest request);

	@Operation(summary = "초대 확인 (FAM-05)", description = "인증 없이 부른다. requiredConsents는 role별로 다르고 CHILD는 TERMS · PRIVACY · LOCATION · PUSH")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "inviterName · memberName · role · requiredConsents · expiresAt")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없는 초대)")
	@ApiResponse(responseCode = "409", description = "INVITE_ALREADY_USED (이미 수락한 초대)")
	@ApiResponse(responseCode = "410", description = "INVITE_EXPIRED (만료된 초대)")
	InviteInfoResponse getInvite(String token);

	@Operation(summary = "초대 수락 (FAM-06)", description = "인증 없이 부른다. Idempotency-Key 필요. requiredConsents를 모두 agreed true로 보내야 한다. 기기 생성 · 푸시 토큰 저장 · 토큰 발급을 한 번에 한다. platform은 ANDROID · IOS")
	@SecurityRequirements
	@Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "중복 요청 방지 키")
	@ApiResponse(responseCode = "201", description = "userId · familyId · role · accessToken · refreshToken")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (없는 초대)")
	@ApiResponse(responseCode = "409", description = "INVITE_ALREADY_USED (이미 수락한 초대)")
	@ApiResponse(responseCode = "410", description = "INVITE_EXPIRED (만료된 초대)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (pushToken 1~255자) · CONSENT_REQUIRED (필수 동의 누락)")
	AcceptInviteResponse acceptInvite(String token, AcceptInviteRequest request);

}
