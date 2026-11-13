package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.TodayPickup;
import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class FamilyScheduleControllerTest extends ScheduleWebTestSupport {

	private static final String TODAY_PICKUPS_PATH = "/v1/families/me/today-pickups";
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_ID = 77L;
	private static final long GUARDIAN_USER_ID = 101L;
	private static final long CO_GUARDIAN_USER_ID = 102L;
	private static final long CHILD_USER_ID = 131L;

	private final MockMvc mockMvc;
	private final AccessTokenFixture accessTokenFixture;

	// ---- SET-12 GET /v1/families/me/today-pickups (#81)

	@Test
	@DisplayName("GET /v1/families/me/today-pickups는 GUARDIAN 토큰으로 200과 items[](scheduleId · childName · academyName · targetAt(KST +09:00) · status)를 유스케이스 순서대로 돌려준다")
	void getTodayPickupsWithGuardianTokenReturns200WithItemsInUseCaseOrder() throws Exception {
		given(getTodayPickupsUseCase.getTodayPickups(anyLong())).willReturn(List.of(
				new TodayPickup(802L, "서연", "다솜피아노", kst("2026-11-16T16:10:00+09:00"),
						TodayPickupStatus.COMPLETED),
				new TodayPickup(801L, "민준", "해솔영어학원", kst("2026-11-16T18:05:00+09:00"),
						TodayPickupStatus.IN_PROGRESS),
				new TodayPickup(803L, "민준", "한빛수학", kst("2026-11-16T19:00:00+09:00"),
						TodayPickupStatus.SCHEDULED)));

		mockMvc.perform(todayPickupsRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[
						  {"scheduleId":802,"childName":"서연","academyName":"다솜피아노","targetAt":"2026-11-16T16:10:00+09:00","status":"COMPLETED"},
						  {"scheduleId":801,"childName":"민준","academyName":"해솔영어학원","targetAt":"2026-11-16T18:05:00+09:00","status":"IN_PROGRESS"},
						  {"scheduleId":803,"childName":"민준","academyName":"한빛수학","targetAt":"2026-11-16T19:00:00+09:00","status":"SCHEDULED"}
						]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/families/me/today-pickups는 GUARDIAN 토큰의 familyId를 유스케이스에 넘긴다")
	void getTodayPickupsPassesFamilyIdFromTokenToUseCase() throws Exception {
		given(getTodayPickupsUseCase.getTodayPickups(anyLong())).willReturn(List.of());

		mockMvc.perform(todayPickupsRequest(guardianBearer()))
				.andExpect(status().isOk());

		then(getTodayPickupsUseCase).should().getTodayPickups(FAMILY_ID);
	}

	@Test
	@DisplayName("GET /v1/families/me/today-pickups는 CO_GUARDIAN 토큰으로도 200과 items를 돌려준다")
	void getTodayPickupsWithCoGuardianTokenReturns200() throws Exception {
		given(getTodayPickupsUseCase.getTodayPickups(anyLong())).willReturn(List.of(
				new TodayPickup(801L, "민준", "해솔영어학원", kst("2026-11-16T18:05:00+09:00"),
						TodayPickupStatus.SCHEDULED)));

		mockMvc.perform(todayPickupsRequest(
						bearer(accessTokenFixture.user(CO_GUARDIAN_USER_ID, FAMILY_ID, Role.CO_GUARDIAN))))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[{"scheduleId":801,"childName":"민준","academyName":"해솔영어학원","targetAt":"2026-11-16T18:05:00+09:00","status":"SCHEDULED"}]}
						""", JsonCompareMode.STRICT));

		then(getTodayPickupsUseCase).should().getTodayPickups(FAMILY_ID);
	}

	@Test
	@DisplayName("GET /v1/families/me/today-pickups는 오늘 픽업이 없으면 200과 빈 items를 돌려준다")
	void getTodayPickupsReturns200WithEmptyItemsWhenNoPickupToday() throws Exception {
		given(getTodayPickupsUseCase.getTodayPickups(anyLong())).willReturn(List.of());

		mockMvc.perform(todayPickupsRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/families/me/today-pickups를 CHILD 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void getTodayPickupsWithChildTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(todayPickupsRequest(bearer(accessTokenFixture.user(CHILD_USER_ID, FAMILY_ID, Role.CHILD))))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getTodayPickupsUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/families/me/today-pickups를 차량 토큰으로 부르면 403 FORBIDDEN을 돌려주고 유스케이스를 부르지 않는다")
	void getTodayPickupsWithVehicleTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(todayPickupsRequest(bearer(accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID))))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(getTodayPickupsUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder todayPickupsRequest(String bearer) {
		return get(TODAY_PICKUPS_PATH).header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private String guardianBearer() {
		return bearer(accessTokenFixture.user(GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN));
	}

	private static Instant kst(String offsetDateTime) {
		return OffsetDateTime.parse(offsetDateTime).toInstant();
	}

}
