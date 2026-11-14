package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.NextPickup;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class ChildScheduleControllerTest extends ScheduleWebTestSupport {

	private static final String NEXT_PICKUP_PATH = "/v1/children/me/next-pickup";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 131L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	// ---- SET-12 GET /v1/children/me/next-pickup (#79)

	@Test
	@DisplayName("GET /v1/children/me/next-pickup은 CHILD 토큰으로 200과 academyName · spotName · targetAt(KST +09:00)을 돌려준다")
	void getNextPickupWithChildTokenReturns200WithAcademyNameSpotNameAndKstTargetAt() throws Exception {
		given(getNextPickupUseCase.getNextPickup(anyLong(), anyLong())).willReturn(Optional.of(new NextPickup(
				"해솔영어학원", "정문 앞", OffsetDateTime.parse("2026-11-16T18:05:00+09:00").toInstant())));

		mockMvc.perform(nextPickupRequest(childBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"academyName":"해솔영어학원","spotName":"정문 앞","targetAt":"2026-11-16T18:05:00+09:00"}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/children/me/next-pickup은 기본 장소가 없으면 spotName을 null로 돌려준다")
	void getNextPickupReturnsNullSpotNameWhenDefaultSpotIsMissing() throws Exception {
		given(getNextPickupUseCase.getNextPickup(anyLong(), anyLong())).willReturn(Optional.of(new NextPickup(
				"해솔영어학원", null, OffsetDateTime.parse("2026-11-16T18:05:00+09:00").toInstant())));

		mockMvc.perform(nextPickupRequest(childBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"academyName":"해솔영어학원","spotName":null,"targetAt":"2026-11-16T18:05:00+09:00"}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/children/me/next-pickup은 CHILD 토큰의 familyId와 userId를 유스케이스에 넘긴다")
	void getNextPickupPassesFamilyIdAndChildUserIdFromTokenToUseCase() throws Exception {
		given(getNextPickupUseCase.getNextPickup(anyLong(), anyLong())).willReturn(Optional.empty());

		mockMvc.perform(nextPickupRequest(childBearer()))
				.andExpect(status().isOk());

		then(getNextPickupUseCase).should().getNextPickup(FAMILY_ID, CHILD_USER_ID);
	}

	@Test
	@DisplayName("GET /v1/children/me/next-pickup은 남은 일정이 없으면 200, Content-Type application/json, 본문 JSON null을 돌려준다")
	void getNextPickupWithoutRemainingScheduleReturns200WithJsonNullBody() throws Exception {
		given(getNextPickupUseCase.getNextPickup(anyLong(), anyLong())).willReturn(Optional.empty());

		mockMvc.perform(nextPickupRequest(childBearer()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().string("null"));
	}

	@Test
	@DisplayName("GET /v1/children/me/next-pickup을 GUARDIAN 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void getNextPickupWithGuardianTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(nextPickupRequest(bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN))))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getNextPickupUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/children/me/next-pickup을 차량 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void getNextPickupWithVehicleTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(nextPickupRequest(bearer(accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID))))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getNextPickupUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder nextPickupRequest(String bearer) {
		return get(NEXT_PICKUP_PATH).header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private String childBearer() {
		return bearer(accessTokenFixture.user(CHILD_USER_ID, FAMILY_ID, Role.CHILD));
	}

}
