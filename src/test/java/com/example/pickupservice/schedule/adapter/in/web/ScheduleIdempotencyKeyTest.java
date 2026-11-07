package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.PickupReadiness;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.in.SetupChild;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.application.port.in.VehicleSettingsSummary;
import com.example.pickupservice.schedule.application.port.in.WeeklyPickups;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static com.example.pickupservice.schedule.fixture.JoinCallModeFixture.joinCallModeOf;
import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class ScheduleIdempotencyKeyTest extends ScheduleWebTestSupport {

	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CHILD_USER_ID = 131L;
	private static final long VEHICLE_ID = 77L;
	private static final long SCHEDULE_ID = 801L;
	private static final long ACADEMY_ID = 55L;
	private static final String SCHEDULE_BODY = """
			{"academy":{"externalPlaceId":"kakao:12345",
			            "name":"해솔영어학원",
			            "address":"서울 강남구 테헤란로 1",
			            "location":{"lat":37.501234,"lng":127.039876},
			            "entrance":{"lat":37.5013,"lng":127.0399},
			            "phone":"0212345678"},
			 "weekdays":["MON","WED"],
			 "classEndTime":"18:00",
			 "targetPickup":5}
			""";

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	@Test
	@DisplayName("Idempotency-Key는 POST /v1/children/{childId}/schedules · POST /v1/children/{childId}/setup-completion에만 요구된다")
	void idempotencyKeyIsRequiredOnlyForCreateScheduleAndCompleteSetup() {
		given(searchAcademyUseCase.searchAcademies(any())).willReturn(List.of());
		given(updateScheduleUseCase.updateSchedule(any())).willReturn(
				new ScheduleResult(SCHEDULE_ID, ACADEMY_ID, ScheduleStatus.READY));
		given(listSchedulesUseCase.listSchedules(any())).willReturn(List.of());
		given(getSetupSummaryUseCase.getSetupSummary(any())).willReturn(new SetupSummary(
				new SetupChild(CHILD_USER_ID, "민준", DeviceStatus.CONNECTED),
				List.of(), List.of(), joinCallModeOf(SetupSummary.class, "AUTO"), List.of(), 8));
		given(getWeeklyPickupsUseCase.getWeeklyPickups(anyLong(), any())).willReturn(new WeeklyPickups(
				LocalDate.of(2026, 11, 2), List.of(), new PickupReadiness(true, List.of())));
		given(getVehicleSettingsSummaryUseCase.getVehicleSettingsSummary(anyLong())).willReturn(
				new VehicleSettingsSummary("김*호", List.of(), joinCallModeOf(VehicleSettingsSummary.class, "AUTO")));

		assertAll(
				() -> mockMvc.perform(withGuardian(post("/v1/children/{childId}/schedules", CHILD_USER_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content(SCHEDULE_BODY)))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(post("/v1/children/{childId}/setup-completion", CHILD_USER_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
										{"settingsVersion":8}
										""")))
						.andExpect(missingIdempotencyKey()),
				() -> mockMvc.perform(withGuardian(get("/v1/places/academies").queryParam("query", "해솔")))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withGuardian(put("/v1/schedules/{scheduleId}", SCHEDULE_ID)
								.contentType(MediaType.APPLICATION_JSON)
								.content(SCHEDULE_BODY)))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withGuardian(get("/v1/children/{childId}/schedules", CHILD_USER_ID)))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withGuardian(get("/v1/children/{childId}/setup-summary", CHILD_USER_ID)))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withVehicle(get("/v1/vehicles/me/weekly-pickups")))
						.andExpect(status().isOk()),
				() -> mockMvc.perform(withVehicle(get("/v1/vehicles/me/settings-summary")))
						.andExpect(status().isOk()));

		then(createScheduleUseCase).shouldHaveNoInteractions();
		then(completeSetupUseCase).shouldHaveNoInteractions();
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
