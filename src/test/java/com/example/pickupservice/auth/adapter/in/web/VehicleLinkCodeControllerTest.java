package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.adapter.in.web.doc.VehicleLinkCodeApiDoc;
import com.example.pickupservice.auth.application.port.in.ApproveLinkCommand;
import com.example.pickupservice.auth.application.port.in.ConsentAgreement;
import com.example.pickupservice.auth.application.port.in.IssueLinkCodeCommand;
import com.example.pickupservice.auth.application.port.in.LinkCodeInfo;
import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.auth.application.port.in.LinkStatus;
import com.example.pickupservice.auth.application.port.in.LinkStatusResult;
import com.example.pickupservice.auth.application.port.in.VehicleLinkResult;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.image.ImageFormat;
import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.common.logging.RequestLogAttributes;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class VehicleLinkCodeControllerTest extends AuthWebTestSupport {

	private static final String ISSUE_PATH = "/v1/vehicle-link-codes";
	private static final String STATUS_PATH = "/v1/vehicle-link-codes/{codeId}/status";
	private static final String BY_CODE_PATH = "/v1/vehicle-link-codes/by-code/{code}";
	private static final AtomicLong VEHICLE_IDS = new AtomicLong(7_000L);
	private static final long DEVICE_ID_OFFSET = 100_000L;
	private static final long SECOND_DEVICE_ID_OFFSET = 500_000L;
	private static final long FAMILY_ID = 12L;
	private static final long USER_ID = 101L;
	private static final long CODE_ID = 5012L;
	private static final String CODE = "K7P2QX9M";
	private static final String QR_PAYLOAD = "pickup://link?code=K7P2QX9M";
	private static final Instant EXPIRES_AT = Instant.parse("2026-11-05T12:10:00Z");
	private static final String EXPIRES_AT_KST = "2026-11-05T21:10:00+09:00";
	private static final int FIRST_REISSUES_LEFT = 4;
	private static final String VEHICLE_ACCESS_TOKEN = "eyJ.vehicle-access-token";
	private static final String VEHICLE_REFRESH_TOKEN = "rt_vehicle-refresh-token";
	private static final int POLL_AFTER_SECONDS = 2;
	private static final String VEHICLE_MODEL = "아이오닉 5";
	private static final String PLATE_NUMBER_MASKED = "12가 ****";
	private static final String CLIENT_IP = "203.0.113.7";
	private static final long ONE_SECOND_AND_A_BIT_MILLIS = 1_100L;
	private static final String APPROVE_PATH = "/v1/vehicle-links";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long LINKED_VEHICLE_ID = 77L;
	private static final String GUARDIAN_ACCESS_TOKEN = "eyJ.guardian-access-token";
	private static final String GUARDIAN_REFRESH_TOKEN = "rt_guardian-refresh-token";
	private static final String PUSH_TOKEN = "fcm-guardian-push-token";
	private static final String GUARDIAN_NAME = "김보호";
	private static final String NAME_21_CHARS = "가".repeat(21);
	private static final String NAME_WITH_EMOJI = "김보호😀";
	private static final String REQUIRED_CONSENTS_JSON =
			"[{\"type\": \"TERMS\", \"agreed\": true}, {\"type\": \"PRIVACY\", \"agreed\": true}, {\"type\": \"MARKETING\", \"agreed\": false}]";
	private static final String MISSING_PRIVACY_CONSENTS_JSON = "[{\"type\": \"TERMS\", \"agreed\": true}]";
	private static final String REQUEST_PART = "request";
	private static final String PROFILE_IMAGE_PART = "profileImage";
	private static final byte[] PNG_BYTES = {
			(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52};

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("POST /v1/vehicle-link-codes는 201과 codeId · code · qrPayload · expiresAt · reissuesLeft를 돌려준다")
	void issueLinkCodeReturns201WithCodeFields() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any())).willReturn(linkCodeResult());

		mockMvc.perform(issueRequest(linkedVehicleToken(vehicleId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codeId").value(CODE_ID))
				.andExpect(jsonPath("$.code").value(CODE))
				.andExpect(jsonPath("$.qrPayload").value(QR_PAYLOAD))
				.andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT_KST))
				.andExpect(jsonPath("$.reissuesLeft").value(FIRST_REISSUES_LEFT));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes는 연결된 차량 토큰의 deviceId · sub · familyId를 IssueLinkCodeCommand에 담아 넘긴다")
	void issueLinkCodePassesLinkedVehicleClaimsToUseCase() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any())).willReturn(linkCodeResult());

		mockMvc.perform(issueRequest(linkedVehicleToken(vehicleId)))
				.andExpect(status().isCreated());

		then(issueLinkCodeUseCase).should()
				.issueLinkCode(new IssueLinkCodeCommand(deviceIdOf(vehicleId), vehicleId, FAMILY_ID));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes는 familyId가 없는 연결 전 차량 토큰으로도 201을 돌려주고 familyId null을 넘긴다")
	void issueLinkCodeWithUnlinkedVehicleTokenReturns201() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any())).willReturn(linkCodeResult());

		mockMvc.perform(issueRequest(unlinkedVehicleToken(vehicleId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(CODE));

		then(issueLinkCodeUseCase).should()
				.issueLinkCode(new IssueLinkCodeCommand(deviceIdOf(vehicleId), vehicleId, null));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes가 한도를 넘으면 429 LINK_CODE_REISSUE_LIMIT을 돌려준다")
	void issueLinkCodeOverReissueLimitReturns429() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any()))
				.willThrow(BusinessException.of(
						HttpStatus.TOO_MANY_REQUESTS, "LINK_CODE_REISSUE_LIMIT", "연결 코드 재발급 한도를 넘었습니다"));

		mockMvc.perform(issueRequest(linkedVehicleToken(vehicleId)))
				.andExpect(commonError(HttpStatus.TOO_MANY_REQUESTS, "LINK_CODE_REISSUE_LIMIT"));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes가 연결 해제된 차량의 연결된 토큰(familyId 있음)이면 409 VEHICLE_UNLINKED를 돌려준다")
	void issueLinkCodeForUnlinkedVehicleWithLinkedTokenReturns409VehicleUnlinked() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any())).willThrow(vehicleUnlinked());

		mockMvc.perform(issueRequest(linkedVehicleToken(vehicleId)))
				.andExpect(commonError(HttpStatus.CONFLICT, "VEHICLE_UNLINKED"));

		then(issueLinkCodeUseCase).should()
				.issueLinkCode(new IssueLinkCodeCommand(deviceIdOf(vehicleId), vehicleId, FAMILY_ID));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes가 연결 해제된 차량의 연결 전 토큰(familyId 없음)이면 409 VEHICLE_UNLINKED를 돌려준다")
	void issueLinkCodeForUnlinkedVehicleWithPreLinkTokenReturns409VehicleUnlinked() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any())).willThrow(vehicleUnlinked());

		mockMvc.perform(issueRequest(unlinkedVehicleToken(vehicleId)))
				.andExpect(commonError(HttpStatus.CONFLICT, "VEHICLE_UNLINKED"));

		then(issueLinkCodeUseCase).should()
				.issueLinkCode(new IssueLinkCodeCommand(deviceIdOf(vehicleId), vehicleId, null));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes의 API 문서는 409 VEHICLE_UNLINKED 응답을 적는다")
	void issueLinkCodeApiDocDescribes409VehicleUnlinked() throws Exception {
		Method issueLinkCode = VehicleLinkCodeApiDoc.class.getMethod("issueLinkCode", AuthPrincipal.class);

		assertThat(issueLinkCode.getAnnotationsByType(ApiResponse.class))
				.filteredOn(response -> response.responseCode().equals("409"))
				.singleElement()
				.satisfies(response -> assertThat(response.description()).contains("VEHICLE_UNLINKED"));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes를 연결 전 토큰(familyId 없음)으로 부르는데 차량이 이미 가족에 연결돼 있으면 409 VEHICLE_ALREADY_LINKED를 돌려준다")
	void issueLinkCodeWithPreLinkTokenForLinkedVehicleReturns409VehicleAlreadyLinked() throws Exception {
		long vehicleId = nextVehicleId();
		given(issueLinkCodeUseCase.issueLinkCode(any())).willThrow(vehicleAlreadyLinked());

		mockMvc.perform(issueRequest(unlinkedVehicleToken(vehicleId)))
				.andExpect(commonError(HttpStatus.CONFLICT, "VEHICLE_ALREADY_LINKED"));

		then(issueLinkCodeUseCase).should()
				.issueLinkCode(new IssueLinkCodeCommand(deviceIdOf(vehicleId), vehicleId, null));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes의 API 문서는 409 VEHICLE_ALREADY_LINKED 응답을 적는다")
	void issueLinkCodeApiDocDescribes409VehicleAlreadyLinked() throws Exception {
		Method issueLinkCode = VehicleLinkCodeApiDoc.class.getMethod("issueLinkCode", AuthPrincipal.class);

		assertThat(issueLinkCode.getAnnotationsByType(ApiResponse.class))
				.filteredOn(response -> response.responseCode().equals("409"))
				.singleElement()
				.satisfies(response -> assertThat(response.description()).contains("VEHICLE_ALREADY_LINKED"));
	}

	@Test
	@DisplayName("POST /v1/vehicle-link-codes를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void issueLinkCodeWithUserTokenReturns403() throws Exception {
		String userToken = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(issueRequest(userToken))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(issueLinkCodeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status는 200과 status를 돌려준다")
	void getLinkStatusReturns200WithStatus() throws Exception {
		long vehicleId = nextVehicleId();
		given(getLinkStatusUseCase.getLinkStatus(CODE_ID, vehicleId, deviceIdOf(vehicleId))).willReturn(waitingResult());

		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(vehicleId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WAITING"))
				.andExpect(jsonPath("$.pollAfterSeconds").value(POLL_AFTER_SECONDS))
				.andExpect(jsonPath("$", hasKey("accessToken")))
				.andExpect(jsonPath("$.accessToken").value(nullValue()))
				.andExpect(jsonPath("$", hasKey("refreshToken")))
				.andExpect(jsonPath("$.refreshToken").value(nullValue()));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status는 LINKED면 vehicleId · familyId · accessToken · refreshToken과 pollAfterSeconds null을 돌려준다")
	void getLinkStatusLinkedReturnsVehicleTokens() throws Exception {
		long vehicleId = nextVehicleId();
		given(getLinkStatusUseCase.getLinkStatus(CODE_ID, vehicleId, deviceIdOf(vehicleId))).willReturn(new LinkStatusResult(
				LinkStatus.LINKED, vehicleId, FAMILY_ID, VEHICLE_ACCESS_TOKEN, VEHICLE_REFRESH_TOKEN, null));

		mockMvc.perform(statusRequest(CODE_ID, unlinkedVehicleToken(vehicleId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("LINKED"))
				.andExpect(jsonPath("$.vehicleId").value(vehicleId))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$.accessToken").value(VEHICLE_ACCESS_TOKEN))
				.andExpect(jsonPath("$.refreshToken").value(VEHICLE_REFRESH_TOKEN))
				.andExpect(jsonPath("$", hasKey("pollAfterSeconds")))
				.andExpect(jsonPath("$.pollAfterSeconds").value(nullValue()));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status는 경로의 codeId와 토큰 sub의 vehicleId · deviceId 클레임으로 GetLinkStatusUseCase.getLinkStatus를 부른다")
	void getLinkStatusPassesCodeIdAndTokenVehicleIdAndDeviceIdToUseCase() throws Exception {
		long vehicleId = nextVehicleId();
		given(getLinkStatusUseCase.getLinkStatus(anyLong(), anyLong(), anyLong())).willReturn(waitingResult());

		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(vehicleId)))
				.andExpect(status().isOk());

		then(getLinkStatusUseCase).should().getLinkStatus(CODE_ID, vehicleId, deviceIdOf(vehicleId));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status는 @PollingLog 대상이라 응답 status 값을 요청 로그 폴링 상태로 남긴다")
	void getLinkStatusRecordsPollStateForRequestLog() throws Exception {
		long vehicleId = nextVehicleId();
		given(getLinkStatusUseCase.getLinkStatus(CODE_ID, vehicleId, deviceIdOf(vehicleId))).willReturn(waitingResult());

		MvcResult result = mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(vehicleId)))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getAttribute(RequestLogAttributes.POLL_STATE)).isEqualTo("WAITING");
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status를 다른 차량 토큰으로 부르면 403 NOT_CODE_OWNER를 돌려준다")
	void getLinkStatusWithOtherVehicleTokenReturns403() throws Exception {
		long otherVehicleId = nextVehicleId();
		given(getLinkStatusUseCase.getLinkStatus(CODE_ID, otherVehicleId, deviceIdOf(otherVehicleId)))
				.willThrow(BusinessException.of(
						HttpStatus.FORBIDDEN, "NOT_CODE_OWNER", "이 차량이 발급한 연결 코드가 아닙니다"));

		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(otherVehicleId)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "NOT_CODE_OWNER"));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status를 1초 안에 다시 부르면 429 TOO_MANY_REQUESTS를 돌려준다")
	void getLinkStatusTwiceWithinOneSecondReturns429() throws Exception {
		long vehicleId = nextVehicleId();
		String token = linkedVehicleToken(vehicleId);
		given(getLinkStatusUseCase.getLinkStatus(anyLong(), anyLong(), anyLong())).willReturn(waitingResult());

		mockMvc.perform(statusRequest(CODE_ID, token))
				.andExpect(status().isOk());
		mockMvc.perform(statusRequest(CODE_ID, token))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
				.andExpect(jsonPath("$.error.message").isNotEmpty())
				.andExpect(jsonPath("$.error.retryAfterSeconds").value(1));

		then(getLinkStatusUseCase).should(times(1)).getLinkStatus(anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status를 다른 차량이 바로 이어 부르면 429가 아니라 200을 돌려준다")
	void getLinkStatusByAnotherVehicleRightAfterReturns200() throws Exception {
		long firstVehicleId = nextVehicleId();
		long secondVehicleId = nextVehicleId();
		given(getLinkStatusUseCase.getLinkStatus(anyLong(), anyLong(), anyLong())).willReturn(waitingResult());

		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(firstVehicleId)))
				.andExpect(status().isOk());
		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(secondVehicleId)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status의 1초 호출 제한은 기기(deviceId) 단위로 센다")
	void getLinkStatusPollLimitIsCountedPerDevice() throws Exception {
		long vehicleId = nextVehicleId();
		long firstDeviceId = deviceIdOf(vehicleId);
		long secondDeviceId = firstDeviceId + SECOND_DEVICE_ID_OFFSET;
		given(getLinkStatusUseCase.getLinkStatus(anyLong(), anyLong(), anyLong())).willReturn(waitingResult());

		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(vehicleId, firstDeviceId)))
				.andExpect(status().isOk());
		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(vehicleId, secondDeviceId)))
				.andExpect(status().isOk());
		mockMvc.perform(statusRequest(CODE_ID, linkedVehicleToken(vehicleId, firstDeviceId)))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));

		then(getLinkStatusUseCase).should().getLinkStatus(CODE_ID, vehicleId, firstDeviceId);
		then(getLinkStatusUseCase).should().getLinkStatus(CODE_ID, vehicleId, secondDeviceId);
		then(getLinkStatusUseCase).should(times(2)).getLinkStatus(anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/{codeId}/status를 1초가 지난 뒤 다시 부르면 200을 돌려준다")
	void getLinkStatusAgainAfterOneSecondReturns200() throws Exception {
		long vehicleId = nextVehicleId();
		String token = linkedVehicleToken(vehicleId);
		given(getLinkStatusUseCase.getLinkStatus(anyLong(), anyLong(), anyLong())).willReturn(waitingResult());

		mockMvc.perform(statusRequest(CODE_ID, token))
				.andExpect(status().isOk());
		Thread.sleep(ONE_SECOND_AND_A_BIT_MILLIS);
		mockMvc.perform(statusRequest(CODE_ID, token))
				.andExpect(status().isOk());

		then(getLinkStatusUseCase).should(times(2)).getLinkStatus(CODE_ID, vehicleId, deviceIdOf(vehicleId));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/by-code/{code}는 인증 헤더 없이 200과 status · vehicleModel · plateNumberMasked · expiresAt을 돌려준다")
	void checkLinkCodeWithoutAuthorizationHeaderReturns200() throws Exception {
		given(checkLinkCodeUseCase.checkLinkCode(anyString(), anyString())).willReturn(linkCodeInfo());

		mockMvc.perform(byCodeRequest(CODE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.vehicleModel").value(VEHICLE_MODEL))
				.andExpect(jsonPath("$.plateNumberMasked").value(PLATE_NUMBER_MASKED))
				.andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT_KST));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/by-code/{code}는 경로의 code와 요청 IP를 clientKey로 CheckLinkCodeUseCase.checkLinkCode에 넘긴다")
	void checkLinkCodePassesCodeAndRemoteAddressToUseCase() throws Exception {
		given(checkLinkCodeUseCase.checkLinkCode(anyString(), anyString())).willReturn(linkCodeInfo());

		mockMvc.perform(byCodeRequest(CODE))
				.andExpect(status().isOk());

		then(checkLinkCodeUseCase).should().checkLinkCode(CODE, CLIENT_IP);
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/by-code/{code}에 만료된 access 토큰이 Authorization 헤더로 붙어 와도 401이 아니라 200을 돌려준다")
	void checkLinkCodeWithExpiredAccessTokenHeaderStillReturns200() throws Exception {
		given(checkLinkCodeUseCase.checkLinkCode(anyString(), anyString())).willReturn(linkCodeInfo());
		String expiredAccessToken = accessTokenFixture.expiredUser(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(byCodeRequest(CODE).header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NEW"));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/by-code/{code}가 없는 코드면 404 LINK_CODE_INVALID를 돌려준다")
	void checkLinkCodeWithUnknownCodeReturns404() throws Exception {
		given(checkLinkCodeUseCase.checkLinkCode(anyString(), anyString()))
				.willThrow(BusinessException.of(HttpStatus.NOT_FOUND, "LINK_CODE_INVALID", "연결 코드를 찾을 수 없습니다"));

		mockMvc.perform(byCodeRequest(CODE))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "LINK_CODE_INVALID"));
	}

	@Test
	@DisplayName("GET /v1/vehicle-link-codes/by-code/{code}가 만료된 코드면 410 LINK_CODE_EXPIRED를 돌려준다")
	void checkLinkCodeWithExpiredCodeReturns410() throws Exception {
		given(checkLinkCodeUseCase.checkLinkCode(anyString(), anyString()))
				.willThrow(BusinessException.of(HttpStatus.GONE, "LINK_CODE_EXPIRED", "만료된 연결 코드입니다"));

		mockMvc.perform(byCodeRequest(CODE))
				.andExpect(commonError(HttpStatus.GONE, "LINK_CODE_EXPIRED"));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links는 인증 헤더 없이 201과 vehicleId · familyId · userId · accessToken · refreshToken을 돌려준다")
	void approveLinkWithoutAuthorizationHeaderReturns201WithGuardianTokens() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willReturn(vehicleLinkResult());

		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vehicleId").value(LINKED_VEHICLE_ID))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$.userId").value(USER_ID))
				.andExpect(jsonPath("$.accessToken").value(GUARDIAN_ACCESS_TOKEN))
				.andExpect(jsonPath("$.refreshToken").value(GUARDIAN_REFRESH_TOKEN));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links는 본문의 code · mode · consents · platform · pushToken · name을 ApproveLinkCommand에 담아 유스케이스에 넘긴다")
	void approveLinkPassesBodyToUseCaseAsCommand() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willReturn(vehicleLinkResult());

		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isCreated());

		then(approveLinkUseCase).should().approveLink(new ApproveLinkCommand(
				CODE,
				LinkMode.NEW,
				List.of(
						new ConsentAgreement(ConsentType.TERMS, true),
						new ConsentAgreement(ConsentType.PRIVACY, true),
						new ConsentAgreement(ConsentType.MARKETING, false)),
				Platform.ANDROID,
				PUSH_TOKEN,
				GUARDIAN_NAME));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links에 만료된 access 토큰이 Authorization 헤더로 붙어 와도 401이 아니라 201을 돌려준다")
	void approveLinkWithExpiredAccessTokenHeaderStillReturns201() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willReturn(vehicleLinkResult());
		String expiredAccessToken = accessTokenFixture.expiredUser(USER_ID, FAMILY_ID, Role.GUARDIAN);

		mockMvc.perform(approveRequest(approveBody("NEW", "IOS", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON))
						.header(HttpHeaders.AUTHORIZATION, bearer(expiredAccessToken)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(USER_ID));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links가 mode 불일치면 409 LINK_MODE_MISMATCH를 돌려준다")
	void approveLinkWithModeMismatchReturns409() throws Exception {
		given(approveLinkUseCase.approveLink(any()))
				.willThrow(BusinessException.of(
						HttpStatus.CONFLICT, "LINK_MODE_MISMATCH", "연결 방식이 코드의 상태와 다릅니다"));

		mockMvc.perform(approveRequest(approveBody("RELINK", "ANDROID", null, REQUIRED_CONSENTS_JSON)))
				.andExpect(commonError(HttpStatus.CONFLICT, "LINK_MODE_MISMATCH"));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links가 필수 동의 누락이면 422 CONSENT_REQUIRED를 돌려준다")
	void approveLinkWithoutRequiredConsentReturns422ConsentRequired() throws Exception {
		given(approveLinkUseCase.approveLink(any()))
				.willThrow(BusinessException.of(
						HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED", "필수 약관에 동의해 주세요"));

		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, MISSING_PRIVACY_CONSENTS_JSON)))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED"));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links에 mode NEW로 name 없이 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다")
	void approveLinkNewWithoutNameReturns422WithNameField() throws Exception {
		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", null, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("name"));

		then(approveLinkUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-links에 mode RELINK로 name 없이 보내면 201을 돌려주고 name null을 넘긴다")
	void approveLinkRelinkWithoutNameReturns201() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willReturn(vehicleLinkResult());

		mockMvc.perform(approveRequest(approveBody("RELINK", "ANDROID", null, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isCreated());

		then(approveLinkUseCase).should().approveLink(new ApproveLinkCommand(
				CODE,
				LinkMode.RELINK,
				List.of(
						new ConsentAgreement(ConsentType.TERMS, true),
						new ConsentAgreement(ConsentType.PRIVACY, true),
						new ConsentAgreement(ConsentType.MARKETING, false)),
				Platform.ANDROID,
				PUSH_TOKEN,
				null));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links에 mode NEW로 21자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다")
	void approveLinkNewWith21CharNameReturns422WithNameField() throws Exception {
		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", NAME_21_CHARS, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("name"));

		then(approveLinkUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-links에 mode NEW로 이모지가 든 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려준다")
	void approveLinkNewWithEmojiNameReturns422WithNameField() throws Exception {
		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", NAME_WITH_EMOJI, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("name"));

		then(approveLinkUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-links에 platform VEHICLE을 보내면 422 VALIDATION_FAILED와 fields의 platform을 돌려준다")
	void approveLinkWithVehiclePlatformReturns422WithPlatformField() throws Exception {
		mockMvc.perform(approveRequest(approveBody("NEW", "VEHICLE", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[0].field").value("platform"));

		then(approveLinkUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-links는 multipart의 request 파트와 profileImage 파트를 받아 201과 토큰을 돌려준다")
	void approveLinkMultipartWithProfileImageReturns201AndPassesImageToUseCase() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willReturn(vehicleLinkResult());

		mockMvc.perform(approveMultipartRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON),
						new MockMultipartFile(PROFILE_IMAGE_PART, "guardian.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vehicleId").value(LINKED_VEHICLE_ID))
				.andExpect(jsonPath("$.familyId").value(FAMILY_ID))
				.andExpect(jsonPath("$.userId").value(USER_ID))
				.andExpect(jsonPath("$.accessToken").value(GUARDIAN_ACCESS_TOKEN))
				.andExpect(jsonPath("$.refreshToken").value(GUARDIAN_REFRESH_TOKEN));

		ArgumentCaptor<ApproveLinkCommand> captor = ArgumentCaptor.forClass(ApproveLinkCommand.class);
		then(approveLinkUseCase).should().approveLink(captor.capture());
		ApproveLinkCommand command = captor.getValue();
		assertThat(command.code()).isEqualTo(CODE);
		assertThat(command.mode()).isEqualTo(LinkMode.NEW);
		assertThat(command.consents()).containsExactly(
				new ConsentAgreement(ConsentType.TERMS, true),
				new ConsentAgreement(ConsentType.PRIVACY, true),
				new ConsentAgreement(ConsentType.MARKETING, false));
		assertThat(command.platform()).isEqualTo(Platform.ANDROID);
		assertThat(command.pushToken()).isEqualTo(PUSH_TOKEN);
		assertThat(command.name()).isEqualTo(GUARDIAN_NAME);
		assertThat(command.profileImage()).isNotNull();
		assertThat(command.profileImage().format()).isEqualTo(ImageFormat.PNG);
		assertThat(command.profileImage().bytes()).isEqualTo(PNG_BYTES);
	}

	@Test
	@DisplayName("POST /v1/vehicle-links는 지금처럼 application/json 본문도 받아 201을 돌려준다")
	void approveLinkStillAcceptsJsonBodyReturns201() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willReturn(vehicleLinkResult());

		mockMvc.perform(approveRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(USER_ID))
				.andExpect(jsonPath("$.accessToken").value(GUARDIAN_ACCESS_TOKEN));

		then(approveLinkUseCase).should().approveLink(new ApproveLinkCommand(
				CODE,
				LinkMode.NEW,
				List.of(
						new ConsentAgreement(ConsentType.TERMS, true),
						new ConsentAgreement(ConsentType.PRIVACY, true),
						new ConsentAgreement(ConsentType.MARKETING, false)),
				Platform.ANDROID,
				PUSH_TOKEN,
				GUARDIAN_NAME));
	}

	@Test
	@DisplayName("POST /v1/vehicle-links의 profileImage가 이미지가 아니면 422 PROFILE_IMAGE_INVALID를 돌려준다")
	void approveLinkMultipartWithNonImageReturns422ProfileImageInvalid() throws Exception {
		mockMvc.perform(approveMultipartRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON),
						new MockMultipartFile(PROFILE_IMAGE_PART, "fake.png", MediaType.IMAGE_PNG_VALUE,
								"not an image".getBytes(StandardCharsets.UTF_8))))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "PROFILE_IMAGE_INVALID"));

		then(approveLinkUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-links의 profileImage가 5MB를 넘으면 413 PROFILE_IMAGE_TOO_LARGE를 돌려준다")
	void approveLinkMultipartWithImageOver5MbReturns413ProfileImageTooLarge() throws Exception {
		byte[] oversized = Arrays.copyOf(PNG_BYTES, 5_242_881);

		mockMvc.perform(approveMultipartRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON),
						new MockMultipartFile(PROFILE_IMAGE_PART, "big.png", MediaType.IMAGE_PNG_VALUE, oversized)))
				.andExpect(commonError(HttpStatus.CONTENT_TOO_LARGE, "PROFILE_IMAGE_TOO_LARGE"));

		then(approveLinkUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/vehicle-links는 저장소 업로드가 실패하면 503 STORAGE_UNAVAILABLE을 돌려준다")
	void approveLinkWhenStorageUploadFailsReturns503StorageUnavailable() throws Exception {
		given(approveLinkUseCase.approveLink(any())).willThrow(new StorageUnavailableException());

		mockMvc.perform(approveMultipartRequest(approveBody("NEW", "ANDROID", GUARDIAN_NAME, REQUIRED_CONSENTS_JSON),
						new MockMultipartFile(PROFILE_IMAGE_PART, "guardian.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES)))
				.andExpect(commonError(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE"));
	}

	private static long nextVehicleId() {
		return VEHICLE_IDS.incrementAndGet();
	}

	private static long deviceIdOf(long vehicleId) {
		return vehicleId + DEVICE_ID_OFFSET;
	}

	private String linkedVehicleToken(long vehicleId) {
		return linkedVehicleToken(vehicleId, deviceIdOf(vehicleId));
	}

	private String linkedVehicleToken(long vehicleId, long deviceId) {
		return accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(vehicleId),
				"principalType", PrincipalType.VEHICLE.name(),
				"familyId", FAMILY_ID,
				"deviceId", deviceId));
	}

	private String unlinkedVehicleToken(long vehicleId) {
		return accessTokenFixture.signedWithClaims(Map.of(
				"sub", String.valueOf(vehicleId),
				"principalType", PrincipalType.VEHICLE.name(),
				"deviceId", deviceIdOf(vehicleId)));
	}

	private static BusinessException vehicleUnlinked() {
		return BusinessException.of(HttpStatus.CONFLICT, "VEHICLE_UNLINKED", "연결이 해제된 차량입니다. 앱을 다시 등록해 주세요");
	}

	private static BusinessException vehicleAlreadyLinked() {
		return BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED);
	}

	private static LinkCodeResult linkCodeResult() {
		return new LinkCodeResult(CODE_ID, CODE, QR_PAYLOAD, EXPIRES_AT, FIRST_REISSUES_LEFT);
	}

	private static LinkStatusResult waitingResult() {
		return new LinkStatusResult(LinkStatus.WAITING, null, null, null, null, POLL_AFTER_SECONDS);
	}

	private static LinkCodeInfo linkCodeInfo() {
		return new LinkCodeInfo(LinkMode.NEW, VEHICLE_MODEL, PLATE_NUMBER_MASKED, EXPIRES_AT);
	}

	private static MockHttpServletRequestBuilder issueRequest(String accessToken) {
		return post(ISSUE_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
	}

	private static MockHttpServletRequestBuilder statusRequest(long codeId, String accessToken) {
		return get(STATUS_PATH, codeId)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
	}

	private static VehicleLinkResult vehicleLinkResult() {
		return new VehicleLinkResult(LINKED_VEHICLE_ID, FAMILY_ID, USER_ID, GUARDIAN_ACCESS_TOKEN, GUARDIAN_REFRESH_TOKEN);
	}

	private static String approveBody(String mode, String platform, String name, String consentsJson) {
		String nameField = name == null ? "" : ", \"name\": \"" + name + "\"";
		return "{\"code\": \"" + CODE + "\", \"mode\": \"" + mode + "\", \"consents\": " + consentsJson
				+ ", \"platform\": \"" + platform + "\", \"pushToken\": \"" + PUSH_TOKEN + "\"" + nameField + "}";
	}

	private static MockHttpServletRequestBuilder approveRequest(String body) {
		return post(APPROVE_PATH)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private static MockMultipartHttpServletRequestBuilder approveMultipartRequest(String requestJson,
			MockMultipartFile... extraParts) {
		MockMultipartHttpServletRequestBuilder builder = multipart(APPROVE_PATH)
				.file(new MockMultipartFile(REQUEST_PART, "", MediaType.APPLICATION_JSON_VALUE,
						requestJson.getBytes(StandardCharsets.UTF_8)));
		for (MockMultipartFile part : extraParts) {
			builder.file(part);
		}
		builder.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
		return builder;
	}

	private static MockHttpServletRequestBuilder byCodeRequest(String code) {
		return get(BY_CODE_PATH, code)
				.with(request -> {
					request.setRemoteAddr(CLIENT_IP);
					return request;
				});
	}

}
