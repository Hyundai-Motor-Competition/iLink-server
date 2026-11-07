package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.place.application.port.in.AcademySpots;
import com.example.pickupservice.place.application.port.in.AddSpotCommand;
import com.example.pickupservice.place.application.port.in.DeleteSpotCommand;
import com.example.pickupservice.place.application.port.in.ListSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotCreated;
import com.example.pickupservice.place.application.port.in.SpotGroups;
import com.example.pickupservice.place.application.port.in.SpotItem;
import com.example.pickupservice.place.application.port.in.SpotView;
import com.example.pickupservice.place.application.port.in.UpdateSpotCommand;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class PickupSpotControllerTest extends PlaceWebTestSupport {

	private static final String SPOTS_PATH = "/v1/families/me/pickup-spots";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_FAMILY_ID = 34L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long VEHICLE_ID = 77L;
	private static final long ACADEMY_ID = 55L;
	private static final String NEAR = "37.501234,127.039876";
	private static final LatLng NEAR_POINT = new LatLng(37.501234, 127.039876);
	private static final long SPOT_ID = 21L;
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final String SPOT_PATH = "/v1/pickup-spots/{spotId}";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	// ---- SPOT-01 GET /v1/families/me/pickup-spots (#29 · #30 · #31 · #32)

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots는 200과 academies[](academyId · academyName · spots[])를 isDefault 키와 null walkMinutes 그대로 돌려준다")
	void listSpotsReturns200WithAcademies() throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(sampleSpotGroups());

		mockMvc.perform(spotsRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"academies":[{"academyId":55,"academyName":"해솔영어학원",
						  "spots":[{"spotId":21,"name":"정문 앞",
						            "stopPoint":{"lat":37.501234,"lng":127.039876},
						            "meetingPoint":{"lat":37.501301,"lng":127.039901},
						            "memo":"편의점 앞","isDefault":true,"walkMinutes":null}]}]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots는 장소가 없으면 200과 빈 academies[]를 돌려준다")
	void listSpotsWithNoSpotsReturns200WithEmptyAcademies() throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(new SpotGroups(List.of()));

		mockMvc.perform(spotsRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"academies":[]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots는 쿼리가 없으면 토큰의 familyId와 null academyId · near · radiusM을 ListSpotsQuery로 넘긴다")
	void listSpotsWithoutQueryPassesFamilyIdAndNullsToUseCase() throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(new SpotGroups(List.of()));

		mockMvc.perform(spotsRequest(guardianBearer()))
				.andExpect(status().isOk());

		assertThat(capturedQuery()).isEqualTo(new ListSpotsQuery(FAMILY_ID, null, null, null));
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots는 academyId · near(lat,lng) · radiusM을 ListSpotsQuery(familyId, academyId, LatLng, radiusM)로 바꿔 넘긴다")
	void listSpotsWithAcademyIdNearAndRadiusPassesConvertedQueryToUseCase() throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(new SpotGroups(List.of()));

		mockMvc.perform(spotsRequest(guardianBearer())
						.queryParam("academyId", String.valueOf(ACADEMY_ID))
						.queryParam("near", NEAR)
						.queryParam("radiusM", "500"))
				.andExpect(status().isOk());

		assertThat(capturedQuery()).isEqualTo(new ListSpotsQuery(FAMILY_ID, ACADEMY_ID, NEAR_POINT, 500));
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CO_GUARDIAN", "DELEGATE"})
	@DisplayName("GET /v1/families/me/pickup-spots는 CO_GUARDIAN · DELEGATE 토큰으로도 200을 돌려준다")
	void listSpotsWithCoGuardianOrDelegateTokenReturns200(Role role) throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(sampleSpotGroups());

		mockMvc.perform(spotsRequest(bearer(accessTokenFixture.user(GUARDIAN_USER_ID + 10, FAMILY_ID, role))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.academies.length()").value(1));
	}

	@ParameterizedTest
	@ValueSource(ints = {50, 3000})
	@DisplayName("GET /v1/families/me/pickup-spots는 경계값 radiusM 50 · 3000을 near와 함께 보내면 200을 돌려준다")
	void listSpotsWithBoundaryRadiusReturns200(int radiusM) throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(new SpotGroups(List.of()));

		mockMvc.perform(spotsRequest(guardianBearer())
						.queryParam("near", NEAR)
						.queryParam("radiusM", String.valueOf(radiusM)))
				.andExpect(status().isOk());

		assertThat(capturedQuery().radiusM()).isEqualTo(radiusM);
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots는 차량 토큰으로도 200과 academies[]를 돌려준다")
	void listSpotsWithVehicleTokenReturns200() throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(sampleSpotGroups());

		mockMvc.perform(spotsRequest(vehicleBearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.academies[0].academyId").value(ACADEMY_ID))
				.andExpect(jsonPath("$.academies[0].spots[0].spotId").value(21));
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots를 차량 토큰으로 부르면 차량 토큰의 familyId를 ListSpotsQuery로 넘긴다")
	void listSpotsWithVehicleTokenPassesVehicleFamilyIdToUseCase() throws Exception {
		given(listPickupSpotsUseCase.listSpots(any())).willReturn(new SpotGroups(List.of()));

		mockMvc.perform(spotsRequest(vehicleBearer()).queryParam("academyId", String.valueOf(ACADEMY_ID)))
				.andExpect(status().isOk());

		assertThat(capturedQuery()).isEqualTo(new ListSpotsQuery(VEHICLE_FAMILY_ID, ACADEMY_ID, null, null));
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots에 near만 보내고 radiusM을 빼면 422 VALIDATION_FAILED와 fields의 radiusM을 돌려주고 유스케이스를 부르지 않는다")
	void listSpotsWithNearWithoutRadiusReturns422ValidationFailed() throws Exception {
		mockMvc.perform(spotsRequest(guardianBearer()).queryParam("near", NEAR))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("radiusM")));

		then(listPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots에 radiusM만 보내고 near를 빼면 422 VALIDATION_FAILED와 fields의 near를 돌려주고 유스케이스를 부르지 않는다")
	void listSpotsWithRadiusWithoutNearReturns422ValidationFailed() throws Exception {
		mockMvc.perform(spotsRequest(guardianBearer()).queryParam("radiusM", "500"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("near")));

		then(listPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@ValueSource(ints = {49, 3001})
	@DisplayName("GET /v1/families/me/pickup-spots에 범위(50~3000) 밖 radiusM(49 · 3001)을 보내면 422 VALIDATION_FAILED와 fields의 radiusM을 돌려주고 유스케이스를 부르지 않는다")
	void listSpotsWithOutOfRangeRadiusReturns422ValidationFailed(int radiusM) throws Exception {
		mockMvc.perform(spotsRequest(guardianBearer())
						.queryParam("near", NEAR)
						.queryParam("radiusM", String.valueOf(radiusM)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("radiusM")));

		then(listPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/families/me/pickup-spots를 TEACHER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void listSpotsWithTeacherTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(spotsRequest(userBearer(GUARDIAN_USER_ID + 20, Role.TEACHER)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(listPickupSpotsUseCase).shouldHaveNoInteractions();
	}

	// ---- SPOT-02 POST /v1/families/me/pickup-spots (#34 · #35 · #36 · #37)

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots는 201과 spotId · isDefault를 돌려준다")
	void addSpotReturns201WithSpotIdAndIsDefault() throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willReturn(new SpotCreated(SPOT_ID, true));

		mockMvc.perform(addSpotRequest(guardianBearer(), validSpotBody()))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"spotId":21,"isDefault":true}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots는 토큰의 familyId · userId(createdBy)와 요청 값을 AddSpotCommand로 바꿔 유스케이스에 넘긴다")
	void addSpotPassesConvertedCommandToUseCase() throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willReturn(new SpotCreated(SPOT_ID, true));

		mockMvc.perform(addSpotRequest(guardianBearer(), validSpotBody()))
				.andExpect(status().isCreated());

		assertThat(capturedAddCommand()).isEqualTo(new AddSpotCommand(
				FAMILY_ID, GUARDIAN_USER_ID, ACADEMY_ID, "정문 앞",
				new LatLng(37.501234, 127.039876),
				new LatLng(37.501301, 127.039901),
				"편의점 앞", true));
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots에 memo · isDefault를 빼고 보내면 201이고 memo는 null, isDefault는 false로 넘긴다")
	void addSpotWithoutMemoAndIsDefaultPassesNullMemoAndFalse() throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willReturn(new SpotCreated(SPOT_ID, true));

		mockMvc.perform(addSpotRequest(guardianBearer(), """
						{"academyId":55,"name":"정문 앞",
						 "stopPoint":{"lat":37.501234,"lng":127.039876},
						 "meetingPoint":{"lat":37.501301,"lng":127.039901}}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.isDefault").value(true));

		AddSpotCommand command = capturedAddCommand();
		assertThat(command.memo()).isNull();
		assertThat(command.isDefault()).isFalse();
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots를 CO_GUARDIAN 토큰으로 부르면 201이고 그 userId를 createdBy로 넘긴다")
	void addSpotWithCoGuardianTokenReturns201AndPassesCreatedBy() throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willReturn(new SpotCreated(SPOT_ID, false));
		long coGuardianUserId = GUARDIAN_USER_ID + 1;

		mockMvc.perform(addSpotRequest(userBearer(coGuardianUserId, Role.CO_GUARDIAN), validSpotBody()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.spotId").value(SPOT_ID))
				.andExpect(jsonPath("$.isDefault").value(false));

		assertThat(capturedAddCommand().createdBy()).isEqualTo(coGuardianUserId);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 50})
	@DisplayName("POST /v1/families/me/pickup-spots에 경계값 1자 · 50자 name을 보내면 201이고 그 값을 그대로 넘긴다")
	void addSpotWithBoundaryNameLengthReturns201(int length) throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willReturn(new SpotCreated(SPOT_ID, true));
		String name = "가".repeat(length);

		mockMvc.perform(addSpotRequest(guardianBearer(), spotBody(quoted(name), quoted("편의점 앞"))))
				.andExpect(status().isCreated());

		assertThat(capturedAddCommand().name()).isEqualTo(name);
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots에 경계값 200자 memo를 보내면 201이고 그 값을 그대로 넘긴다")
	void addSpotWith200CharMemoReturns201() throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willReturn(new SpotCreated(SPOT_ID, true));
		String memo = "m".repeat(200);

		mockMvc.perform(addSpotRequest(guardianBearer(), spotBody(quoted("정문 앞"), quoted(memo))))
				.andExpect(status().isCreated());

		assertThat(capturedAddCommand().memo()).isEqualTo(memo);
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots에 51자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려주고 유스케이스를 부르지 않는다")
	void addSpotWith51CharNameReturns422ValidationFailed() throws Exception {
		mockMvc.perform(addSpotRequest(guardianBearer(), spotBody(quoted("가".repeat(51)), quoted("편의점 앞"))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("name")));

		then(addPickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots에 0자 name을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려주고 유스케이스를 부르지 않는다")
	void addSpotWithEmptyNameReturns422ValidationFailed() throws Exception {
		mockMvc.perform(addSpotRequest(guardianBearer(), spotBody(quoted(""), quoted("편의점 앞"))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("name")));

		then(addPickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots에 201자 memo를 보내면 422 VALIDATION_FAILED와 fields의 memo를 돌려주고 유스케이스를 부르지 않는다")
	void addSpotWith201CharMemoReturns422ValidationFailed() throws Exception {
		mockMvc.perform(addSpotRequest(guardianBearer(), spotBody(quoted("정문 앞"), quoted("m".repeat(201)))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("memo")));

		then(addPickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots에 빈 본문 {}를 보내면 422 VALIDATION_FAILED와 fields의 academyId · name · stopPoint · meetingPoint를 돌려주고 유스케이스를 부르지 않는다")
	void addSpotWithMissingRequiredFieldsReturns422ValidationFailed() throws Exception {
		mockMvc.perform(addSpotRequest(guardianBearer(), "{}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field",
						hasItems("academyId", "name", "stopPoint", "meetingPoint")));

		then(addPickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다")
	void addSpotTooFarFromAcademyReturns422SpotTooFar() throws Exception {
		given(addPickupSpotUseCase.addSpot(any())).willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_TOO_FAR", "학원에서 500m 안이어야 합니다"));

		mockMvc.perform(addSpotRequest(guardianBearer(), validSpotBody()))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_TOO_FAR"));
	}

	@Test
	@DisplayName("POST /v1/families/me/pickup-spots를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void addSpotWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(addSpotRequest(userBearer(GUARDIAN_USER_ID + 10, Role.DELEGATE), validSpotBody()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(addPickupSpotUseCase).shouldHaveNoInteractions();
	}

	// ---- SPOT-03 PATCH /v1/pickup-spots/{spotId} (#38 · #39)

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}는 200과 spotId · name · stopPoint · meetingPoint · memo · isDefault를 돌려준다")
	void updateSpotReturns200WithSpotView() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willReturn(sampleSpotView());

		mockMvc.perform(updateSpotRequest(guardianBearer(), fullUpdateBody()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"spotId":21,"name":"후문 앞",
						 "stopPoint":{"lat":37.501111,"lng":127.039222},
						 "meetingPoint":{"lat":37.501333,"lng":127.039444},
						 "memo":"카페 앞","isDefault":true}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}는 Idempotency-Key 없이 토큰의 familyId · 경로의 spotId · 보낸 값을 UpdateSpotCommand로 바꿔 넘긴다")
	void updateSpotPassesConvertedCommandToUseCase() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willReturn(sampleSpotView());

		mockMvc.perform(updateSpotRequest(guardianBearer(), fullUpdateBody()))
				.andExpect(status().isOk());

		assertThat(capturedUpdateCommand()).isEqualTo(new UpdateSpotCommand(
				FAMILY_ID, SPOT_ID, "후문 앞",
				new LatLng(37.501111, 127.039222),
				new LatLng(37.501333, 127.039444),
				"카페 앞", true));
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}에 name만 보내면 200이고 나머지 필드는 null(안 보냄)로 넘긴다")
	void updateSpotWithOnlyNamePassesNullsForOtherFields() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willReturn(sampleSpotView());

		mockMvc.perform(updateSpotRequest(guardianBearer(), """
						{"name":"후문 앞"}
						"""))
				.andExpect(status().isOk());

		assertThat(capturedUpdateCommand()).isEqualTo(new UpdateSpotCommand(
				FAMILY_ID, SPOT_ID, "후문 앞", null, null, null, null));
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}에 빈 본문 {}를 보내면 200이고 모든 필드를 null로 넘긴다")
	void updateSpotWithEmptyBodyPassesAllNulls() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willReturn(sampleSpotView());

		mockMvc.perform(updateSpotRequest(guardianBearer(), "{}"))
				.andExpect(status().isOk());

		assertThat(capturedUpdateCommand()).isEqualTo(new UpdateSpotCommand(
				FAMILY_ID, SPOT_ID, null, null, null, null, null));
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}를 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void updateSpotWithCoGuardianTokenReturns200() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willReturn(sampleSpotView());

		mockMvc.perform(updateSpotRequest(userBearer(GUARDIAN_USER_ID + 1, Role.CO_GUARDIAN), fullUpdateBody()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.spotId").value(SPOT_ID));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 51})
	@DisplayName("PATCH /v1/pickup-spots/{spotId}에 범위(1~50) 밖 name(0자 · 51자)을 보내면 422 VALIDATION_FAILED와 fields의 name을 돌려주고 유스케이스를 부르지 않는다")
	void updateSpotWithOutOfRangeNameReturns422ValidationFailed(int length) throws Exception {
		mockMvc.perform(updateSpotRequest(guardianBearer(), """
						{"name":%s}
						""".formatted(quoted("가".repeat(length)))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("name")));

		then(updatePickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}에 201자 memo를 보내면 422 VALIDATION_FAILED와 fields의 memo를 돌려주고 유스케이스를 부르지 않는다")
	void updateSpotWith201CharMemoReturns422ValidationFailed() throws Exception {
		mockMvc.perform(updateSpotRequest(guardianBearer(), """
						{"memo":%s}
						""".formatted(quoted("m".repeat(201)))))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("memo")));

		then(updatePickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}가 500m를 넘으면 422 SPOT_TOO_FAR를 돌려준다")
	void updateSpotTooFarFromAcademyReturns422SpotTooFar() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_TOO_FAR", "학원에서 500m 안이어야 합니다"));

		mockMvc.perform(updateSpotRequest(guardianBearer(), fullUpdateBody()))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "SPOT_TOO_FAR"));

		then(updatePickupSpotUseCase).should().updateSpot(any());
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}로 기본 장소를 isDefault false로 바꾸려 하면 422 DEFAULT_SPOT_REQUIRED를 돌려준다")
	void updateSpotUnsettingDefaultReturns422DefaultSpotRequired() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "DEFAULT_SPOT_REQUIRED", "먼저 다른 장소를 기본으로 지정해 주세요"));

		mockMvc.perform(updateSpotRequest(guardianBearer(), """
						{"isDefault":false}
						"""))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "DEFAULT_SPOT_REQUIRED"));

		assertThat(capturedUpdateCommand().isDefault()).isFalse();
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}가 다른 가족의 장소면 유스케이스가 던진 404 NOT_FOUND를 돌려준다")
	void updateSpotOfOtherFamilyReturns404NotFound() throws Exception {
		given(updatePickupSpotUseCase.updateSpot(any())).willThrow(BusinessException.of(ErrorCode.NOT_FOUND));

		mockMvc.perform(updateSpotRequest(guardianBearer(), fullUpdateBody()))
				.andExpect(commonError(HttpStatus.NOT_FOUND, "NOT_FOUND"));

		then(updatePickupSpotUseCase).should().updateSpot(any());
	}

	@Test
	@DisplayName("PATCH /v1/pickup-spots/{spotId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void updateSpotWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(updateSpotRequest(userBearer(GUARDIAN_USER_ID + 10, Role.DELEGATE), fullUpdateBody()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(updatePickupSpotUseCase).shouldHaveNoInteractions();
	}

	// ---- SPOT-03 DELETE /v1/pickup-spots/{spotId} (#40 · #41)

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}는 204와 빈 본문을 돌려준다")
	void deleteSpotReturns204() throws Exception {
		mockMvc.perform(deleteSpotRequest(guardianBearer()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}는 토큰의 familyId와 경로의 spotId를 DeleteSpotCommand로 넘긴다")
	void deleteSpotPassesConvertedCommandToUseCase() throws Exception {
		mockMvc.perform(deleteSpotRequest(guardianBearer()))
				.andExpect(status().isNoContent());

		then(deletePickupSpotUseCase).should().deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID));
	}

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}를 CO_GUARDIAN 토큰으로 부르면 204를 돌려준다")
	void deleteSpotWithCoGuardianTokenReturns204() throws Exception {
		mockMvc.perform(deleteSpotRequest(userBearer(GUARDIAN_USER_ID + 1, Role.CO_GUARDIAN)))
				.andExpect(status().isNoContent());

		then(deletePickupSpotUseCase).should().deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID));
	}

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}가 진행 중 세션의 장소면 409 SPOT_IN_USE를 돌려준다")
	void deleteSpotInUseReturns409SpotInUse() throws Exception {
		willThrow(BusinessException.of(HttpStatus.CONFLICT, "SPOT_IN_USE", "진행 중인 픽업에서 쓰는 장소입니다"))
				.given(deletePickupSpotUseCase).deleteSpot(any());

		mockMvc.perform(deleteSpotRequest(guardianBearer()))
				.andExpect(commonError(HttpStatus.CONFLICT, "SPOT_IN_USE"));

		then(deletePickupSpotUseCase).should().deleteSpot(any());
	}

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}가 다른 장소가 남은 기본 장소면 422 DEFAULT_SPOT_REQUIRED를 돌려준다")
	void deleteDefaultSpotWithOtherSpotsReturns422DefaultSpotRequired() throws Exception {
		willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "DEFAULT_SPOT_REQUIRED", "먼저 다른 장소를 기본으로 지정해 주세요"))
				.given(deletePickupSpotUseCase).deleteSpot(any());

		mockMvc.perform(deleteSpotRequest(guardianBearer()))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "DEFAULT_SPOT_REQUIRED"));

		then(deletePickupSpotUseCase).should().deleteSpot(new DeleteSpotCommand(FAMILY_ID, SPOT_ID));
	}

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}에 Idempotency-Key가 없으면 422 VALIDATION_FAILED와 fields의 Idempotency-Key를 돌려주고 유스케이스를 부르지 않는다")
	void deleteSpotWithoutIdempotencyKeyReturns422ValidationFailed() throws Exception {
		mockMvc.perform(delete(SPOT_PATH, SPOT_ID).header(HttpHeaders.AUTHORIZATION, guardianBearer()))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem(IDEMPOTENCY_KEY_HEADER)));

		then(deletePickupSpotUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("DELETE /v1/pickup-spots/{spotId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void deleteSpotWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(deleteSpotRequest(userBearer(GUARDIAN_USER_ID + 10, Role.DELEGATE)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(deletePickupSpotUseCase).shouldHaveNoInteractions();
	}

	private UpdateSpotCommand capturedUpdateCommand() {
		ArgumentCaptor<UpdateSpotCommand> captor = ArgumentCaptor.forClass(UpdateSpotCommand.class);
		then(updatePickupSpotUseCase).should().updateSpot(captor.capture());
		return captor.getValue();
	}

	private MockHttpServletRequestBuilder updateSpotRequest(String bearer, String body) {
		return patch(SPOT_PATH, SPOT_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder deleteSpotRequest(String bearer) {
		return delete(SPOT_PATH, SPOT_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString());
	}

	private static String fullUpdateBody() {
		return """
				{"name":"후문 앞",
				 "stopPoint":{"lat":37.501111,"lng":127.039222},
				 "meetingPoint":{"lat":37.501333,"lng":127.039444},
				 "memo":"카페 앞","isDefault":true}
				""";
	}

	private static SpotView sampleSpotView() {
		return new SpotView(SPOT_ID, "후문 앞",
				new LatLng(37.501111, 127.039222),
				new LatLng(37.501333, 127.039444),
				"카페 앞", true);
	}

	private AddSpotCommand capturedAddCommand() {
		ArgumentCaptor<AddSpotCommand> captor = ArgumentCaptor.forClass(AddSpotCommand.class);
		then(addPickupSpotUseCase).should().addSpot(captor.capture());
		return captor.getValue();
	}

	private MockHttpServletRequestBuilder addSpotRequest(String bearer, String body) {
		return post(SPOTS_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private static String validSpotBody() {
		return spotBody(quoted("정문 앞"), quoted("편의점 앞"));
	}

	private static String spotBody(String nameJson, String memoJson) {
		return """
				{"academyId":55,"name":%s,
				 "stopPoint":{"lat":37.501234,"lng":127.039876},
				 "meetingPoint":{"lat":37.501301,"lng":127.039901},
				 "memo":%s,"isDefault":true}
				""".formatted(nameJson, memoJson);
	}

	private static String quoted(String value) {
		return "\"" + value + "\"";
	}

	private ListSpotsQuery capturedQuery() {
		ArgumentCaptor<ListSpotsQuery> captor = ArgumentCaptor.forClass(ListSpotsQuery.class);
		then(listPickupSpotsUseCase).should().listSpots(captor.capture());
		return captor.getValue();
	}

	private static SpotGroups sampleSpotGroups() {
		return new SpotGroups(List.of(new AcademySpots(ACADEMY_ID, "해솔영어학원", List.of(new SpotItem(
				21L, "정문 앞",
				new LatLng(37.501234, 127.039876),
				new LatLng(37.501301, 127.039901),
				"편의점 앞", true, null)))));
	}

	private MockHttpServletRequestBuilder spotsRequest(String bearer) {
		return get(SPOTS_PATH).header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private String guardianBearer() {
		return userBearer(GUARDIAN_USER_ID, Role.GUARDIAN);
	}

	private String userBearer(long userId, Role role) {
		return bearer(accessTokenFixture.user(userId, FAMILY_ID, role));
	}

	private String vehicleBearer() {
		return bearer(accessTokenFixture.vehicle(VEHICLE_ID, VEHICLE_FAMILY_ID));
	}

}
