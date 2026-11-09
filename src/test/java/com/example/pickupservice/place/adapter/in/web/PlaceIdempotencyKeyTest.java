package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.place.application.port.in.SpotGroups;
import com.example.pickupservice.place.application.port.in.SpotView;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class PlaceIdempotencyKeyTest extends PlaceWebTestSupport {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long VEHICLE_ID = 77L;
	private static final long SPOT_ID = 21L;
	private static final String SPOTS_PATH = "/v1/families/me/pickup-spots";
	private static final String SPOT_PATH = "/v1/pickup-spots/{spotId}";
	private static final String ADD_SPOT_BODY = """
			{"academyId":55,"name":"정문 앞",
			 "stopPoint":{"lat":37.501234,"lng":127.039876},
			 "meetingPoint":{"lat":37.501301,"lng":127.039901},
			 "memo":"편의점 앞","isDefault":true}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("Idempotency-Key는 POST /v1/families/me/pickup-spots · DELETE /v1/pickup-spots/{spotId}에만 요구된다")
	void idempotencyKeyIsRequiredOnlyForAddSpotAndDeleteSpot() {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(new SpotGroups(List.of()));
		given(updatePickupSpotUseCase.updateSpot(any())).willReturn(new SpotView(SPOT_ID, "후문 앞",
				new LatLng(37.501111, 127.039222),
				new LatLng(37.501333, 127.039444),
				"카페 앞", true));

		assertAll(
				() -> mockMvc.perform(withGuardian(post(SPOTS_PATH)
								.contentType(MediaType.APPLICATION_JSON)
								.content(ADD_SPOT_BODY)))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(delete(SPOT_PATH, SPOT_ID)))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(get(SPOTS_PATH)))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withGuardian(patch(SPOT_PATH, SPOT_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"name":"후문 앞"}
										""")))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withVehicle(get(SPOTS_PATH)))
						.andExpect(status().isOk()));

		then(addPickupSpotUseCase).shouldHaveNoInteractions();
		then(deletePickupSpotUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder withGuardian(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION,
				bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN)));
	}

	private MockHttpServletRequestBuilder withVehicle(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer(accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID)));
	}

	private static ResultMatcher missingIdempotencyKey() {
		return result -> {
			status().isUnprocessableContent().match(result);
			commonErrorKeys().match(result);
			jsonPath("$.error.code").value("VALIDATION_FAILED").match(result);
			jsonPath("$.error.fields", hasSize(1)).match(result);
			jsonPath("$.error.fields[0].field").value(IDEMPOTENCY_KEY_HEADER).match(result);
		};
	}

}
