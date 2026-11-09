package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.in.SessionStatusView;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupViolation;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@RequiredArgsConstructor
class PickupErrorsTest extends IntegrationTestSupport {

	private static final String TEST_ENDPOINT_PATTERN = "/test/pickup-errors/**";
	private static final String CALL_GUARD_UNMET_PATH = "/test/pickup-errors/call-guard-unmet";
	private static final String SETUP_INCOMPLETE_PATH = "/test/pickup-errors/setup-incomplete";
	private static final String HELP_REQUEST_TOO_SOON_PATH = "/test/pickup-errors/help-request-too-soon";
	private static final String STATE_VERSION_CONFLICT_PATH = "/test/pickup-errors/state-version-conflict";
	private static final String RULE_VIOLATION_PATH = "/test/pickup-errors/rule-violation/{violation}";

	private static final String NOT_TODAY_MESSAGE = "오늘 날짜의 픽업만 시작할 수 있습니다";
	private static final String SCHEDULES_NOT_GROUPABLE_MESSAGE = "같은 학원 · 같은 시간대의 일정만 함께 시작할 수 있습니다";
	private static final String SETUP_INCOMPLETE_MESSAGE = "픽업 준비가 끝나지 않았습니다";
	private static final String ANOTHER_SESSION_ACTIVE_MESSAGE = "이미 진행 중인 다른 픽업이 있습니다";
	private static final String SPOT_NOT_SAVED_MESSAGE = "저장된 장소가 아닙니다";
	private static final String CALL_ALREADY_SENT_MESSAGE = "이미 합류 호출을 보냈습니다";
	private static final String CHILD_ALREADY_MOVING_MESSAGE = "자녀가 이미 이동 중이라 장소를 바꿀 수 없습니다";
	private static final String SESSION_ALREADY_FINISHED_MESSAGE = "이미 끝난 픽업입니다";
	private static final String VEHICLE_NOT_STOPPED_MESSAGE = "차량이 아직 정차하지 않았습니다";
	private static final String CHILDREN_NOT_ALL_BOARDED_MESSAGE = "아직 탑승하지 않은 자녀가 있습니다";
	private static final String REJECTION_WINDOW_CLOSED_MESSAGE = "탑승을 취소할 수 있는 시간이 지났습니다";
	private static final String INCIDENT_ALREADY_OPEN_MESSAGE = "이미 처리 중인 예외 상황이 있습니다";
	private static final String PREP_EXTENSION_LIMIT_MESSAGE = "준비 시간은 모두 15분까지만 늘릴 수 있습니다";
	private static final String LOCATION_CONSENT_REQUIRED_MESSAGE = "위치 정보 제공 동의가 필요합니다";
	private static final String HELP_REQUEST_TOO_SOON_MESSAGE = "잠시 후 다시 요청해 주세요";
	private static final String CALL_GUARD_UNMET_MESSAGE = "합류 호출을 보낼 수 있는 조건이 아닙니다";
	private static final String NOT_GUARDIAN_CONFIRM_MODE_MESSAGE = "보호자 확인 방식일 때만 직접 호출할 수 있습니다";
	private static final String CALL_ALREADY_REVOKED_MESSAGE = "이미 철회된 합류 호출입니다";
	private static final String CHILD_NOT_CALLED_MESSAGE = "합류 안내를 받은 뒤에만 위치를 보낼 수 있습니다";

	private static final List<String> MISSING = List.of("CHILD_DEVICE", "PICKUP_SPOT");
	private static final List<String> UNMET = List.of("G2", "G4");
	private static final Instant NEXT_ALLOWED_AT_UTC = Instant.parse("2026-11-05T09:10:00Z");
	private static final String NEXT_ALLOWED_AT_KST = "2026-11-05T18:10:00+09:00";

