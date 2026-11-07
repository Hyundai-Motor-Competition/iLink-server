package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.DailyPickups;
import com.example.pickupservice.schedule.application.port.in.PickupReadiness;
import com.example.pickupservice.schedule.application.port.in.VehicleChild;
import com.example.pickupservice.schedule.application.port.in.VehicleSettingsSummary;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickup;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickups;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.example.pickupservice.schedule.fixture.JoinCallModeFixture.joinCallModeOf;
import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class VehicleScheduleControllerTest extends ScheduleWebTestSupport {

	private static final String WEEKLY_PICKUPS_PATH = "/v1/vehicles/me/weekly-pickups";
	private static final String SETTINGS_SUMMARY_PATH = "/v1/vehicles/me/settings-summary";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 131L;
	private static final LocalDate WEEK_START = LocalDate.of(2026, 11, 2);

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	// ---- SET-09 GET /v1/vehicles/me/weekly-pickups (#52 · #53)

	@Test
	@DisplayName("GET /v1/vehicles/me/weekly-pickups는 차량 토큰으로 200과 weekStart · days · readiness를 돌려준다")
	void getWeeklyPickupsWithVehicleTokenReturns200WithWeekStartDaysAndReadiness() throws Exception {
		given(getWeeklyPickupsUseCase.getWeeklyPickups(anyLong(), any())).willReturn(sampleWeeklyPickups());

		mockMvc.perform(weeklyPickupsRequest(vehicleBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"weekStart":"2026-11-02",
						 "days":[{"date":"2026-11-05",
						          "pickups":[{"scheduleId":801,"childName":"민준","academyName":"해솔영어학원",
						                      "classEndTime":"18:00","targetPickupTime":"18:05"}]}],
						 "readiness":{"startable":true,"missing":[]}}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/weekly-pickups는 readiness의 startable false와 missing(CHILD_DEVICE · PICKUP_SPOT), 빈 pickups를 그대로 돌려준다")
	void getWeeklyPickupsReturnsNotStartableReadinessAndEmptyPickups() throws Exception {
		given(getWeeklyPickupsUseCase.getWeeklyPickups(anyLong(), any())).willReturn(new WeeklyPickups(
				WEEK_START,
				List.of(new DailyPickups(LocalDate.of(2026, 11, 2), List.of())),
				new PickupReadiness(false, List.of(MissingSetupItem.CHILD_DEVICE, MissingSetupItem.PICKUP_SPOT))));

		mockMvc.perform(weeklyPickupsRequest(vehicleBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"weekStart":"2026-11-02",
						 "days":[{"date":"2026-11-02","pickups":[]}],
						 "readiness":{"startable":false,"missing":["CHILD_DEVICE","PICKUP_SPOT"]}}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/weekly-pickups는 weekStart가 없으면 차량 토큰의 familyId와 null을 유스케이스에 넘긴다")
	void getWeeklyPickupsWithoutWeekStartPassesFamilyIdAndNullToUseCase() throws Exception {
		given(getWeeklyPickupsUseCase.getWeeklyPickups(anyLong(), any())).willReturn(sampleWeeklyPickups());

		mockMvc.perform(weeklyPickupsRequest(vehicleBearer()))
				.andExpect(status().isOk());

		then(getWeeklyPickupsUseCase).should().getWeeklyPickups(FAMILY_ID, null);
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/weekly-pickups는 weekStart(yyyy-MM-dd)를 LocalDate로 바꿔 차량 토큰의 familyId와 함께 유스케이스에 넘긴다")
	void getWeeklyPickupsWithWeekStartPassesFamilyIdAndLocalDateToUseCase() throws Exception {
		given(getWeeklyPickupsUseCase.getWeeklyPickups(anyLong(), any())).willReturn(sampleWeeklyPickups());

		mockMvc.perform(weeklyPickupsRequest(vehicleBearer()).queryParam("weekStart", "2026-11-02"))
				.andExpect(status().isOk());

		then(getWeeklyPickupsUseCase).should().getWeeklyPickups(FAMILY_ID, WEEK_START);
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/weekly-pickups에 날짜 형식이 아닌 weekStart를 보내면 400 INVALID_REQUEST를 돌려주고 유스케이스를 부르지 않는다")
	void getWeeklyPickupsWithMalformedWeekStartReturns400InvalidRequest() throws Exception {
		mockMvc.perform(weeklyPickupsRequest(vehicleBearer()).queryParam("weekStart", "2026-13-01"))
				.andExpect(commonError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST"));

		then(getWeeklyPickupsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/weekly-pickups를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void getWeeklyPickupsWithUserTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(weeklyPickupsRequest(guardianBearer()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getWeeklyPickupsUseCase).shouldHaveNoInteractions();
	}

	// ---- SET-10 GET /v1/vehicles/me/settings-summary (#54)

	@Test
	@DisplayName("GET /v1/vehicles/me/settings-summary는 차량 토큰으로 200과 guardianNameMasked · children · joinCallMode를 돌려준다")
	void getSettingsSummaryWithVehicleTokenReturns200WithMaskedNameChildrenAndJoinCallMode() throws Exception {
		given(getVehicleSettingsSummaryUseCase.getVehicleSettingsSummary(anyLong())).willReturn(
				new VehicleSettingsSummary("김*호", List.of(new VehicleChild(CHILD_USER_ID, "민준")), joinCallModeOf(VehicleSettingsSummary.class, "AUTO")));

		mockMvc.perform(settingsSummaryRequest(vehicleBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"guardianNameMasked":"김*호","children":[{"userId":131,"name":"민준"}],"joinCallMode":"AUTO"}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/settings-summary는 차량 토큰의 familyId를 유스케이스에 넘긴다")
	void getSettingsSummaryPassesFamilyIdToUseCase() throws Exception {
		given(getVehicleSettingsSummaryUseCase.getVehicleSettingsSummary(anyLong())).willReturn(
				new VehicleSettingsSummary("김*호", List.of(), joinCallModeOf(VehicleSettingsSummary.class, "GUARDIAN_CONFIRM")));

		mockMvc.perform(settingsSummaryRequest(vehicleBearer()))
				.andExpect(status().isOk());

		then(getVehicleSettingsSummaryUseCase).should().getVehicleSettingsSummary(FAMILY_ID);
	}

	@Test
	@DisplayName("GET /v1/vehicles/me/settings-summary를 USER 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void getSettingsSummaryWithUserTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(settingsSummaryRequest(guardianBearer()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getVehicleSettingsSummaryUseCase).shouldHaveNoInteractions();
	}

	private static WeeklyPickups sampleWeeklyPickups() {
		return new WeeklyPickups(
				WEEK_START,
				List.of(new DailyPickups(LocalDate.of(2026, 11, 5), List.of(new WeeklyPickup(
						801L, "민준", "해솔영어학원", LocalTime.of(18, 0), LocalTime.of(18, 5))))),
				new PickupReadiness(true, List.of()));
	}

	private MockHttpServletRequestBuilder weeklyPickupsRequest(String bearer) {
		return get(WEEKLY_PICKUPS_PATH).header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private MockHttpServletRequestBuilder settingsSummaryRequest(String bearer) {
		return get(SETTINGS_SUMMARY_PATH).header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private String vehicleBearer() {
		return bearer(accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID));
	}

	private String guardianBearer() {
		return bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN));
	}

}
