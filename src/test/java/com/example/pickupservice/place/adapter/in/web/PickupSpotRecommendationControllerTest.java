package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.place.application.port.in.RecommendSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotRecommendations;
import com.example.pickupservice.place.domain.LineKind;
import com.example.pickupservice.place.domain.RecommendedSpot;
import com.example.pickupservice.place.domain.SpotCandidate;
import com.example.pickupservice.place.domain.SpotCaution;
import com.example.pickupservice.place.domain.SpotKind;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class PickupSpotRecommendationControllerTest extends PlaceWebTestSupport {

	private static final String PATH = "/v1/pickup-spot-recommendations";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_FAMILY_ID = 34L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long VEHICLE_ID = 77L;
	private static final long ACADEMY_ID = 55L;
	private static final String ORIGIN = "37.49611,127.05278";
	private static final LatLng ORIGIN_POINT = new LatLng(37.49611, 127.05278);
	private static final String FUTURE_ARRIVE_AT = "2099-11-13T22:00:00+09:00";
	private static final Instant FUTURE_ARRIVE_INSTANT = Instant.parse("2099-11-13T13:00:00Z");
	private static final String PAST_ARRIVE_AT = "2020-11-13T22:00:00+09:00";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	// ---- SPOT-04 GET /v1/pickup-spot-recommendations (#79 · #80 · #81 · #82)

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations는 200과 supported · items[](rank · kind · name · location · walkMeters · walkSeconds · driveSeconds · totalSeconds · cautions)를 돌려준다")
	void recommendReturns200WithSupportedAndItems() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any())).willReturn(sampleRecommendations());

		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"supported":true,"items":[
						  {"rank":1,"kind":"CURB_STOP","name":"영동대로50길",
						   "location":{"lat":37.498527,"lng":127.069331},
						   "walkMeters":346,"walkSeconds":315,"driveSeconds":450,"totalSeconds":765,
						   "cautions":["LINE_DATA_2021","STOP_5MIN_ONLY"]},
						  {"rank":2,"kind":"PUBLIC_PARKING","name":"삼성동 공영주차장",
						   "location":{"lat":37.497001,"lng":127.060002},
						   "walkMeters":120,"walkSeconds":110,"driveSeconds":null,"totalSeconds":null,
						   "cautions":[]}]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations는 지원 지역 밖이면 200과 supported false, 빈 items[]를 돌려준다")
	void recommendOutsideServiceAreaReturns200WithEmptyItems() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any()))
				.willReturn(new SpotRecommendations(false, List.of()));

		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"supported":false,"items":[]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations는 academyId · origin(lat,lng) · arriveAt을 RecommendSpotsQuery(academyId, LatLng, Instant)로 바꿔 넘긴다")
	void recommendPassesConvertedQueryToUseCase() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any())).willReturn(sampleRecommendations());

		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN)
						.queryParam("arriveAt", FUTURE_ARRIVE_AT))
				.andExpect(status().isOk());

		assertThat(capturedQuery()).isEqualTo(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN_POINT, FUTURE_ARRIVE_INSTANT));
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations는 arriveAt을 안 보내면 null로 넘긴다")
	void recommendWithoutArriveAtPassesNullToUseCase() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any())).willReturn(sampleRecommendations());

		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(status().isOk());

		assertThat(capturedQuery()).isEqualTo(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN_POINT, null));
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CO_GUARDIAN", "DELEGATE"})
	@DisplayName("GET /v1/pickup-spot-recommendations는 CO_GUARDIAN · DELEGATE 토큰으로도 200을 돌려준다")
	void recommendWithCoGuardianOrDelegateTokenReturns200(Role role) throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any())).willReturn(sampleRecommendations());

		mockMvc.perform(recommendRequest(bearer(accessTokenFixture.user(GUARDIAN_USER_ID + 10, FAMILY_ID, role)))
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2));
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations는 차량 토큰으로도 200과 supported · items[]를 돌려준다")
	void recommendWithVehicleTokenReturns200() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any())).willReturn(sampleRecommendations());

		mockMvc.perform(recommendRequest(vehicleBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.supported").value(true))
				.andExpect(jsonPath("$.items[0].rank").value(1))
				.andExpect(jsonPath("$.items[0].name").value("영동대로50길"));
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations에 academyId가 없으면 422 VALIDATION_FAILED와 fields의 academyId를 돌려주고 유스케이스를 부르지 않는다")
	void recommendWithoutAcademyIdReturns422ValidationFailed() throws Exception {
		mockMvc.perform(recommendRequest(guardianBearer()).queryParam("origin", ORIGIN))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("academyId")));

		then(recommendPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations에 origin이 없으면 422 VALIDATION_FAILED와 fields의 origin을 돌려주고 유스케이스를 부르지 않는다")
	void recommendWithoutOriginReturns422ValidationFailed() throws Exception {
		mockMvc.perform(recommendRequest(guardianBearer()).queryParam("academyId", String.valueOf(ACADEMY_ID)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("origin")));

		then(recommendPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@ValueSource(strings = {"abc", "37.5"})
	@DisplayName("GET /v1/pickup-spot-recommendations에 lat,lng 형식이 아닌 origin(abc · 37.5)을 보내면 422 VALIDATION_FAILED와 fields의 origin을 돌려주고 유스케이스를 부르지 않는다")
	void recommendWithMalformedOriginReturns422ValidationFailed(String origin) throws Exception {
		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", origin))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("origin")));

		then(recommendPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations에 숫자가 아닌 academyId를 보내면 422 VALIDATION_FAILED와 fields의 academyId를 돌려주고 유스케이스를 부르지 않는다")
	void recommendWithNonNumericAcademyIdReturns422ValidationFailed() throws Exception {
		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", "abc")
						.queryParam("origin", ORIGIN))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("academyId")));

		then(recommendPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations에 과거 arriveAt을 보내면 422 VALIDATION_FAILED와 fields의 arriveAt을 돌려주고 유스케이스를 부르지 않는다")
	void recommendWithPastArriveAtReturns422ValidationFailed() throws Exception {
		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN)
						.queryParam("arriveAt", PAST_ARRIVE_AT))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("arriveAt")));

		then(recommendPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void recommendWithTeacherTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(recommendRequest(bearer(accessTokenFixture.user(GUARDIAN_USER_ID + 20, FAMILY_ID, Role.TEACHER)))
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(recommendPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations는 Idempotency-Key 없이 보내도 422 없이 200을 돌려준다")
	void recommendWithoutIdempotencyKeyReturns200() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any())).willReturn(sampleRecommendations());

		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.supported").value(true));

		then(recommendPickupSpotsUseCase).should().recommendSpots(any());
	}

	@Test
	@DisplayName("GET /v1/pickup-spot-recommendations가 유스케이스에서 던진 NOT_FOUND를 404 NOT_FOUND로 돌려준다")
	void recommendWithUnknownAcademyIdReturns404NotFound() throws Exception {
		given(recommendPickupSpotsUseCase.recommendSpots(any()))
				.willThrow(BusinessException.of(ErrorCode.NOT_FOUND));

		mockMvc.perform(recommendRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("origin", ORIGIN))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));

		then(recommendPickupSpotsUseCase).should().recommendSpots(any());
	}

	private RecommendSpotsQuery capturedQuery() {
		ArgumentCaptor<RecommendSpotsQuery> captor = ArgumentCaptor.forClass(RecommendSpotsQuery.class);
		then(recommendPickupSpotsUseCase).should().recommendSpots(captor.capture());
		return captor.getValue();
	}

	private static SpotRecommendations sampleRecommendations() {
		return new SpotRecommendations(true, List.of(
				new RecommendedSpot(1,
						new SpotCandidate(SpotKind.CURB_STOP, "영동대로50길",
								new LatLng(37.498527, 127.069331), 346, 315, LineKind.DOTTED_YELLOW, false),
						450, 765, List.of(SpotCaution.LINE_DATA_2021, SpotCaution.STOP_5MIN_ONLY)),
				new RecommendedSpot(2,
						new SpotCandidate(SpotKind.PUBLIC_PARKING, "삼성동 공영주차장",
								new LatLng(37.497001, 127.060002), 120, 110, null, false),
						null, null, List.of())));
	}

	private MockHttpServletRequestBuilder recommendRequest(String bearer) {
		return get(PATH).header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private String guardianBearer() {
		return bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN));
	}

	private String vehicleBearer() {
		return bearer(accessTokenFixture.vehicle(VEHICLE_ID, VEHICLE_FAMILY_ID));
	}

}
