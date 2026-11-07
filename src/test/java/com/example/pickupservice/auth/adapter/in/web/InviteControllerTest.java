package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.AcceptInviteCommand;
import com.example.pickupservice.auth.application.port.in.AcceptInviteResult;
import com.example.pickupservice.auth.application.port.in.ConsentAgreement;
import com.example.pickupservice.auth.application.port.in.InviteInfo;
import com.example.pickupservice.auth.application.port.in.InviteResult;
import com.example.pickupservice.auth.application.port.in.IssueInviteCommand;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class InviteControllerTest extends AuthWebTestSupport {

	private static final String ISSUE_PATH = "/v1/families/me/members/{userId}/invites";
	private static final String GET_PATH = "/v1/invites/{token}";
	private static final String ACCEPT_PATH = "/v1/invites/{token}/acceptance";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 102L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long CHILD_USER_ID = 131L;
	private static final long VEHICLE_ID = 77L;
	private static final String INVITE_TOKEN = "f3a9c1e7b5d24f6a8c0e2b4d6f8a1c3e";
	private static final String INVITE_URL = "https://app.example/invite/" + INVITE_TOKEN;
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-06T12:05:00Z");
	private static final String EXPIRES_AT_KST = "2026-11-06T21:05:00+09:00";
	private static final String INVITER_NAME = "김보호";
	private static final String MEMBER_NAME = "민준";
	private static final String CHILD_ACCESS_TOKEN = "eyJ.child-access-token";
	private static final String CHILD_REFRESH_TOKEN = "rt_child-refresh-token";
	private static final String PUSH_TOKEN = "fcm-child-push-token";
	private static final String ALL_CHILD_CONSENTS_JSON =
			"[{\"type\": \"TERMS\", \"agreed\": true}, {\"type\": \"PRIVACY\", \"agreed\": true}, "
					+ "{\"type\": \"LOCATION\", \"agreed\": true}, {\"type\": \"PUSH\", \"agreed\": true}]";
	private static final String LOCATION_NOT_AGREED_CONSENTS_JSON =
			"[{\"type\": \"TERMS\", \"agreed\": true}, {\"type\": \"PRIVACY\", \"agreed\": true}, "
					+ "{\"type\": \"LOCATION\", \"agreed\": false}, {\"type\": \"PUSH\", \"agreed\": true}]";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites는 201과 inviteUrl · expiresAt을 돌려준다")
	void issueInviteReturns201WithInviteUrlAndExpiresAt() throws Exception {
		given(issueInviteUseCase.issueInvite(any())).willReturn(new InviteResult(INVITE_URL, EXPIRES_AT));

		mockMvc.perform(issueRequest(CHILD_USER_ID, guardianToken(), "{\"replaceDevice\": false}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.inviteUrl").value(INVITE_URL))
				.andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT_KST));
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites는 토큰의 familyId · 경로의 userId · 본문의 replaceDevice를 IssueInviteCommand에 담아 넘긴다")
	void issueInvitePassesFamilyIdUserIdAndReplaceDeviceToUseCase() throws Exception {
		given(issueInviteUseCase.issueInvite(any())).willReturn(new InviteResult(INVITE_URL, EXPIRES_AT));

		mockMvc.perform(issueRequest(CHILD_USER_ID, guardianToken(), "{\"replaceDevice\": true}"))
				.andExpect(status().isCreated());

		then(issueInviteUseCase).should().issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_USER_ID, true));
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites에 replaceDevice가 없는 본문을 보내면 replaceDevice false를 넘긴다")
	void issueInviteWithoutReplaceDeviceFieldPassesFalse() throws Exception {
		given(issueInviteUseCase.issueInvite(any())).willReturn(new InviteResult(INVITE_URL, EXPIRES_AT));

		mockMvc.perform(issueRequest(CHILD_USER_ID, guardianToken(), "{}"))
				.andExpect(status().isCreated());

		then(issueInviteUseCase).should().issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_USER_ID, false));
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites에 본문 없이 보내면 201을 돌려주고 replaceDevice false를 넘긴다")
	void issueInviteWithoutBodyPassesFalse() throws Exception {
		given(issueInviteUseCase.issueInvite(any())).willReturn(new InviteResult(INVITE_URL, EXPIRES_AT));

		mockMvc.perform(post(ISSUE_PATH, CHILD_USER_ID)
						.header(HttpHeaders.AUTHORIZATION, bearer(guardianToken()))
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.inviteUrl").value(INVITE_URL));

		then(issueInviteUseCase).should().issueInvite(new IssueInviteCommand(FAMILY_ID, CHILD_USER_ID, false));
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites는 CO_GUARDIAN 토큰으로도 201을 돌려준다")
	void issueInviteWithCoGuardianTokenReturns201() throws Exception {
		given(issueInviteUseCase.issueInvite(any())).willReturn(new InviteResult(INVITE_URL, EXPIRES_AT));
		String coGuardianToken = accessTokenFixture.user(CO_GUARDIAN_USER_ID, FAMILY_ID, Role.CO_GUARDIAN);

		mockMvc.perform(issueRequest(CHILD_USER_ID, coGuardianToken, "{}"))
				.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites가 기기 연결 멤버에 replaceDevice false면 409 DEVICE_ALREADY_CONNECTED를 돌려준다")
	void issueInviteForConnectedMemberWithoutReplaceReturns409() throws Exception {
		given(issueInviteUseCase.issueInvite(any()))
				.willThrow(BusinessException.of(
						HttpStatus.CONFLICT, "DEVICE_ALREADY_CONNECTED", "이미 기기가 연결된 멤버입니다"));

		mockMvc.perform(issueRequest(CHILD_USER_ID, guardianToken(), "{\"replaceDevice\": false}"))
				.andExpect(commonError(HttpStatus.CONFLICT, "DEVICE_ALREADY_CONNECTED"));
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void issueInviteWithDelegateTokenReturns403() throws Exception {
		String delegateToken = accessTokenFixture.user(DELEGATE_USER_ID, FAMILY_ID, Role.DELEGATE);

		mockMvc.perform(issueRequest(CHILD_USER_ID, delegateToken, "{}"))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(issueInviteUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites를 VEHICLE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void issueInviteWithVehicleTokenReturns403() throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		mockMvc.perform(issueRequest(CHILD_USER_ID, vehicleToken, "{}"))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(issueInviteUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/members/{userId}/invites를 인증 헤더 없이 부르면 401 UNAUTHORIZED를 돌려준다")
	void issueInviteWithoutAuthorizationHeaderReturns401() throws Exception {
		mockMvc.perform(post(ISSUE_PATH, CHILD_USER_ID)
						.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));

		then(issueInviteUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/invites/{token}은 인증 헤더 없이 200과 inviterName · memberName · role · requiredConsents · expiresAt을 돌려준다")
	void getInviteWithoutAuthorizationHeaderReturns200() throws Exception {
		given(getInviteUseCase.getInvite(INVITE_TOKEN)).willReturn(childInviteInfo());

		mockMvc.perform(get(GET_PATH, INVITE_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inviterName").value(INVITER_NAME))
				.andExpect(jsonPath("$.memberName").value(MEMBER_NAME))
				.andExpect(jsonPath("$.role").value("CHILD"))
				.andExpect(jsonPath("$.requiredConsents", contains("TERMS", "PRIVACY", "LOCATION", "PUSH")))
				.andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT_KST));
	}

	@Test
	@DisplayName("GET /v1/invites/{token}은 경로의 token을 GetInviteUseCase.getInvite에 그대로 넘긴다")
	void getInvitePassesPathTokenToUseCase() throws Exception {
		given(getInviteUseCase.getInvite(anyString())).willReturn(childInviteInfo());

		mockMvc.perform(get(GET_PATH, INVITE_TOKEN))
				.andExpect(status().isOk());

		then(getInviteUseCase).should().getInvite(INVITE_TOKEN);
	}

	@Test
	@DisplayName("GET /v1/invites/{token}에 만료된 access 토큰이 Authorization 헤더로 붙어 와도 401이 아니라 200을 돌려준다")
	void getInviteWithExpiredAccessTokenHeaderStillReturns200() throws Exception {
		given(getInviteUseCase.getInvite(INVITE_TOKEN)).willReturn(childInviteInfo());
		String expiredAccessToken = accessTokenFixture.expiredUser(CHILD_USER_ID, FAMILY_ID, Role.CHILD);

		mockMvc.perform(get(GET_PATH, INVITE_TOKEN)
						.header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberName").value(MEMBER_NAME));
	}

	@Test
	@DisplayName("GET /v1/invites/{token}이 사용된 초대면 409 INVITE_ALREADY_USED를 돌려준다")
	void getInviteAlreadyUsedReturns409() throws Exception {
		given(getInviteUseCase.getInvite(INVITE_TOKEN))
				.willThrow(BusinessException.of(HttpStatus.CONFLICT, "INVITE_ALREADY_USED", "이미 수락한 초대입니다"));

		mockMvc.perform(get(GET_PATH, INVITE_TOKEN))
				.andExpect(commonError(HttpStatus.CONFLICT, "INVITE_ALREADY_USED"));
	}

	@Test
	@DisplayName("GET /v1/invites/{token}이 만료된 초대면 410 INVITE_EXPIRED를 돌려준다")
	void getInviteExpiredReturns410() throws Exception {
		given(getInviteUseCase.getInvite(INVITE_TOKEN))
				.willThrow(BusinessException.of(HttpStatus.GONE, "INVITE_EXPIRED", "만료된 초대입니다"));

		mockMvc.perform(get(GET_PATH, INVITE_TOKEN))
				.andExpect(commonError(HttpStatus.GONE, "INVITE_EXPIRED"));
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance는 인증 헤더 없이 201과 userId · familyId · role · accessToken · refreshToken을 돌려준다")
	void acceptInviteWithoutAuthorizationHeaderReturns201WithTokens() throws Exception {
		given(acceptInviteUseCase.acceptInvite(eq(INVITE_TOKEN), any())).willReturn(acceptInviteResult());

		mockMvc.perform(acceptRequest(acceptBody(ALL_CHILD_CONSENTS_JSON, "ANDROID", PUSH_TOKEN)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(CHILD_USER_ID))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$.role").value("CHILD"))
				.andExpect(jsonPath("$.accessToken").value(CHILD_ACCESS_TOKEN))
				.andExpect(jsonPath("$.refreshToken").value(CHILD_REFRESH_TOKEN));
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance는 경로의 token과 본문의 consents · platform · pushToken을 AcceptInviteCommand에 담아 넘긴다")
	void acceptInvitePassesTokenAndBodyToUseCase() throws Exception {
		given(acceptInviteUseCase.acceptInvite(anyString(), any())).willReturn(acceptInviteResult());

		mockMvc.perform(acceptRequest(acceptBody(ALL_CHILD_CONSENTS_JSON, "IOS", PUSH_TOKEN)))
				.andExpect(status().isCreated());

		then(acceptInviteUseCase).should().acceptInvite(INVITE_TOKEN, new AcceptInviteCommand(
				List.of(
						new ConsentAgreement(ConsentType.TERMS, true),
						new ConsentAgreement(ConsentType.PRIVACY, true),
						new ConsentAgreement(ConsentType.LOCATION, true),
						new ConsentAgreement(ConsentType.PUSH, true)),
				Platform.IOS,
				PUSH_TOKEN));
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance는 pushToken 없이도 201을 돌려주고 pushToken null을 넘긴다")
	void acceptInviteWithoutPushTokenPassesNull() throws Exception {
		given(acceptInviteUseCase.acceptInvite(anyString(), any())).willReturn(acceptInviteResult());

		mockMvc.perform(acceptRequest(acceptBody(ALL_CHILD_CONSENTS_JSON, "ANDROID", null)))
				.andExpect(status().isCreated());

		then(acceptInviteUseCase).should().acceptInvite(INVITE_TOKEN, new AcceptInviteCommand(
				List.of(
						new ConsentAgreement(ConsentType.TERMS, true),
						new ConsentAgreement(ConsentType.PRIVACY, true),
						new ConsentAgreement(ConsentType.LOCATION, true),
						new ConsentAgreement(ConsentType.PUSH, true)),
				Platform.ANDROID,
				null));
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다")
	void acceptInviteWithoutRequiredConsentReturns422ConsentRequired() throws Exception {
		given(acceptInviteUseCase.acceptInvite(anyString(), any()))
				.willThrow(BusinessException.of(
						HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED", "필수 약관에 동의해 주세요"));

		mockMvc.perform(acceptRequest(acceptBody(LOCATION_NOT_AGREED_CONSENTS_JSON, "ANDROID", PUSH_TOKEN)))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED"));
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance에 platform VEHICLE을 보내면 422 VALIDATION_FAILED와 fields의 platform을 돌려준다")
	void acceptInviteWithVehiclePlatformReturns422WithPlatformField() throws Exception {
		mockMvc.perform(acceptRequest(acceptBody(ALL_CHILD_CONSENTS_JSON, "VEHICLE", PUSH_TOKEN)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("platform"));

		then(acceptInviteUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance에 256자 pushToken을 보내면 422 VALIDATION_FAILED와 fields의 pushToken을 돌려준다")
	void acceptInviteWith256CharPushTokenReturns422WithPushTokenField() throws Exception {
		mockMvc.perform(acceptRequest(acceptBody(ALL_CHILD_CONSENTS_JSON, "ANDROID", "a".repeat(256))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("pushToken"));

		then(acceptInviteUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/invites/{token}/acceptance에 만료된 access 토큰이 Authorization 헤더로 붙어 와도 401이 아니라 201을 돌려준다")
	void acceptInviteWithExpiredAccessTokenHeaderStillReturns201() throws Exception {
		given(acceptInviteUseCase.acceptInvite(anyString(), any())).willReturn(acceptInviteResult());
		String expiredAccessToken = accessTokenFixture.expiredUser(CHILD_USER_ID, FAMILY_ID, Role.CHILD);

		mockMvc.perform(acceptRequest(acceptBody(ALL_CHILD_CONSENTS_JSON, "ANDROID", PUSH_TOKEN))
						.header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(CHILD_USER_ID));
	}

	private String guardianToken() {
		return accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN);
	}

	private static InviteInfo childInviteInfo() {
		return new InviteInfo(
				INVITER_NAME,
				MEMBER_NAME,
				Role.CHILD,
				List.of(ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.LOCATION, ConsentType.PUSH),
				EXPIRES_AT);
	}

	private static AcceptInviteResult acceptInviteResult() {
		return new AcceptInviteResult(CHILD_USER_ID, FAMILY_ID, Role.CHILD, CHILD_ACCESS_TOKEN, CHILD_REFRESH_TOKEN);
	}

	private static MockHttpServletRequestBuilder issueRequest(long userId, String accessToken, String body) {
		return post(ISSUE_PATH, userId)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private static String acceptBody(String consentsJson, String platform, String pushToken) {
		String pushTokenField = pushToken == null ? "" : ", \"pushToken\": \"" + pushToken + "\"";
		return "{\"consents\": " + consentsJson + ", \"platform\": \"" + platform + "\"" + pushTokenField + "}";
	}

	private static MockHttpServletRequestBuilder acceptRequest(String body) {
		return post(ACCEPT_PATH, INVITE_TOKEN)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

}