	private static final long LATEST_SESSION_ID = 1024L;
	private static final int LATEST_STATE_VERSION = 15;
	private static final SessionView LATEST_SESSION_VIEW = new SessionView(LATEST_SESSION_ID, LATEST_STATE_VERSION,
			new SessionStatusView(SessionStatus.ACTIVE, VehiclePhase.APPROACHING, "NONE", SpotStatus.CONFIRMED),
			null, null, null, List.of(), null, null, null);

	private static final List<ErrorSpec> PLAIN_ERRORS = List.of(
			new ErrorSpec("NOT_TODAY", HttpStatus.UNPROCESSABLE_CONTENT, NOT_TODAY_MESSAGE, PickupErrors::notToday),
			new ErrorSpec("SCHEDULES_NOT_GROUPABLE", HttpStatus.UNPROCESSABLE_CONTENT, SCHEDULES_NOT_GROUPABLE_MESSAGE,
					PickupErrors::schedulesNotGroupable),
			new ErrorSpec("ANOTHER_SESSION_ACTIVE", HttpStatus.CONFLICT, ANOTHER_SESSION_ACTIVE_MESSAGE,
					PickupErrors::anotherSessionActive),
			new ErrorSpec("SPOT_NOT_SAVED", HttpStatus.UNPROCESSABLE_CONTENT, SPOT_NOT_SAVED_MESSAGE,
					PickupErrors::spotNotSaved),
			new ErrorSpec("CALL_ALREADY_SENT", HttpStatus.CONFLICT, CALL_ALREADY_SENT_MESSAGE,
					PickupErrors::callAlreadySent),
			new ErrorSpec("CHILD_ALREADY_MOVING", HttpStatus.CONFLICT, CHILD_ALREADY_MOVING_MESSAGE,
					PickupErrors::childAlreadyMoving),
			new ErrorSpec("SESSION_ALREADY_FINISHED", HttpStatus.CONFLICT, SESSION_ALREADY_FINISHED_MESSAGE,
					PickupErrors::sessionAlreadyFinished),
			new ErrorSpec("VEHICLE_NOT_STOPPED", HttpStatus.UNPROCESSABLE_CONTENT, VEHICLE_NOT_STOPPED_MESSAGE,
					PickupErrors::vehicleNotStopped),
			new ErrorSpec("CHILDREN_NOT_ALL_BOARDED", HttpStatus.UNPROCESSABLE_CONTENT, CHILDREN_NOT_ALL_BOARDED_MESSAGE,
					PickupErrors::childrenNotAllBoarded),
			new ErrorSpec("REJECTION_WINDOW_CLOSED", HttpStatus.CONFLICT, REJECTION_WINDOW_CLOSED_MESSAGE,
					PickupErrors::rejectionWindowClosed),
			new ErrorSpec("INCIDENT_ALREADY_OPEN", HttpStatus.CONFLICT, INCIDENT_ALREADY_OPEN_MESSAGE,
					PickupErrors::incidentAlreadyOpen),
			new ErrorSpec("PREP_EXTENSION_LIMIT", HttpStatus.UNPROCESSABLE_CONTENT, PREP_EXTENSION_LIMIT_MESSAGE,
					PickupErrors::prepExtensionLimit),
			new ErrorSpec("LOCATION_CONSENT_REQUIRED", HttpStatus.FORBIDDEN, LOCATION_CONSENT_REQUIRED_MESSAGE,
					PickupErrors::locationConsentRequired),
			new ErrorSpec("NOT_GUARDIAN_CONFIRM_MODE", HttpStatus.CONFLICT, NOT_GUARDIAN_CONFIRM_MODE_MESSAGE,
					PickupErrors::notGuardianConfirmMode),
			new ErrorSpec("CALL_ALREADY_REVOKED", HttpStatus.CONFLICT, CALL_ALREADY_REVOKED_MESSAGE,
					PickupErrors::callAlreadyRevoked),
			new ErrorSpec("CHILD_NOT_CALLED", HttpStatus.CONFLICT, CHILD_NOT_CALLED_MESSAGE,
					PickupErrors::childNotCalled),
			new ErrorSpec("NOT_FOUND", HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage(), PickupErrors::notFound));

