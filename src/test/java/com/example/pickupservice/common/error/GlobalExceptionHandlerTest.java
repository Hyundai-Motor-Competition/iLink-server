package com.example.pickupservice.common.error;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class GlobalExceptionHandlerTest extends IntegrationTestSupport {

	private static final String TEST_ENDPOINT_PATTERN = "/test/common-error/**";
	private static final String JSON_BODY_PATH = "/test/common-error/json-body";
	private static final String VALIDATED_BODY_PATH = "/test/common-error/validated-body";
	private static final String VALIDATED_BODY_WITH_ID_PATH = "/test/common-error/validated-body/{id}";
	private static final String VALIDATED_PARAM_PATH = "/test/common-error/validated-param";
	private static final String VALIDATED_NAMED_PARAM_PATH = "/test/common-error/validated-named-param";
	private static final String VALIDATED_NAMED_HEADER_PATH = "/test/common-error/validated-named-header";
	private static final String VALIDATED_UNNAMED_PARAM_PATH = "/test/common-error/validated-unnamed-param";
	private static final String GET_ONLY_PATH = "/test/common-error/get-only";
	private static final String REQUIRED_PARAM_PATH = "/test/common-error/required-param";
	private static final String UNHANDLED_EXCEPTION_PATH = "/test/common-error/unhandled-exception";
	private static final String UNMAPPED_PATH = "/test/common-error/no-such-endpoint";
	private static final String TOO_MANY_REQUESTS_PATH = "/test/common-error/business-too-many-requests";
	private static final String RESEND_TOO_SOON_PATH = "/test/common-error/business-resend-too-soon";
	private static final String DOMAIN_CODE_PATH = "/test/common-error/business-domain-code";
	private static final String COMMON_NOT_FOUND_PATH = "/test/common-error/business-common-not-found";
	private static final String COMMON_ERROR_CODE_PATH = "/test/common-error/business-common-code/{code}";
	private static final String INVALID_FIELD_PATH = "/test/common-error/business-invalid-field";
	private static final String OVERLAP_PATH = "/test/common-error/business-overlap";
	private static final String REQUIRED_HEADER_PATH = "/test/common-error/required-header";
	private static final String JSON_ONLY_PATH = "/test/common-error/json-only";
	private static final String MAX_UPLOAD_SIZE_EXCEEDED_PATH = "/test/common-error/max-upload-size-exceeded";
	private static final long MULTIPART_MAX_REQUEST_SIZE = 6L * 1024 * 1024;
	private static final String RESPONSE_STATUS_EXCEPTION_PATH = "/test/common-error/response-status-exception";
	private static final String RESPONSE_STATUS_WITHOUT_REASON_PATH = "/test/common-error/response-status-exception-without-reason";
	private static final String DOC_VALIDATED_ID_PATH = "/test/common-error/doc-validated/{id}";
	private static final String DOC_VALIDATED_SIZE_PATH = "/test/common-error/doc-validated-size";
	private static final String GUARD_UNMET_PATH = "/test/common-error/business-detail-unmet";
	private static final String SETUP_INCOMPLETE_PATH = "/test/common-error/business-detail-missing";
	private static final String HELP_TOO_SOON_PATH = "/test/common-error/business-detail-next-allowed-at";
	private static final String STATE_VERSION_CONFLICT_PATH = "/test/common-error/business-state-version-conflict";
	private static final String IDEMPOTENCY_KEY_REUSED_PATH = "/test/common-error/business-common-code/IDEMPOTENCY_KEY_REUSED";
	private static final String SECURED_PATH = "/v1/pickup-sessions/1";

	private static final String GUARD_UNMET_CODE = "CALL_GUARD_UNMET";
	private static final String GUARD_UNMET_MESSAGE = "합류 안내를 보낼 수 있는 조건이 아닙니다";
	private static final String SETUP_INCOMPLETE_CODE = "SETUP_INCOMPLETE";
	private static final String SETUP_INCOMPLETE_MESSAGE = "픽업 준비가 끝나지 않았습니다";
	private static final String HELP_TOO_SOON_CODE = "HELP_REQUEST_TOO_SOON";
	private static final String HELP_TOO_SOON_MESSAGE = "잠시 후 다시 요청해 주세요";
	private static final Instant NEXT_ALLOWED_AT_UTC = Instant.parse("2026-11-05T09:10:00Z");
	private static final String NEXT_ALLOWED_AT_KST = "2026-11-05T18:10:00+09:00";
	private static final String STATE_VERSION_CONFLICT_CODE = "STATE_VERSION_CONFLICT";
	private static final long LATEST_SESSION_ID = 42L;
	private static final int LATEST_STATE_VERSION = 7;
	private static final String LATEST_SESSION_STATUS = "ACTIVE";
	private static final String LATEST_VEHICLE_PHASE = "DRIVING";

	private static final long MAX_PAGE_SIZE = 50;
	private static final String SIZE_MAX_REASON = "size는 50 이하여야 합니다";

	private static final String REQUIRED_HEADER_NAME = "X-Test-Header";
	private static final String RESPONSE_STATUS_REASON = "이미 처리된 요청입니다";

	private static final int RETRY_AFTER_SECONDS = 30;
	private static final String DOMAIN_CODE = "SOME_DOMAIN_CODE";
	private static final String DOMAIN_CODE_MESSAGE = "도메인 규칙에 맞지 않는 요청입니다";
	private static final String OVERLAP_CODE = "SCHEDULE_OVERLAP";
	private static final String OVERLAP_MESSAGE = "겹치는 일정이 있습니다";
	private static final long OVERLAP_SCHEDULE_ID_1 = 801L;
	private static final long OVERLAP_SCHEDULE_ID_2 = 802L;

	private static final String VALIDATION_FAILED_MESSAGE = "입력값을 확인해 주세요";
	private static final String INVALID_FIELD_NAME = "phone";
	private static final String INVALID_FIELD_REASON = "자녀가 아닌 멤버는 전화번호가 필요합니다";
	private static final String NAME_BLANK_REASON = "이름을 입력해 주세요";
	private static final String NAME_LENGTH_REASON = "1~20자여야 합니다";
	private static final String GRADE_REQUIRED_REASON = "학년을 입력해 주세요";
	private static final String GRADE_MIN_REASON = "1 이상이어야 합니다";
	private static final String ID_POSITIVE_REASON = "id는 0보다 커야 합니다";
	private static final String BODY_PARAMETER_NAME = "request";
	private static final String BLANK_NAME_BODY = "{\"name\": \"   \", \"grade\": 3}";
	private static final String EMPTY_NAME_BODY = "{\"name\": \"\", \"grade\": 3}";

	private static final String CHILD_ID_PARAM_NAME = "child_id";
	private static final String CHILD_ID_JAVA_NAME = "childId";
	private static final String CHILD_ID_MIN_REASON = "자녀 id는 1 이상이어야 합니다";
	private static final String SAMPLE_KEY_HEADER_NAME = "X-Sample-Key";
	private static final String SAMPLE_KEY_JAVA_NAME = "sampleKey";
	private static final String SAMPLE_KEY_LENGTH_REASON = "8자 이상이어야 합니다";

	private static final String INTERNAL_DETAIL = "db-password=secret-1234 failed in InternalRepository.load";
	private static final String APPLICATION_PACKAGE = "com.example.pickupservice";

	private final MockMvc mockMvc;

	@ParameterizedTest(name = "본문 {0}")
	@ValueSource(strings = {"{\"name\": \"minwoo\"", "{\"name\": }", "not-json"})
	@DisplayName("JSON 형식이 틀린 본문을 보내면 400과 error.code INVALID_REQUEST · 비어 있지 않은 error.message를 돌려주고 error.fields는 채우지 않는다")
	void malformedJsonBodyReturns400WithInvalidRequestError(String malformedBody) throws Exception {
		mockMvc.perform(withJsonBody(post(JSON_BODY_PATH), malformedBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.error.message").isNotEmpty())
				.andExpect(jsonPath("$.error.fields").doesNotExist());
	}

	@Test
	@DisplayName("@Valid 본문의 필드 하나가 검증에 실패하면 422 VALIDATION_FAILED · 입력값을 확인해 주세요와 fields[0].field name · fields[0].reason 제약 메시지를 돌려준다")
	void invalidBodyFieldReturns422WithValidationFailedAndFieldReason() throws Exception {
		mockMvc.perform(withJsonBody(post(VALIDATED_BODY_PATH), BLANK_NAME_BODY))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("name"))
				.andExpect(jsonPath("$.error.fields[0].reason").value(NAME_BLANK_REASON));
	}

	@Test
	@DisplayName("@Valid 본문의 필드 두 개가 검증에 실패하면 422 VALIDATION_FAILED의 fields에 name과 grade가 각자의 reason과 함께 하나씩 들어간다")
	void twoInvalidBodyFieldsReturn422WithEachFieldInFields() throws Exception {
		String nameOver20Chars = "가".repeat(21);

		mockMvc.perform(withJsonBody(post(VALIDATED_BODY_PATH), "{\"name\": \"" + nameOver20Chars + "\", \"grade\": 0}"))
				.andExpect(validationFailedWithFieldCount(2))
				.andExpect(jsonPath("$.error.fields[*].field", containsInAnyOrder("name", "grade")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'name')].reason", contains(NAME_LENGTH_REASON)))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'grade')].reason", contains(GRADE_MIN_REASON)));
	}

	@Test
	@DisplayName("제약을 단 요청 파라미터가 검증에 실패하면 422 VALIDATION_FAILED와 fields[0].field grade · fields[0].reason 제약 메시지를 돌려준다")
	void invalidRequestParamReturns422WithValidationFailedAndFieldReason() throws Exception {
		mockMvc.perform(get(VALIDATED_PARAM_PATH)
						.param("grade", "0"))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("grade"))
				.andExpect(jsonPath("$.error.fields[0].reason").value(GRADE_MIN_REASON));
	}

	@Test
	@DisplayName("@RequestParam(\"child_id\") @Min(1) long childId에 child_id=0을 보내면 422 VALIDATION_FAILED의 fields[0].field는 Java 파라미터 이름 childId가 아니라 애너테이션에 적은 이름 child_id이고 fields[0].reason은 제약 메시지다")
	void namedRequestParamValidationFailureReportsAnnotationNameNotJavaName() throws Exception {
		mockMvc.perform(get(VALIDATED_NAMED_PARAM_PATH)
						.param(CHILD_ID_PARAM_NAME, "0"))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value(CHILD_ID_PARAM_NAME))
				.andExpect(jsonPath("$.error.fields[0].reason").value(CHILD_ID_MIN_REASON))
				.andExpect(jsonPath("$.error.fields[*].field", not(hasItem(CHILD_ID_JAVA_NAME))));
	}

	@Test
	@DisplayName("@RequestHeader(\"X-Sample-Key\") @Size(min = 8) String sampleKey에 3자 값을 보내면 422 VALIDATION_FAILED의 fields[0].field는 Java 파라미터 이름 sampleKey가 아니라 애너테이션에 적은 헤더 이름 X-Sample-Key이고 fields[0].reason은 제약 메시지다")
	void namedRequestHeaderValidationFailureReportsAnnotationNameNotJavaName() throws Exception {
		mockMvc.perform(get(VALIDATED_NAMED_HEADER_PATH)
						.header(SAMPLE_KEY_HEADER_NAME, "abc"))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value(SAMPLE_KEY_HEADER_NAME))
				.andExpect(jsonPath("$.error.fields[0].reason").value(SAMPLE_KEY_LENGTH_REASON))
				.andExpect(jsonPath("$.error.fields[*].field", not(hasItem(SAMPLE_KEY_JAVA_NAME))));
	}

	@Test
	@DisplayName("애너테이션에 이름을 적지 않은 @RequestParam @Min(1) int grade에 grade=0을 보내면 422 VALIDATION_FAILED의 fields[0].field는 Java 파라미터 이름 grade이고 fields[0].reason은 제약 메시지다")
	void unnamedRequestParamValidationFailureReportsJavaParameterName() throws Exception {
		mockMvc.perform(get(VALIDATED_UNNAMED_PARAM_PATH)
						.param("grade", "0"))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("grade"))
				.andExpect(jsonPath("$.error.fields[0].reason").value(GRADE_MIN_REASON));
	}

	@Test
	@DisplayName("@PathVariable @Positive id와 @Valid 본문이 함께 있어 메서드 검증으로 처리될 때 id 1은 올바르고 본문 name이 빈 문자열이면 422 VALIDATION_FAILED의 fields[0].field는 파라미터 이름 request가 아니라 본문 필드 이름 name이고 fields[0].reason은 제약 메시지다")
	void bodyFieldErrorUnderMethodValidationReportsBodyFieldNameNotParameterName() throws Exception {
		mockMvc.perform(withJsonBody(post(VALIDATED_BODY_WITH_ID_PATH, 1L), EMPTY_NAME_BODY))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("name"))
				.andExpect(jsonPath("$.error.fields[0].reason").value(NAME_BLANK_REASON))
				.andExpect(jsonPath("$.error.fields[*].field", not(hasItem(BODY_PARAMETER_NAME))));
	}

	@Test
	@DisplayName("@PathVariable @Positive id와 @Valid 본문이 함께 있어 메서드 검증으로 처리될 때 id 0과 본문 name 빈 문자열이 함께 틀리면 422 VALIDATION_FAILED의 fields에 path 파라미터 id와 본문 필드 name이 각자의 reason과 함께 하나씩 들어가고 request는 들어가지 않는다")
	void pathVariableAndBodyFieldErrorsUnderMethodValidationReportEachFieldName() throws Exception {
		mockMvc.perform(withJsonBody(post(VALIDATED_BODY_WITH_ID_PATH, 0L), EMPTY_NAME_BODY))
				.andExpect(validationFailedWithFieldCount(2))
				.andExpect(jsonPath("$.error.fields[*].field", containsInAnyOrder("id", "name")))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'id')].reason", contains(ID_POSITIVE_REASON)))
				.andExpect(jsonPath("$.error.fields[?(@.field == 'name')].reason", contains(NAME_BLANK_REASON)));
	}

	@Test
	@DisplayName("JSON 형식이 틀린 본문의 400 INVALID_REQUEST 응답은 JSON 본문의 error에 code · message · retryAfterSeconds 키를 담고 retryAfterSeconds는 null이다")
	void malformedJsonErrorBodyHasCommonErrorKeys() throws Exception {
		mockMvc.perform(withJsonBody(post(JSON_BODY_PATH), "not-json"))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));
	}

	@Test
	@DisplayName("Bean Validation 실패의 422 VALIDATION_FAILED 응답은 JSON 본문의 error에 code · message · retryAfterSeconds · fields 키를 담고 retryAfterSeconds는 null이다")
	void validationFailedErrorBodyHasCommonErrorKeysAndFields() throws Exception {
		mockMvc.perform(withJsonBody(post(VALIDATED_BODY_PATH), BLANK_NAME_BODY))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields", hasSize(1)));
	}

	@Test
	@DisplayName("GET만 받는 API를 POST로 부르면 405와 JSON 본문의 error.code METHOD_NOT_ALLOWED · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void wrongHttpMethodReturns405WithCommonErrorBody() throws Exception {
		mockMvc.perform(post(GET_ONLY_PATH))
				.andExpect(commonError(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("JSON 본문을 받는 API에 Content-Type text/plain으로 보내면 415와 JSON 본문의 error.code UNSUPPORTED_MEDIA_TYPE · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void unsupportedContentTypeReturns415WithCommonErrorBody() throws Exception {
		mockMvc.perform(post(JSON_BODY_PATH)
						.contentType(MediaType.TEXT_PLAIN)
						.content("name=minwoo"))
				.andExpect(commonError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	@DisplayName("multipart 한도를 넘어 MaxUploadSizeExceededException이 나면 413과 JSON 본문의 error.code PROFILE_IMAGE_TOO_LARGE · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void maxUploadSizeExceededReturns413ProfileImageTooLarge() throws Exception {
		mockMvc.perform(get(MAX_UPLOAD_SIZE_EXCEEDED_PATH))
				.andExpect(commonError(HttpStatus.CONTENT_TOO_LARGE, "PROFILE_IMAGE_TOO_LARGE"));
	}

	@Test
	@DisplayName("필수 요청 파라미터 grade를 빼고 부르면 400과 JSON 본문의 error.code INVALID_REQUEST · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void missingRequiredParamReturns400WithCommonErrorBody() throws Exception {
		mockMvc.perform(get(REQUIRED_PARAM_PATH))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));
	}

	@Test
	@DisplayName("숫자 요청 파라미터 grade에 abc를 보내면 400과 JSON 본문의 error.code INVALID_REQUEST · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void mismatchedParamTypeReturns400WithCommonErrorBody() throws Exception {
		mockMvc.perform(get(REQUIRED_PARAM_PATH)
						.param("grade", "abc"))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));
	}

	@Test
	@DisplayName("매핑되지 않은 경로를 부르면 404와 JSON 본문의 error.code NOT_FOUND · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void unmappedPathReturns404WithCommonErrorBody() throws Exception {
		mockMvc.perform(get(UNMAPPED_PATH))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));
	}

	@Test
	@DisplayName("컨트롤러가 처리하지 않은 RuntimeException이 나면 500과 JSON 본문의 error.code INTERNAL_SERVER_ERROR · error.message · error.retryAfterSeconds(null) 키를 돌려주고 본문에 예외 메시지 · 예외 클래스 이름 · 스택의 패키지 이름이 들어가지 않는다")
	void unhandledRuntimeExceptionReturns500WithoutInternalDetail() throws Exception {
		mockMvc.perform(get(UNHANDLED_EXCEPTION_PATH))
				.andExpect(commonError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.error.message", not(containsString(INTERNAL_DETAIL))))
				.andExpect(content().string(allOf(
						not(containsString(INTERNAL_DETAIL)),
						not(containsString("secret-1234")),
						not(containsString("IllegalStateException")),
						not(containsString(APPLICATION_PACKAGE)))));
	}

	@Test
	@DisplayName("컨트롤러가 retryAfterSeconds 30을 담은 BusinessException.tooManyRequests(30)을 던지면 429와 JSON 본문의 error.code TOO_MANY_REQUESTS · error.message · error.retryAfterSeconds 30을 돌려주고 error.fields는 채우지 않는다")
	void businessExceptionWithRetryAfterSecondsReturns429WithRetryAfterSeconds() throws Exception {
		mockMvc.perform(get(TOO_MANY_REQUESTS_PATH))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.error", allOf(hasKey("code"), hasKey("message"), hasKey("retryAfterSeconds"))))
				.andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
				.andExpect(jsonPath("$.error.message").isNotEmpty())
				.andExpect(jsonPath("$.error.retryAfterSeconds").value(RETRY_AFTER_SECONDS))
				.andExpect(jsonPath("$.error.fields").doesNotExist());
	}

	@Test
	@DisplayName("컨트롤러가 retryAfterSeconds 없이 BusinessException.of(409 · SOME_DOMAIN_CODE · 메시지)를 던지면 409와 JSON 본문의 error.code SOME_DOMAIN_CODE · 던진 메시지 그대로의 error.message · error.retryAfterSeconds(null)를 돌려주고 error.fields는 채우지 않는다")
	void businessExceptionWithDomainCodeReturnsItsStatusCodeAndMessageAsIs() throws Exception {
		mockMvc.perform(get(DOMAIN_CODE_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, DOMAIN_CODE))
				.andExpect(jsonPath("$.error.message").value(DOMAIN_CODE_MESSAGE));
	}

	@Test
	@DisplayName("컨트롤러가 overlapScheduleIds [801, 802]를 담은 BusinessException.conflictWithOverlap(409 · SCHEDULE_OVERLAP · 메시지 · [801, 802])을 던지면 409와 error.overlapScheduleIds [801, 802]를 돌려주고 error.code · error.message · error.retryAfterSeconds(null) 키는 그대로 있으며 error.fields는 채우지 않는다")
	void businessExceptionWithOverlapScheduleIdsReturns409WithOverlapScheduleIds() throws Exception {
		mockMvc.perform(get(OVERLAP_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, OVERLAP_CODE))
				.andExpect(jsonPath("$.error.message").value(OVERLAP_MESSAGE))
				.andExpect(jsonPath("$.error.overlapScheduleIds", hasSize(2)))
				.andExpect(jsonPath("$.error.overlapScheduleIds[0]").value(OVERLAP_SCHEDULE_ID_1))
				.andExpect(jsonPath("$.error.overlapScheduleIds[1]").value(OVERLAP_SCHEDULE_ID_2));
	}

	@Test
	@DisplayName("overlapScheduleIds를 담지 않은 BusinessException.of(409 · SOME_DOMAIN_CODE · 메시지)를 던지면 409 응답의 error에 overlapScheduleIds 키가 없고 code · message · retryAfterSeconds 키는 그대로 있다")
	void businessExceptionWithoutOverlapScheduleIdsOmitsOverlapScheduleIdsKey() throws Exception {
		mockMvc.perform(get(DOMAIN_CODE_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, DOMAIN_CODE))
				.andExpect(jsonPath("$.error", not(hasKey("overlapScheduleIds"))));
	}

	@Test
	@DisplayName("컨트롤러가 공통 오류 코드로 만든 BusinessException.of(ErrorCode.NOT_FOUND)를 던지면 404와 error.code NOT_FOUND · error.message 대상을 찾을 수 없습니다(ErrorCode 문구) · error.retryAfterSeconds null을 돌려준다")
	void businessExceptionOfCommonNotFoundCodeReturnsItsStatusCodeAndMessage() throws Exception {
		mockMvc.perform(get(COMMON_NOT_FOUND_PATH))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"))
				.andExpect(jsonPath("$.error.message").value("대상을 찾을 수 없습니다"))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.NOT_FOUND.getMessage()));
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(value = ErrorCode.class, names = {"INVALID_REQUEST", "UNAUTHORIZED", "FORBIDDEN",
			"METHOD_NOT_ALLOWED", "IDEMPOTENCY_KEY_REUSED"})
	@DisplayName("컨트롤러가 공통 오류 코드로 만든 BusinessException.of(errorCode)를 던지면 그 코드의 HTTP 상태 · 코드 이름 그대로의 error.code · 그 코드의 문구 error.message · error.retryAfterSeconds null을 돌려준다")
	void businessExceptionOfCommonErrorCodeReturnsThatCodesStatusNameAndMessage(ErrorCode errorCode) throws Exception {
		mockMvc.perform(get(COMMON_ERROR_CODE_PATH, errorCode.name()))
				.andExpect(commonError(errorCode.getStatus(), errorCode.name()))
				.andExpect(jsonPath("$.error.message").value(errorCode.getMessage()));
	}

	@Test
	@DisplayName("서비스가 필드를 지정해 던진 VALIDATION_FAILED는 422 응답의 fields에 그 field와 reason을 담는다")
	void businessExceptionInvalidFieldReturns422WithThatFieldAndReasonInFields() throws Exception {
		mockMvc.perform(get(INVALID_FIELD_PATH))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getMessage()))
				.andExpect(jsonPath("$.error.fields[0].field").value(INVALID_FIELD_NAME))
				.andExpect(jsonPath("$.error.fields[0].reason").value(INVALID_FIELD_REASON));
	}

	@Test
	@DisplayName("필드를 지정하지 않은 BusinessException.of(ErrorCode.VALIDATION_FAILED)를 던지면 422와 error.code VALIDATION_FAILED · ErrorCode 문구를 돌려주고 error.fields는 지금처럼 채우지 않는다")
	void businessExceptionOfValidationFailedWithoutFieldKeepsFieldsAbsent() throws Exception {
		mockMvc.perform(get(COMMON_ERROR_CODE_PATH, ErrorCode.VALIDATION_FAILED.name()))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.VALIDATION_FAILED.getMessage()));
	}

	@Test
	@DisplayName("공통 오류 코드 ErrorCode.TOO_MANY_REQUESTS는 HTTP 429이고 BusinessException.tooManyRequests(30)을 던지면 429와 error.code TOO_MANY_REQUESTS · error.message ErrorCode.TOO_MANY_REQUESTS 문구 · error.retryAfterSeconds 30을 돌려준다")
	void tooManyRequestsBusinessExceptionReturns429WithErrorCodeMessageAndRetryAfterSeconds() throws Exception {
		mockMvc.perform(get(TOO_MANY_REQUESTS_PATH))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.error.code").value(ErrorCode.TOO_MANY_REQUESTS.name()))
				.andExpect(jsonPath("$.error.message").value(ErrorCode.TOO_MANY_REQUESTS.getMessage()))
				.andExpect(jsonPath("$.error.retryAfterSeconds").value(RETRY_AFTER_SECONDS))
				.andExpect(jsonPath("$.error.fields").doesNotExist());

		assertThat(ErrorCode.TOO_MANY_REQUESTS.getStatus())
				.isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	@DisplayName("컨트롤러가 BusinessException.tooManyRequests(ErrorCode.RESEND_TOO_SOON, 30)을 던지면 429와 JSON 본문의 error.code RESEND_TOO_SOON · error.message 「잠시 뒤에 다시 보내 주세요」 · error.retryAfterSeconds 30을 돌려주고 error.fields는 채우지 않는다")
	void resendTooSoonBusinessExceptionReturns429WithCodeMessageAndRetryAfterSeconds() throws Exception {
		mockMvc.perform(get(RESEND_TOO_SOON_PATH))
				.andExpect(status().isTooManyRequests())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.error.code").value("RESEND_TOO_SOON"))
				.andExpect(jsonPath("$.error.message").value("잠시 뒤에 다시 보내 주세요"))
				.andExpect(jsonPath("$.error.retryAfterSeconds").value(RETRY_AFTER_SECONDS))
				.andExpect(jsonPath("$.error.fields").doesNotExist());
	}

	@Test
	@DisplayName("필수 요청 헤더 X-Test-Header를 빼고 부르면 500이 아니라 400과 JSON 본문의 error.code INVALID_REQUEST · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void missingRequiredHeaderReturns400WithInvalidRequestError() throws Exception {
		mockMvc.perform(get(REQUIRED_HEADER_PATH))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));
	}

	@Test
	@DisplayName("JSON만 내는 API(produces application/json)를 Accept application/xml로 부르면 500이 아니라 406과 Content-Type application/json 본문의 error.code NOT_ACCEPTABLE · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void unacceptableAcceptHeaderReturns406WithJsonCommonErrorBody() throws Exception {
		mockMvc.perform(get(JSON_ONLY_PATH)
						.accept(MediaType.APPLICATION_XML))
				.andExpect(commonError(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE"));
	}

	@Test
	@DisplayName("컨트롤러가 ResponseStatusException(409, 이유)을 던지면 500이 아니라 409와 JSON 본문의 error.code CONFLICT · error.message · error.retryAfterSeconds(null) 키를 돌려주고 error.fields는 채우지 않는다")
	void responseStatusExceptionReturnsItsStatusWithCommonErrorBody() throws Exception {
		mockMvc.perform(get(RESPONSE_STATUS_EXCEPTION_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, "CONFLICT"));
	}

	@Test
	@DisplayName("컨트롤러가 ResponseStatusException(409, 이미 처리된 요청입니다)을 던지면 409 응답의 error.message는 던진 reason 이미 처리된 요청입니다 그대로다")
	void responseStatusExceptionReasonIsReturnedAsErrorMessage() throws Exception {
		mockMvc.perform(get(RESPONSE_STATUS_EXCEPTION_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, "CONFLICT"))
				.andExpect(jsonPath("$.error.message").value(RESPONSE_STATUS_REASON));
	}

	@Test
	@DisplayName("컨트롤러가 reason 없이 ResponseStatusException(409)을 던지면 409 응답의 error.code는 CONFLICT이고 error.message는 null이나 빈 문자열이 아닌 기본 문구다")
	void responseStatusExceptionWithoutReasonReturnsNonBlankDefaultMessage() throws Exception {
		mockMvc.perform(get(RESPONSE_STATUS_WITHOUT_REASON_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, "CONFLICT"))
				.andExpect(jsonPath("$.error.message", not(blankOrNullString())));
	}

	@Test
	@DisplayName("스웨거 doc 인터페이스(제약 없음)를 구현한 컨트롤러의 @PathVariable @Positive id에 0을 보내면 500(HV000151)이 아니라 422 VALIDATION_FAILED와 fields[0].field id · fields[0].reason 제약 메시지를 돌려준다")
	void docInterfaceControllerPathVariableConstraintViolationReturns422NotServerError() throws Exception {
		mockMvc.perform(get(DOC_VALIDATED_ID_PATH, 0L))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("id"))
				.andExpect(jsonPath("$.error.fields[0].reason").value(ID_POSITIVE_REASON));
	}

	@Test
	@DisplayName("스웨거 doc 인터페이스(제약 없음)를 구현한 컨트롤러의 @RequestParam(\"size\") @Max(50) size에 100을 보내면 500(HV000151)이 아니라 422 VALIDATION_FAILED와 fields[0].field size · fields[0].reason 제약 메시지를 돌려준다")
	void docInterfaceControllerRequestParamConstraintViolationReturns422NotServerError() throws Exception {
		mockMvc.perform(get(DOC_VALIDATED_SIZE_PATH)
						.param("size", "100"))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error.fields[0].field").value("size"))
				.andExpect(jsonPath("$.error.fields[0].reason").value(SIZE_MAX_REASON));
	}

	@Test
	@DisplayName("스웨거 doc 인터페이스(제약 없음)를 구현한 컨트롤러의 제약 파라미터에 올바른 값(id 7 · size 50)을 보내면 500이 아니라 200과 그 값을 돌려준다")
	void docInterfaceControllerWithValidParametersReturns200() throws Exception {
		mockMvc.perform(get(DOC_VALIDATED_ID_PATH, 7L))
				.andExpect(status().isOk())
				.andExpect(content().string("7"));
		mockMvc.perform(get(DOC_VALIDATED_SIZE_PATH)
						.param("size", String.valueOf(MAX_PAGE_SIZE)))
				.andExpect(status().isOk())
				.andExpect(content().string(String.valueOf(MAX_PAGE_SIZE)));
	}

	@Test
	@DisplayName("컨트롤러가 BusinessException.of(422 · CALL_GUARD_UNMET · 메시지).withDetail(unmet, [G2, G4])를 던지면 422와 error.unmet [G2, G4]를 돌려주고 error.code · error.message · error.retryAfterSeconds(null)는 그대로이며 unmet은 본문 최상위가 아니라 error 안에만 있다")
	void businessExceptionWithUnmetDetailReturnsUnmetArrayInsideError() throws Exception {
		mockMvc.perform(get(GUARD_UNMET_PATH))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, GUARD_UNMET_CODE))
				.andExpect(jsonPath("$.error.message").value(GUARD_UNMET_MESSAGE))
				.andExpect(jsonPath("$.error.unmet", contains("G2", "G4")))
				.andExpect(jsonPath("$.unmet").doesNotExist())
				.andExpect(jsonPath("$", not(hasKey("session"))));
	}

	@Test
	@DisplayName("컨트롤러가 BusinessException.of(422 · SETUP_INCOMPLETE · 메시지).withDetail(missing, [PICKUP_SPOT])를 던지면 422와 error.missing [PICKUP_SPOT]을 돌려주고 error에는 code · message · retryAfterSeconds · missing 네 키만 있다")
	void businessExceptionWithMissingDetailReturnsMissingArrayInsideError() throws Exception {
		mockMvc.perform(get(SETUP_INCOMPLETE_PATH))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, SETUP_INCOMPLETE_CODE))
				.andExpect(jsonPath("$.error.message").value(SETUP_INCOMPLETE_MESSAGE))
				.andExpect(jsonPath("$.error.missing", contains("PICKUP_SPOT")))
				.andExpect(jsonPath("$.error", aMapWithSize(4)));
	}

	@Test
	@DisplayName("컨트롤러가 BusinessException.of(429 · HELP_REQUEST_TOO_SOON · 메시지).withDetail(nextAllowedAt, UTC Instant 2026-11-05T09:10:00Z)를 던지면 429와 error.nextAllowedAt을 한국 시각 문자열 2026-11-05T18:10:00+09:00으로 돌려준다")
	void businessExceptionWithInstantDetailReturnsKstOffsetString() throws Exception {
		mockMvc.perform(get(HELP_TOO_SOON_PATH))
				.andExpect(commonError(HttpStatus.TOO_MANY_REQUESTS, HELP_TOO_SOON_CODE))
				.andExpect(jsonPath("$.error.message").value(HELP_TOO_SOON_MESSAGE))
				.andExpect(jsonPath("$.error.nextAllowedAt").value(NEXT_ALLOWED_AT_KST));
	}

	@Test
	@DisplayName("컨트롤러가 BusinessException.stateVersionConflict(최신 세션 뷰)를 던지면 409와 본문 최상위에 error와 session 두 키를 돌려주고 session에는 세션 뷰(sessionId 42 · stateVersion 7 · status.session ACTIVE · status.vehicle DRIVING)가, error에는 code STATE_VERSION_CONFLICT · message · retryAfterSeconds(null) 세 키만 있다")
	void stateVersionConflictReturns409WithLatestSessionViewBesideError() throws Exception {
		mockMvc.perform(get(STATE_VERSION_CONFLICT_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, STATE_VERSION_CONFLICT_CODE))
				.andExpect(jsonPath("$", aMapWithSize(2)))
				.andExpect(jsonPath("$", allOf(hasKey("error"), hasKey("session"))))
				.andExpect(jsonPath("$.error", aMapWithSize(3)))
				.andExpect(jsonPath("$.session.sessionId").value(LATEST_SESSION_ID))
				.andExpect(jsonPath("$.session.stateVersion").value(LATEST_STATE_VERSION))
				.andExpect(jsonPath("$.session.status.session").value(LATEST_SESSION_STATUS))
				.andExpect(jsonPath("$.session.status.vehicle").value(LATEST_VEHICLE_PHASE));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {TOO_MANY_REQUESTS_PATH, DOMAIN_CODE_PATH, OVERLAP_PATH, INVALID_FIELD_PATH,
			IDEMPOTENCY_KEY_REUSED_PATH, GUARD_UNMET_PATH, UNHANDLED_EXCEPTION_PATH, UNMAPPED_PATH})
	@DisplayName("STATE_VERSION_CONFLICT가 아닌 오류 응답 본문의 최상위에는 error 키 하나만 있고 session 키는 null 값으로도 나오지 않는다")
	void nonStateVersionConflictErrorBodyHasOnlyErrorKeyWithoutSession(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$", hasKey("error")))
				.andExpect(jsonPath("$", not(hasKey("session"))));
	}

	@Test
	@DisplayName("추가 키를 넣지 않은 BusinessException.of(409 · SOME_DOMAIN_CODE · 메시지)의 응답 error에는 code · message · retryAfterSeconds 세 키만 있다")
	void businessExceptionWithoutDetailsHasOnlyThreeCommonErrorKeys() throws Exception {
		mockMvc.perform(get(DOMAIN_CODE_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, DOMAIN_CODE))
				.andExpect(jsonPath("$.error", aMapWithSize(3)));
	}

	@Test
	@DisplayName("conflictWithOverlap(409 · SCHEDULE_OVERLAP · 메시지 · [801, 802]) 응답의 error에는 지금처럼 code · message · retryAfterSeconds · overlapScheduleIds 네 키만 있다")
	void overlapErrorBodyHasExactlyCommonKeysAndOverlapScheduleIds() throws Exception {
		mockMvc.perform(get(OVERLAP_PATH))
				.andExpect(commonError(HttpStatus.CONFLICT, OVERLAP_CODE))
				.andExpect(jsonPath("$.error", allOf(aMapWithSize(4), hasKey("overlapScheduleIds"))));
	}

	@Test
	@DisplayName("서비스가 필드를 지정해 던진 VALIDATION_FAILED 응답의 error에는 지금처럼 code · message · retryAfterSeconds · fields 네 키만 있다")
	void invalidFieldErrorBodyHasExactlyCommonKeysAndFields() throws Exception {
		mockMvc.perform(get(INVALID_FIELD_PATH))
				.andExpect(validationFailedWithFieldCount(1))
				.andExpect(jsonPath("$.error", allOf(aMapWithSize(4), hasKey("fields"))));
	}

	@Test
	@DisplayName("토큰 없이 인증이 필요한 경로를 부르면 필터 단계(ErrorResponseWriter)의 401 본문은 지금처럼 최상위에 error 하나만 있고 error에는 code UNAUTHORIZED · message · retryAfterSeconds(null) 세 키만 있다")
	void filterStageUnauthorizedBodyKeepsCommonShapeWithoutSession() throws Exception {
		mockMvc.perform(get(SECURED_PATH))
				.andExpect(commonError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"))
				.andExpect(jsonPath("$", aMapWithSize(1)))
				.andExpect(jsonPath("$.error", aMapWithSize(3)));
	}

	private static MockHttpServletRequestBuilder withJsonBody(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static ResultMatcher validationFailedWithFieldCount(int fieldCount) {
		return result -> {
			status().isUnprocessableContent().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.message").value(VALIDATION_FAILED_MESSAGE).match(result);
			jsonPath("$.error.fields", hasSize(fieldCount)).match(result);
		};
	}

	record JsonBodyRequest(String name) {
	}

	record SessionViewStub(long sessionId, int stateVersion, SessionStatusStub status) {
	}

	record SessionStatusStub(String session, String vehicle) {
	}

	record ValidatedBodyRequest(
			@NotBlank(message = NAME_BLANK_REASON) @Size(max = 20, message = NAME_LENGTH_REASON) String name,
			@NotNull(message = GRADE_REQUIRED_REASON) @Min(value = 1, message = GRADE_MIN_REASON) Integer grade) {
	}

	@RestController
	static class JsonBodyTestController {

		@PostMapping(JSON_BODY_PATH)
		public JsonBodyRequest echo(@RequestBody JsonBodyRequest request) {
			return request;
		}

	}

	@RestController
	static class ValidationTestController {

		@PostMapping(VALIDATED_BODY_PATH)
		public ValidatedBodyRequest echo(@Valid @RequestBody ValidatedBodyRequest request) {
			return request;
		}

		@PostMapping(VALIDATED_BODY_WITH_ID_PATH)
		public ValidatedBodyRequest echoWithId(
				@PathVariable("id") @Positive(message = ID_POSITIVE_REASON) Long id,
				@Valid @RequestBody ValidatedBodyRequest request) {
			return request;
		}

		@GetMapping(VALIDATED_PARAM_PATH)
		public int echoGrade(@RequestParam("grade") @Min(value = 1, message = GRADE_MIN_REASON) int grade) {
			return grade;
		}

		@GetMapping(VALIDATED_NAMED_PARAM_PATH)
		public long echoChildId(@RequestParam(CHILD_ID_PARAM_NAME) @Min(value = 1, message = CHILD_ID_MIN_REASON) long childId) {
			return childId;
		}

		@GetMapping(VALIDATED_NAMED_HEADER_PATH)
		public String echoSampleKey(
				@RequestHeader(SAMPLE_KEY_HEADER_NAME) @Size(min = 8, message = SAMPLE_KEY_LENGTH_REASON) String sampleKey) {
			return sampleKey;
		}

		@GetMapping(VALIDATED_UNNAMED_PARAM_PATH)
		public int echoUnnamedGrade(@RequestParam @Min(value = 1, message = GRADE_MIN_REASON) int grade) {
			return grade;
		}

	}

	@RestController
	static class CommonErrorShapeTestController {

		@GetMapping(GET_ONLY_PATH)
		public String getOnly() {
			return "ok";
		}

		@GetMapping(REQUIRED_PARAM_PATH)
		public int requiredGrade(@RequestParam("grade") int grade) {
			return grade;
		}

		@GetMapping(UNHANDLED_EXCEPTION_PATH)
		public String throwUnhandled() {
			throw new IllegalStateException(INTERNAL_DETAIL);
		}

	}

	@RestController
	static class BusinessExceptionTestController {

		@GetMapping(TOO_MANY_REQUESTS_PATH)
		public String throwTooManyRequests() {
			throw BusinessException.tooManyRequests(RETRY_AFTER_SECONDS);
		}

		@GetMapping(RESEND_TOO_SOON_PATH)
		public String throwResendTooSoon() {
			throw BusinessException.tooManyRequests(ErrorCode.RESEND_TOO_SOON, RETRY_AFTER_SECONDS);
		}

		@GetMapping(DOMAIN_CODE_PATH)
		public String throwDomainCode() {
			throw BusinessException.of(HttpStatus.CONFLICT, DOMAIN_CODE, DOMAIN_CODE_MESSAGE);
		}

		@GetMapping(COMMON_NOT_FOUND_PATH)
		public String throwCommonNotFound() {
			throw BusinessException.of(ErrorCode.NOT_FOUND);
		}

		@GetMapping(COMMON_ERROR_CODE_PATH)
		public String throwCommonErrorCode(@PathVariable("code") ErrorCode code) {
			throw BusinessException.of(code);
		}

		@GetMapping(INVALID_FIELD_PATH)
		public String throwInvalidField() {
			throw BusinessException.invalidField(INVALID_FIELD_NAME, INVALID_FIELD_REASON);
		}

		@GetMapping(OVERLAP_PATH)
		public String throwOverlap() {
			throw BusinessException.conflictWithOverlap(HttpStatus.CONFLICT, OVERLAP_CODE, OVERLAP_MESSAGE,
					List.of(OVERLAP_SCHEDULE_ID_1, OVERLAP_SCHEDULE_ID_2));
		}

		@GetMapping(GUARD_UNMET_PATH)
		public String throwGuardUnmet() {
			throw BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, GUARD_UNMET_CODE, GUARD_UNMET_MESSAGE)
					.withDetail("unmet", List.of("G2", "G4"));
		}

		@GetMapping(SETUP_INCOMPLETE_PATH)
		public String throwSetupIncomplete() {
			throw BusinessException.of(HttpStatus.UNPROCESSABLE_CONTENT, SETUP_INCOMPLETE_CODE, SETUP_INCOMPLETE_MESSAGE)
					.withDetail("missing", List.of("PICKUP_SPOT"));
		}

		@GetMapping(HELP_TOO_SOON_PATH)
		public String throwHelpTooSoon() {
			throw BusinessException.of(HttpStatus.TOO_MANY_REQUESTS, HELP_TOO_SOON_CODE, HELP_TOO_SOON_MESSAGE)
					.withDetail("nextAllowedAt", NEXT_ALLOWED_AT_UTC);
		}

		@GetMapping(STATE_VERSION_CONFLICT_PATH)
		public String throwStateVersionConflict() {
			throw BusinessException.stateVersionConflict(new SessionViewStub(LATEST_SESSION_ID, LATEST_STATE_VERSION,
					new SessionStatusStub(LATEST_SESSION_STATUS, LATEST_VEHICLE_PHASE)));
		}

	}

	@RestController
	static class MvcRequestExceptionTestController {

		@GetMapping(REQUIRED_HEADER_PATH)
		public String requiredHeader(@RequestHeader(REQUIRED_HEADER_NAME) String header) {
			return header;
		}

		@GetMapping(value = JSON_ONLY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
		public JsonBodyRequest jsonOnly() {
			return new JsonBodyRequest("minwoo");
		}

		@GetMapping(RESPONSE_STATUS_EXCEPTION_PATH)
		public String throwResponseStatus() {
			throw new ResponseStatusException(HttpStatus.CONFLICT, RESPONSE_STATUS_REASON);
		}

		@GetMapping(RESPONSE_STATUS_WITHOUT_REASON_PATH)
		public String throwResponseStatusWithoutReason() {
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}

		@GetMapping(MAX_UPLOAD_SIZE_EXCEEDED_PATH)
		public String throwMaxUploadSizeExceeded() {
			throw new MaxUploadSizeExceededException(MULTIPART_MAX_REQUEST_SIZE);
		}

	}

	@Tag(name = "doc-validated-test")
	interface DocValidatedTestApiDoc {

		@Operation(summary = "id로 조회")
		long findById(@Parameter(description = "대상 id") Long id);

		@Operation(summary = "개수를 정해 조회")
		long list(@Parameter(description = "한 번에 받을 개수") int size);

	}

	@RestController
	static class DocValidatedTestController implements DocValidatedTestApiDoc {

		@Override
		@GetMapping(DOC_VALIDATED_ID_PATH)
		public long findById(@PathVariable("id") @Positive(message = ID_POSITIVE_REASON) Long id) {
			return id;
		}

		@Override
		@GetMapping(DOC_VALIDATED_SIZE_PATH)
		public long list(@RequestParam("size") @Max(value = MAX_PAGE_SIZE, message = SIZE_MAX_REASON) int size) {
			return size;
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	@Import({JsonBodyTestController.class, ValidationTestController.class, CommonErrorShapeTestController.class,
			BusinessExceptionTestController.class, MvcRequestExceptionTestController.class,
			DocValidatedTestController.class})
	static class ErrorTestEndpointConfiguration {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain errorTestEndpointSecurityFilterChain(HttpSecurity http) throws Exception {
			http.securityMatcher(TEST_ENDPOINT_PATTERN)
					.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
					.csrf(AbstractHttpConfigurer::disable);
			return http.build();
		}

	}

}
