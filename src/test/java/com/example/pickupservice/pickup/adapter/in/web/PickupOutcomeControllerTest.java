package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeCommand;
import com.example.pickupservice.pickup.application.port.in.CorrectionView;
import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.application.port.in.HistoryQuery;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OUT-01~03. 권한은 GUARDIAN · CO_GUARDIAN이고 Idempotency-Key는 요구하지 않으므로(#203) 성공 요청에 그 헤더를 싣지 않는다.
 */
@RequiredArgsConstructor
class PickupOutcomeControllerTest extends PickupWebTestSupport {

	private static final String HISTORY_PATH = "/v1/children/{childId}/pickup-history";
	private static final String OUTCOME_PATH = "/v1/pickup-sessions/{sessionId}/outcome";
	private static final String CORRECTION_PATH = "/v1/pickup-sessions/{sessionId}/outcome/correction";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long CHILD_ID = 31L;
	private static final long SESSION_ID = 1024L;
	private static final String SPOT_NAME = "해솔영어학원 정문 앞";
	private static final Instant COMPLETED_AT = Instant.parse("2026-11-05T09:20:00Z");
	private static final Instant PREDICTED_STOP_AT = Instant.parse("2026-11-05T09:12:00Z");
	private static final Instant ACTUAL_STOP_AT = Instant.parse("2026-11-05T09:13:30Z");
	private static final Instant CORRECTED_STOP_AT = Instant.parse("2026-11-05T09:12:30Z");
	private static final String CORRECTION_NOTE = "정차 시각이 실제보다 1분 늦게 찍혔어요";
	private static final String CORRECTION_BODY = """
			{"correctedStopAt":"2026-11-05T18:12:30+09:00","reasonCode":"WRONG_STOP_TIME",\
			"note":"정차 시각이 실제보다 1분 늦게 찍혔어요"}
			""";
	private static final AuthPrincipal GUARDIAN = user(GUARDIAN_USER_ID, Role.GUARDIAN);
	private static final AuthPrincipal CO_GUARDIAN = user(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN);

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	/**
	 * OUT-01~03 권한: GUARDIAN · CO_GUARDIAN.
	 */
	static Stream<AuthPrincipal> guardians() {
		return Stream.of(GUARDIAN, CO_GUARDIAN);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("guardians")
	@DisplayName("GET /v1/children/{childId}/pickup-history는 200과 items[](date · sessionId · status · completedAt)를 돌려준다")
	void historyReturns200WithItems(AuthPrincipal principal) throws Exception {
		given(getPickupHistoryUseCase.history(any())).willReturn(List.of(
				new HistoryItem(LocalDate.of(2026, 11, 5), SESSION_ID, SessionStatus.COMPLETED, COMPLETED_AT),
				new HistoryItem(LocalDate.of(2026, 11, 6), 1031L, SessionStatus.CANCELLED, null)));

		mockMvc.perform(get(HISTORY_PATH, CHILD_ID)
						.queryParam("month", "2026-11")
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal)))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[
						  {"date":"2026-11-05","sessionId":1024,"status":"COMPLETED","completedAt":"2026-11-05T18:20:00+09:00"},
						  {"date":"2026-11-06","sessionId":1031,"status":"CANCELLED","completedAt":null}
						]}
						""", JsonCompareMode.STRICT));

		then(getPickupHistoryUseCase).should().history(
				new HistoryQuery(principal, CHILD_ID, YearMonth.of(2026, 11)));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/pickup-history에 month 2026-13을 보내면 422 VALIDATION_FAILED를 돌려준다")
	void historyWithMonth13Returns422ValidationFailedOnMonth() throws Exception {
		mockMvc.perform(get(HISTORY_PATH, CHILD_ID)
						.queryParam("month", "2026-13")
						.header(HttpHeaders.AUTHORIZATION, bearerOf(GUARDIAN)))
				.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("month"));

		then(getPickupHistoryUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("guardians")
	@DisplayName("GET /v1/pickup-sessions/{sessionId}/outcome은 200과 completedAt · spotName · boardingMethod · predictedStopAt · actualStopAt · diffSeconds · correction을 돌려준다")
	void getOutcomeReturns200WithOutcomeAndEmptyCorrection(AuthPrincipal principal) throws Exception {
		given(getOutcomeUseCase.getOutcome(any())).willReturn(outcomeView(new CorrectionView(null, null)));

		mockMvc.perform(get(OUTCOME_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal)))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"sessionId":1024,"completedAt":"2026-11-05T18:20:00+09:00","spotName":"해솔영어학원 정문 앞",
						 "boardingMethod":"DOOR","predictedStopAt":"2026-11-05T18:12:00+09:00",
						 "actualStopAt":"2026-11-05T18:13:30+09:00","diffSeconds":90,
						 "correction":{"status":null,"correctedStopAt":null}}
						""", JsonCompareMode.STRICT));

		then(getOutcomeUseCase).should().getOutcome(new SessionQuery(principal, SESSION_ID));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("guardians")
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/outcome/correction은 200과 correction.status REQUESTED를 돌려준다")
	void correctOutcomeReturns200WithRequestedCorrection(AuthPrincipal principal) throws Exception {
		given(correctOutcomeUseCase.correct(any()))
				.willReturn(outcomeView(new CorrectionView("REQUESTED", CORRECTED_STOP_AT)));

		mockMvc.perform(put(CORRECTION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(principal))
						.contentType(MediaType.APPLICATION_JSON)
						.content(CORRECTION_BODY))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"sessionId":1024,"completedAt":"2026-11-05T18:20:00+09:00","spotName":"해솔영어학원 정문 앞",
						 "boardingMethod":"DOOR","predictedStopAt":"2026-11-05T18:12:00+09:00",
						 "actualStopAt":"2026-11-05T18:13:30+09:00","diffSeconds":90,
						 "correction":{"status":"REQUESTED","correctedStopAt":"2026-11-05T18:12:30+09:00"}}
						""", JsonCompareMode.STRICT));

		then(correctOutcomeUseCase).should().correct(new CorrectOutcomeCommand(
				principal, SESSION_ID, CORRECTED_STOP_AT, "WRONG_STOP_TIME", CORRECTION_NOTE));
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 201자 note를 보내면 422 VALIDATION_FAILED를 돌려준다")
	void correctOutcomeWith201CharNoteReturns422ValidationFailedOnNote() throws Exception {
		mockMvc.perform(put(CORRECTION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(GUARDIAN))
						.contentType(MediaType.APPLICATION_JSON)
						.content(correctionBody("WRONG_STOP_TIME", "가".repeat(201))))
				.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("note"));

		then(correctOutcomeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 WRONG_STOP_TIME · OTHER 밖의 reasonCode를 보내면 422 VALIDATION_FAILED를 돌려준다")
	void correctOutcomeWithUnknownReasonCodeReturns422ValidationFailedOnReasonCode() throws Exception {
		mockMvc.perform(put(CORRECTION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(GUARDIAN))
						.contentType(MediaType.APPLICATION_JSON)
						.content(correctionBody("LATE_ARRIVAL", CORRECTION_NOTE)))
				.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("reasonCode"));

		then(correctOutcomeUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/pickup-sessions/{sessionId}/outcome/correction에 correctedStopAt이 없으면 422 VALIDATION_FAILED(fields correctedStopAt)를 돌려주고 유스케이스를 부르지 않는다")
	void correctOutcomeWithoutCorrectedStopAtReturns422ValidationFailedOnCorrectedStopAt() throws Exception {
		mockMvc.perform(put(CORRECTION_PATH, SESSION_ID)
						.header(HttpHeaders.AUTHORIZATION, bearerOf(GUARDIAN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reasonCode":"WRONG_STOP_TIME","note":"정차 시각이 실제보다 1분 늦게 찍혔어요"}
								"""))
				.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields.length()").value(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("correctedStopAt"));

		then(correctOutcomeUseCase).shouldHaveNoInteractions();
	}

	/**
	 * 정정 요청 본문. correctedStopAt은 세션 날짜(2026-11-05, 한국) 안이다.
	 */
	private static String correctionBody(String reasonCode, String note) {
		return """
				{"correctedStopAt":"2026-11-05T18:12:30+09:00","reasonCode":"%s","note":"%s"}
				""".formatted(reasonCode, note);
	}

	/**
	 * 완료된 세션의 결과. 정정해도 actualStopAt 원본 · diffSeconds는 그대로이고 correction만 바뀐다.
	 */
	private static OutcomeView outcomeView(CorrectionView correction) {
		return new OutcomeView(SESSION_ID, COMPLETED_AT, SPOT_NAME, "DOOR", PREDICTED_STOP_AT, ACTUAL_STOP_AT, 90L,
				correction);
	}

	private String bearerOf(AuthPrincipal principal) {
		return bearer(accessTokenFixture.user(principal.id(), principal.familyId(), principal.role()));
	}

	private static AuthPrincipal user(long userId, Role role) {
		return new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
	}

}