	private final MockMvc mockMvc;

	static Stream<ErrorSpec> plainErrors() {
		return PLAIN_ERRORS.stream();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("plainErrors")
	@DisplayName("추가 값이 없는 픽업 오류 팩토리는 API 명세의 HTTP 상태 · code · 한국어 message를 담은 BusinessException을 만들고 retryAfterSeconds · fields · error 추가 키 · session은 비어 있다")
	void plainPickupErrorCarriesSpecStatusCodeAndKoreanMessage(ErrorSpec spec) {
		BusinessException exception = spec.factory().get();

		assertThat(exception.status()).isEqualTo(spec.status());
		assertThat(exception.code()).isEqualTo(spec.code());
		assertThat(exception.getMessage()).isEqualTo(spec.message()).containsPattern("[가-힣]");
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.fields()).isNull();
		assertThat(exception.details()).isEmpty();
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("setupIncomplete([CHILD_DEVICE, PICKUP_SPOT])는 422 SETUP_INCOMPLETE · 한국어 message이고 error 추가 키 missing 하나에 받은 문자열 목록을 담는다")
	void setupIncompleteCarriesMissingListAsErrorDetail() {
		BusinessException exception = PickupErrors.setupIncomplete(MISSING);

		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("SETUP_INCOMPLETE");
		assertThat(exception.getMessage()).isEqualTo(SETUP_INCOMPLETE_MESSAGE);
		assertThat(exception.details()).containsOnly(entry("missing", MISSING));
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("helpRequestTooSoon(2026-11-05T09:10:00Z)는 429 HELP_REQUEST_TOO_SOON · 한국어 message이고 error 추가 키 nextAllowedAt 하나에 받은 Instant를 담으며 retryAfterSeconds는 비어 있다")
	void helpRequestTooSoonCarriesNextAllowedAtInstantAsErrorDetail() {
		BusinessException exception = PickupErrors.helpRequestTooSoon(NEXT_ALLOWED_AT_UTC);

		assertThat(exception.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exception.code()).isEqualTo("HELP_REQUEST_TOO_SOON");
		assertThat(exception.getMessage()).isEqualTo(HELP_REQUEST_TOO_SOON_MESSAGE);
		assertThat(exception.details()).containsOnly(entry("nextAllowedAt", NEXT_ALLOWED_AT_UTC));
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("callGuardUnmet([G2, G4])는 422 CALL_GUARD_UNMET · 한국어 message이고 error 추가 키 unmet 하나에 받은 가드 목록을 담는다")
	void callGuardUnmetCarriesUnmetGuardListAsErrorDetail() {
		BusinessException exception = PickupErrors.callGuardUnmet(UNMET);

		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("CALL_GUARD_UNMET");
		assertThat(exception.getMessage()).isEqualTo(CALL_GUARD_UNMET_MESSAGE);
		assertThat(exception.details()).containsOnly(entry("unmet", UNMET));
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("stateVersionConflict(최신 세션 뷰)는 409 STATE_VERSION_CONFLICT · 공통 ErrorCode 문구이고 session에 받은 세션 뷰를 그대로 담으며 error 추가 키는 없다")
	void stateVersionConflictCarriesLatestSessionViewAsSession() {
		BusinessException exception = PickupErrors.stateVersionConflict(LATEST_SESSION_VIEW);

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo(ErrorCode.STATE_VERSION_CONFLICT.name());
		assertThat(exception.getMessage()).isEqualTo(ErrorCode.STATE_VERSION_CONFLICT.getMessage());
		assertThat(exception.session()).isSameAs(LATEST_SESSION_VIEW);
		assertThat(exception.details()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = PickupViolation.class, mode = EnumSource.Mode.EXCLUDE, names = "CHILD_NOT_IN_SESSION")
	@DisplayName("from(PickupRuleException)은 CHILD_NOT_IN_SESSION 밖의 도메인 위반 PickupViolation마다 이름이 같은 픽업 오류의 HTTP 상태 · code · 한국어 message를 담은 BusinessException으로 바꾼다")
	void fromRuleExceptionMapsEachViolationToPickupErrorOfSameCode(PickupViolation violation) {
		ErrorSpec expected = plainErrorOf(violation.name());

		BusinessException exception = PickupErrors.from(new PickupRuleException(violation));

		assertThat(exception.status()).isEqualTo(expected.status());
		assertThat(exception.code()).isEqualTo(expected.code());
		assertThat(exception.getMessage()).isEqualTo(expected.message());
		assertThat(exception.details()).isEmpty();
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("from(PickupRuleException(CHILD_NOT_IN_SESSION))은 세션에 없는 자녀를 404 NOT_FOUND · 공통 ErrorCode 문구인 BusinessException으로 바꾸고 error 추가 키 · session은 비어 있다")
	void fromRuleExceptionMapsChildNotInSessionToNotFound() {
		BusinessException exception = PickupErrors.from(new PickupRuleException(PickupViolation.CHILD_NOT_IN_SESSION));

		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.getMessage()).isEqualTo(ErrorCode.NOT_FOUND.getMessage());
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.fields()).isNull();
		assertThat(exception.details()).isEmpty();
		assertThat(exception.session()).isNull();
	}

	@Test
	@DisplayName("컨트롤러가 PickupErrors.callGuardUnmet([G2, G4])를 던지면 422 응답의 error.code CALL_GUARD_UNMET · error.message · error.unmet [G2, G4]이고 unmet은 본문 최상위가 아니라 error 안에만 있다")
	void callGuardUnmetResponseHasUnmetInsideError() throws Exception {
		mockMvc.perform(get(CALL_GUARD_UNMET_PATH))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "CALL_GUARD_UNMET"))
				.andExpect(jsonPath("$.error.message").value(CALL_GUARD_UNMET_MESSAGE))
				.andExpect(jsonPath("$.error.unmet", contains("G2", "G4")))
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$", not(hasKey("unmet"))));
	}

	@Test
	@DisplayName("컨트롤러가 PickupErrors.setupIncomplete([CHILD_DEVICE, PICKUP_SPOT])를 던지면 422 응답의 error에 code SETUP_INCOMPLETE · message · retryAfterSeconds · missing [CHILD_DEVICE, PICKUP_SPOT] 네 키만 있다")
	void setupIncompleteResponseHasMissingInsideError() throws Exception {
		mockMvc.perform(get(SETUP_INCOMPLETE_PATH))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "SETUP_INCOMPLETE"))
				.andExpect(jsonPath("$.error.message").value(SETUP_INCOMPLETE_MESSAGE))
				.andExpect(jsonPath("$.error.missing", contains("CHILD_DEVICE", "PICKUP_SPOT")))
				.andExpect(jsonPath("$.error", aMapWithSize(4)))
				.andExpect(jsonPath("$", aMapWithSize(1)));
	}

