package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class ScheduleControllerTest extends ScheduleWebTestSupport {

	private static final String ACADEMIES_PATH = "/v1/places/academies";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 111L;
	private static final long DELEGATE_USER_ID = 121L;
	private static final long VEHICLE_ID = 77L;
	private static final String QUERY = "해솔";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("GET /v1/places/academies는 200과 items[](externalPlaceId · name · address · location{lat, lng})를 돌려준다")
	void searchAcademiesReturns200WithItems() throws Exception {
		given(searchAcademyUseCase.searchAcademies(any())).willReturn(List.of(
				new AcademyCandidate("kakao:12345", "해솔영어학원", "서울 강남구 테헤란로 1", new LatLng(37.501234, 127.039876))));

		mockMvc.perform(searchRequest(guardianBearer(), QUERY))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[
						  {"externalPlaceId":"kakao:12345",
						   "name":"해솔영어학원",
						   "address":"서울 강남구 테헤란로 1",
						   "location":{"lat":37.501234,"lng":127.039876}}
						]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/places/academies는 query를 그대로 유스케이스에 넘긴다")
	void searchAcademiesPassesQueryToUseCase() throws Exception {
		given(searchAcademyUseCase.searchAcademies(any())).willReturn(List.of());

		mockMvc.perform(searchRequest(guardianBearer(), QUERY))
				.andExpect(status().isOk());

		then(searchAcademyUseCase).should().searchAcademies(QUERY);
	}

	@Test
	@DisplayName("GET /v1/places/academies는 결과가 없으면 200과 빈 items[]를 돌려준다")
	void searchAcademiesWithNoResultReturns200WithEmptyItems() throws Exception {
		given(searchAcademyUseCase.searchAcademies(any())).willReturn(List.of());

		mockMvc.perform(searchRequest(guardianBearer(), QUERY))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/places/academies를 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void searchAcademiesWithCoGuardianTokenReturns200() throws Exception {
		given(searchAcademyUseCase.searchAcademies(any())).willReturn(List.of());

		mockMvc.perform(searchRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), QUERY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("GET /v1/places/academies에 1자 query를 보내면 422 VALIDATION_FAILED와 fields의 query를 돌려주고 유스케이스를 부르지 않는다")
	void searchAcademiesWith1CharQueryReturns422ValidationFailed() throws Exception {
		mockMvc.perform(searchRequest(guardianBearer(), "해"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("query")));

		then(searchAcademyUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/places/academies에 51자 query를 보내면 422 VALIDATION_FAILED와 fields의 query를 돌려주고 유스케이스를 부르지 않는다")
	void searchAcademiesWith51CharQueryReturns422ValidationFailed() throws Exception {
		mockMvc.perform(searchRequest(guardianBearer(), "가".repeat(51)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("query")));

		then(searchAcademyUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/places/academies에 2자 · 50자 query를 보내면 200을 돌려준다")
	void searchAcademiesWithBoundaryLengthQueryReturns200() throws Exception {
		given(searchAcademyUseCase.searchAcademies(any())).willReturn(List.of());

		mockMvc.perform(searchRequest(guardianBearer(), "해솔"))
				.andExpect(status().isOk());
		mockMvc.perform(searchRequest(guardianBearer(), "가".repeat(50)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("GET /v1/places/academies가 외부 API 오류면 502 MAP_API_UNAVAILABLE을 돌려준다")
	void searchAcademiesWhenMapApiFailsReturns502MapApiUnavailable() throws Exception {
		given(searchAcademyUseCase.searchAcademies(any())).willThrow(BusinessException.of(
				HttpStatus.BAD_GATEWAY, "MAP_API_UNAVAILABLE", "지도 서비스를 잠시 쓸 수 없습니다"));

		mockMvc.perform(searchRequest(guardianBearer(), QUERY))
				.andExpect(commonError(HttpStatus.BAD_GATEWAY, "MAP_API_UNAVAILABLE"));
	}

	@Test
	@DisplayName("GET /v1/places/academies를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void searchAcademiesWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(searchRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE), QUERY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(searchAcademyUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/places/academies를 VEHICLE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void searchAcademiesWithVehicleTokenReturns403Forbidden() throws Exception {
		String vehicleToken = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		mockMvc.perform(searchRequest(bearer(vehicleToken), QUERY))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(searchAcademyUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder searchRequest(String bearer, String query) {
		return get(ACADEMIES_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.queryParam("query", query);
	}

	private String guardianBearer() {
		return userBearer(GUARDIAN_USER_ID, Role.GUARDIAN);
	}

	private String userBearer(long userId, Role role) {
		return bearer(accessTokenFixture.user(userId, FAMILY_ID, role));
	}

}
