package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.CompleteSetupCommand;
import com.example.pickupservice.schedule.application.port.in.SetupChild;
import com.example.pickupservice.schedule.application.port.in.SetupCompletionResult;
import com.example.pickupservice.schedule.application.port.in.SetupSchedule;
import com.example.pickupservice.schedule.application.port.in.SetupSpot;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.JoinCallMode;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class SetupControllerTest extends ScheduleWebTestSupport {

	private static final String SETUP_SUMMARY_PATH = "/v1/children/{childId}/setup-summary";
	private static final String SETUP_COMPLETION_PATH = "/v1/children/{childId}/setup-completion";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long CHILD_USER_ID = 131L;
	private static final int SETTINGS_VERSION = 8;
	private static final String COMPLETION_BODY = """
			{"settingsVersion":8}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	// ---- SET-07 GET /v1/children/{childId}/setup-summary (#48)

	@Test
	@DisplayName("GET /v1/children/{childId}/setup-summary는 200과 child · schedules · pickupSpots · joinCallMode · missing · settingsVersion을 돌려준다")
	void getSetupSummaryReturns200WithAllSections() throws Exception {
		given(getSetupSummaryUseCase.getSetupSummary(any())).willReturn(readySummary());

		mockMvc.perform(setupSummaryRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"child":{"userId":131,"name":"민준","deviceStatus":"CONNECTED"},
						 "schedules":[{"scheduleId":801,"academyName":"해솔영어학원","status":"READY"}],
						 "pickupSpots":[{"spotId":21,"name":"정문 앞","isDefault":true}],
						 "joinCallMode":"AUTO",
						 "missing":[],
						 "settingsVersion":8}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/setup-summary는 pickupSpots의 isDefault 키 이름을 그대로 쓰고 false 값도 돌려준다")
	void getSetupSummaryKeepsIsDefaultKeyNameForFalseValue() throws Exception {
		given(getSetupSummaryUseCase.getSetupSummary(any())).willReturn(new SetupSummary(
				new SetupChild(CHILD_USER_ID, "민준", DeviceStatus.CONNECTED),
				List.of(),
				List.of(new SetupSpot(21L, "정문 앞", true), new SetupSpot(22L, "후문", false)),
				JoinCallMode.AUTO,
				List.of(),
				SETTINGS_VERSION));

		mockMvc.perform(setupSummaryRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pickupSpots[*].isDefault", contains(true, false)))
				.andExpect(jsonPath("$.pickupSpots[0].default").doesNotExist());
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/setup-summary는 missing 항목(CHILD_DEVICE · SCHEDULE · PICKUP_SPOT)과 DRAFT 일정 · 연결 안 된 deviceStatus를 이름 그대로 돌려준다")
	void getSetupSummaryReturnsMissingItemsAndEnumNames() throws Exception {
		given(getSetupSummaryUseCase.getSetupSummary(any())).willReturn(new SetupSummary(
				new SetupChild(CHILD_USER_ID, "민준", DeviceStatus.INVITE_PENDING),
				List.of(new SetupSchedule(801L, "해솔영어학원", ScheduleStatus.DRAFT)),
				List.of(),
				JoinCallMode.GUARDIAN_CONFIRM,
				List.of(MissingSetupItem.CHILD_DEVICE, MissingSetupItem.SCHEDULE, MissingSetupItem.PICKUP_SPOT),
				3));

		mockMvc.perform(setupSummaryRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"child":{"userId":131,"name":"민준","deviceStatus":"INVITE_PENDING"},
						 "schedules":[{"scheduleId":801,"academyName":"해솔영어학원","status":"DRAFT"}],
						 "pickupSpots":[],
						 "joinCallMode":"GUARDIAN_CONFIRM",
						 "missing":["CHILD_DEVICE","SCHEDULE","PICKUP_SPOT"],
						 "settingsVersion":3}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/setup-summary는 토큰의 familyId · 경로의 childId를 ChildQuery로 바꿔 유스케이스에 넘긴다")
	void getSetupSummaryPassesChildQueryToUseCase() throws Exception {
		given(getSetupSummaryUseCase.getSetupSummary(any())).willReturn(readySummary());

		mockMvc.perform(setupSummaryRequest(guardianBearer()))
				.andExpect(status().isOk());

		then(getSetupSummaryUseCase).should().getSetupSummary(new ChildQuery(FAMILY_ID, CHILD_USER_ID));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/setup-summary를 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void getSetupSummaryWithCoGuardianTokenReturns200() throws Exception {
		given(getSetupSummaryUseCase.getSetupSummary(any())).willReturn(readySummary());

		mockMvc.perform(setupSummaryRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settingsVersion").value(SETTINGS_VERSION));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/setup-summary를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void getSetupSummaryWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(setupSummaryRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getSetupSummaryUseCase).shouldHaveNoInteractions();
	}

	// ---- SET-08 POST /v1/children/{childId}/setup-completion (#49 · #50 · #51)

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion은 200과 syncedToVehicle을 돌려준다")
	void completeSetupReturns200WithSyncedToVehicle() throws Exception {
		given(completeSetupUseCase.completeSetup(any())).willReturn(new SetupCompletionResult(true));

		mockMvc.perform(setupCompletionRequest(guardianBearer(), COMPLETION_BODY))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"syncedToVehicle":true}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion은 syncedToVehicle false도 그대로 돌려준다")
	void completeSetupReturnsSyncedToVehicleFalseAsIs() throws Exception {
		given(completeSetupUseCase.completeSetup(any())).willReturn(new SetupCompletionResult(false));

		mockMvc.perform(setupCompletionRequest(guardianBearer(), COMPLETION_BODY))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"syncedToVehicle":false}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion은 토큰의 familyId · 경로의 childId · 본문의 settingsVersion을 CompleteSetupCommand로 바꿔 유스케이스에 넘긴다")
	void completeSetupPassesCommandToUseCase() throws Exception {
		given(completeSetupUseCase.completeSetup(any())).willReturn(new SetupCompletionResult(true));

		mockMvc.perform(setupCompletionRequest(guardianBearer(), COMPLETION_BODY))
				.andExpect(status().isOk());

		ArgumentCaptor<CompleteSetupCommand> captor = ArgumentCaptor.forClass(CompleteSetupCommand.class);
		then(completeSetupUseCase).should().completeSetup(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new CompleteSetupCommand(FAMILY_ID, CHILD_USER_ID, SETTINGS_VERSION));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion을 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void completeSetupWithCoGuardianTokenReturns200() throws Exception {
		given(completeSetupUseCase.completeSetup(any())).willReturn(new SetupCompletionResult(true));

		mockMvc.perform(setupCompletionRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), COMPLETION_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.syncedToVehicle").value(true));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion이 버전 충돌이면 409 SETTINGS_VERSION_CONFLICT를 돌려준다")
	void completeSetupWithVersionConflictReturns409SettingsVersionConflict() throws Exception {
		given(completeSetupUseCase.completeSetup(any())).willThrow(BusinessException.of(
				HttpStatus.CONFLICT, "SETTINGS_VERSION_CONFLICT", "설정이 바뀌었습니다. 요약을 다시 불러 주세요"));

		mockMvc.perform(setupCompletionRequest(guardianBearer(), COMPLETION_BODY))
				.andExpect(commonError(HttpStatus.CONFLICT, "SETTINGS_VERSION_CONFLICT"));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion이 누락이 있으면 422 SETUP_INCOMPLETE와 common ErrorCode 문구(픽업 시작과 같은 문구)를 돌려주고 missing은 error.missing 추가 키로 나간다")
	void completeSetupWithMissingItemsReturns422SetupIncomplete() throws Exception {
		given(completeSetupUseCase.completeSetup(any())).willThrow(BusinessException.of(ErrorCode.SETUP_INCOMPLETE)
				.withDetail("missing", List.of(MissingSetupItem.CHILD_DEVICE, MissingSetupItem.PICKUP_SPOT)));

		mockMvc.perform(setupCompletionRequest(guardianBearer(), COMPLETION_BODY))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "SETUP_INCOMPLETE"))
				.andExpect(jsonPath("$.error.message").value("픽업 준비가 끝나지 않았습니다"))
				.andExpect(jsonPath("$.error.missing", contains("CHILD_DEVICE", "PICKUP_SPOT")));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion에 settingsVersion 없이 {}를 보내면 422 VALIDATION_FAILED와 fields의 settingsVersion을 돌려주고 유스케이스를 부르지 않는다")
	void completeSetupWithoutSettingsVersionReturns422ValidationFailed() throws Exception {
		mockMvc.perform(setupCompletionRequest(guardianBearer(), "{}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("settingsVersion")));

		then(completeSetupUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion에 Idempotency-Key가 없으면 422 VALIDATION_FAILED와 fields의 Idempotency-Key를 돌려주고 유스케이스를 부르지 않는다")
	void completeSetupWithoutIdempotencyKeyReturns422ValidationFailed() throws Exception {
		mockMvc.perform(post(SETUP_COMPLETION_PATH, CHILD_USER_ID)
						.header(HttpHeaders.AUTHORIZATION, guardianBearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content(COMPLETION_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem(IDEMPOTENCY_KEY_HEADER)));

		then(completeSetupUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/setup-completion을 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void completeSetupWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(setupCompletionRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE), COMPLETION_BODY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(completeSetupUseCase).shouldHaveNoInteractions();
	}

	private static SetupSummary readySummary() {
		return new SetupSummary(
				new SetupChild(CHILD_USER_ID, "민준", DeviceStatus.CONNECTED),
				List.of(new SetupSchedule(801L, "해솔영어학원", ScheduleStatus.READY)),
				List.of(new SetupSpot(21L, "정문 앞", true)),
				JoinCallMode.AUTO,
				List.of(),
				SETTINGS_VERSION);
	}

	private MockHttpServletRequestBuilder setupSummaryRequest(String bearer) {
		return get(SETUP_SUMMARY_PATH, CHILD_USER_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private MockHttpServletRequestBuilder setupCompletionRequest(String bearer, String body) {
		return post(SETUP_COMPLETION_PATH, CHILD_USER_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private String guardianBearer() {
		return userBearer(GUARDIAN_USER_ID, Role.GUARDIAN);
	}

	private String userBearer(long userId, Role role) {
		return bearer(accessTokenFixture.user(userId, FAMILY_ID, role));
	}

}