	@Test
	@DisplayName("컨트롤러가 PickupErrors.helpRequestTooSoon(2026-11-05T09:10:00Z)를 던지면 429 응답의 error.code HELP_REQUEST_TOO_SOON · error.message이고 error.nextAllowedAt은 한국 시각 2026-11-05T18:10:00+09:00이다")
	void helpRequestTooSoonResponseHasKstNextAllowedAtInsideError() throws Exception {
		mockMvc.perform(get(HELP_REQUEST_TOO_SOON_PATH))
				.andExpect(commonError(HttpStatus.TOO_MANY_REQUESTS, "HELP_REQUEST_TOO_SOON"))
				.andExpect(jsonPath("$.error.message").value(HELP_REQUEST_TOO_SOON_MESSAGE))
				.andExpect(jsonPath("$.error.nextAllowedAt").value(NEXT_ALLOWED_AT_KST))
				.andExpect(jsonPath("$", aMapWithSize(1)));
	}

	@Test
	@DisplayName("컨트롤러가 PickupErrors.stateVersionConflict(최신 세션 뷰)를 던지면 409 응답 최상위에 error와 session 두 키가 있고 session은 sessionId 1024 · stateVersion 15 · status.session ACTIVE · status.vehicle APPROACHING인 세션 뷰다")
	void stateVersionConflictResponseHasLatestSessionViewBesideError() throws Exception {
		mockMvc.perform(get(STATE_VERSION_CONFLICT_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, ErrorCode.STATE_VERSION_CONFLICT.name()))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.STATE_VERSION_CONFLICT.getMessage()))
				.andExpect(jsonPath("$", aMapWithSize(2)))
				.andExpect(jsonPath("$", allOf(hasKey("error"), hasKey("session"))))
				.andExpect(jsonPath("$.error", aMapWithSize(3)))
				.andExpect(jsonPath("$.session.sessionId").value(LATEST_SESSION_ID))
				.andExpect(jsonPath("$.session.stateVersion").value(LATEST_STATE_VERSION))
				.andExpect(jsonPath("$.session.status.session").value("ACTIVE"))
				.andExpect(jsonPath("$.session.status.vehicle").value("APPROACHING"));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = PickupViolation.class, names = {"REJECTION_WINDOW_CLOSED", "CHILDREN_NOT_ALL_BOARDED"})
	@DisplayName("컨트롤러가 PickupErrors.from(PickupRuleException)을 던지면 응답은 그 위반과 이름이 같은 code의 HTTP 상태 · error.code · 한국어 error.message이고 error에는 code · message · retryAfterSeconds 세 키만 있다")
	void ruleViolationResponseHasMappedStatusCodeAndMessage(PickupViolation violation) throws Exception {
		ErrorSpec expected = plainErrorOf(violation.name());

		mockMvc.perform(get(RULE_VIOLATION_PATH, violation.name()))
				.andExpect(commonError(expected.status(), expected.code()))
				.andExpect(jsonPath("$.error.message").value(expected.message()))
				.andExpect(jsonPath("$.error", aMapWithSize(3)))
				.andExpect(jsonPath("$", aMapWithSize(1)));
	}

	private static ErrorSpec plainErrorOf(String code) {
		return PLAIN_ERRORS.stream()
				.filter(spec -> spec.code().equals(code))
				.findFirst()
				.orElseThrow(() -> new AssertionError("테스트 표에 없는 픽업 오류 code: " + code));
	}

	record ErrorSpec(String code, HttpStatus status, String message, Supplier<BusinessException> factory) {

		@Override
		public String toString() {
			return code;
		}
	}

	@RestController
	static class PickupErrorsTestController {

		@GetMapping(CALL_GUARD_UNMET_PATH)
		public String throwCallGuardUnmet() {
			throw PickupErrors.callGuardUnmet(UNMET);
		}

		@GetMapping(SETUP_INCOMPLETE_PATH)
		public String throwSetupIncomplete() {
			throw PickupErrors.setupIncomplete(MISSING);
		}

		@GetMapping(HELP_REQUEST_TOO_SOON_PATH)
		public String throwHelpRequestTooSoon() {
			throw PickupErrors.helpRequestTooSoon(NEXT_ALLOWED_AT_UTC);
		}

		@GetMapping(STATE_VERSION_CONFLICT_PATH)
		public String throwStateVersionConflict() {
			throw PickupErrors.stateVersionConflict(LATEST_SESSION_VIEW);
		}

		@GetMapping(RULE_VIOLATION_PATH)
		public String throwRuleViolation(@PathVariable("violation") PickupViolation violation) {
			throw PickupErrors.from(new PickupRuleException(violation));
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import(PickupErrorsTestController.class)
	static class PickupErrorsTestEndpointConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain pickupErrorsTestEndpointSecurityFilterChain(HttpSecurity http) throws Exception {
			http.securityMatcher(TEST_ENDPOINT_PATTERN)
					.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
					.csrf(AbstractHttpConfigurer::disable);
			return http.build();
		}

	}

}
