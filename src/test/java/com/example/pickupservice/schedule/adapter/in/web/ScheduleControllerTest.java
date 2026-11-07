package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.AccessTokenFixture;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.ScheduleAcademy;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.application.port.in.ScheduleView;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleCommand;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.example.pickupservice.common.fixture.AccessTokenFixture.bearer;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonError;
import static com.example.pickupservice.common.fixture.ErrorResponseMatchers.commonErrorKeys;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
	private static final String SCHEDULES_PATH = "/v1/children/{childId}/schedules";
	private static final String SCHEDULE_PATH = "/v1/schedules/{scheduleId}";
	private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	private static final long CHILD_USER_ID = 131L;
	private static final long SCHEDULE_ID = 801L;
	private static final long ACADEMY_ID = 55L;
	private static final String FULL_ACADEMY_JSON = """
			{"externalPlaceId":"kakao:12345",
			 "name":"해솔영어학원",
			 "address":"서울 강남구 테헤란로 1",
			 "location":{"lat":37.501234,"lng":127.039876},
			 "entrance":{"lat":37.5013,"lng":127.0399},
			 "phone":"0212345678"}
			""";

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

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules는 201과 scheduleId · academyId · status를 돌려준다")
	void createScheduleReturns201WithScheduleIdAcademyIdAndStatus() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willReturn(readyResult());

		mockMvc.perform(createScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(status().isCreated())
				.andExpect(content().json("""
						{"scheduleId":801,"academyId":55,"status":"READY"}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules는 토큰의 familyId · 경로의 childId · 요청 값을 CreateScheduleCommand로 바꿔 유스케이스에 넘긴다(MON → MONDAY, 18:00 → LocalTime)")
	void createSchedulePassesConvertedCommandToUseCase() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willReturn(readyResult());

		mockMvc.perform(createScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateScheduleCommand> captor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
		then(createScheduleUseCase).should().createSchedule(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new CreateScheduleCommand(
				FAMILY_ID,
				CHILD_USER_ID,
				new AcademyInput("kakao:12345", "해솔영어학원", "서울 강남구 테헤란로 1",
						new LatLng(37.501234, 127.039876), new LatLng(37.5013, 127.0399), "0212345678"),
				EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
				LocalTime.of(18, 0),
				5));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules는 weekdays MON~SUN을 DayOfWeek MONDAY~SUNDAY로 바꿔 넘긴다")
	void createScheduleConvertsAllWeekdaysToDayOfWeek() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willReturn(readyResult());

		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\",\"SAT\",\"SUN\"]", "\"18:00\"", "5")))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateScheduleCommand> captor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
		then(createScheduleUseCase).should().createSchedule(captor.capture());
		assertThat(captor.getValue().weekdays()).containsExactlyInAnyOrder(DayOfWeek.values());
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 entrance · phone을 빼고 보내면 201이고 AcademyInput의 entrance · phone은 null로 넘긴다")
	void createScheduleWithoutOptionalAcademyFieldsPassesNulls() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willReturn(
				new ScheduleResult(SCHEDULE_ID, ACADEMY_ID, ScheduleStatus.DRAFT));

		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody("""
						{"externalPlaceId":"kakao:12345",
						 "name":"해솔영어학원",
						 "address":"서울 강남구 테헤란로 1",
						 "location":{"lat":37.501234,"lng":127.039876}}
						""", "[\"MON\"]", "\"18:00\"", "5")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("DRAFT"));

		ArgumentCaptor<CreateScheduleCommand> captor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
		then(createScheduleUseCase).should().createSchedule(captor.capture());
		assertThat(captor.getValue().academy().entrance()).isNull();
		assertThat(captor.getValue().academy().phone()).isNull();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules를 CO_GUARDIAN 토큰으로 부르면 201을 돌려준다")
	void createScheduleWithCoGuardianTokenReturns201() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willReturn(readyResult());

		mockMvc.perform(createScheduleRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), validScheduleBody()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 targetPickup 61을 보내면 422 VALIDATION_FAILED와 fields의 targetPickup을 돌려주고 유스케이스를 부르지 않는다")
	void createScheduleWithTargetPickup61Returns422ValidationFailed() throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\"]", "\"18:00\"", "61")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("targetPickup")));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 targetPickup -1을 보내면 422 VALIDATION_FAILED와 fields의 targetPickup을 돌려주고 유스케이스를 부르지 않는다")
	void createScheduleWithNegativeTargetPickupReturns422ValidationFailed() throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\"]", "\"18:00\"", "-1")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("targetPickup")));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 60})
	@DisplayName("POST /v1/children/{childId}/schedules에 경계값 targetPickup 0 · 60을 보내면 201이고 그 값을 그대로 넘긴다")
	void createScheduleWithBoundaryTargetPickupReturns201(int targetPickup) throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willReturn(readyResult());

		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\"]", "\"18:00\"", String.valueOf(targetPickup))))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateScheduleCommand> captor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
		then(createScheduleUseCase).should().createSchedule(captor.capture());
		assertThat(captor.getValue().targetPickup()).isEqualTo(targetPickup);
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 빈 weekdays를 보내면 422 VALIDATION_FAILED와 fields의 weekdays를 돌려주고 유스케이스를 부르지 않는다")
	void createScheduleWithEmptyWeekdaysReturns422ValidationFailed() throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[]", "\"18:00\"", "5")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("weekdays")));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 MON~SUN이 아닌 요일(FUNDAY)을 보내면 422 VALIDATION_FAILED와 fields의 weekdays를 돌려주고 유스케이스를 부르지 않는다")
	void createScheduleWithInvalidWeekdayReturns422ValidationFailed() throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\",\"FUNDAY\"]", "\"18:00\"", "5")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem(startsWith("weekdays"))));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@ParameterizedTest
	@ValueSource(strings = {"25:00", "18:60", "1800", "6:00", "18:00:00"})
	@DisplayName("POST /v1/children/{childId}/schedules에 HH:MM이 아닌 classEndTime을 보내면 422 VALIDATION_FAILED와 fields의 classEndTime을 돌려주고 유스케이스를 부르지 않는다")
	void createScheduleWithMalformedClassEndTimeReturns422ValidationFailed(String classEndTime) throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\"]", "\"" + classEndTime + "\"", "5")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("classEndTime")));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 빈 본문 {}를 보내면 422 VALIDATION_FAILED와 fields의 academy · weekdays · classEndTime · targetPickup을 돌려준다")
	void createScheduleWithMissingTopLevelFieldsReturns422ValidationFailed() throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), "{}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field",
						hasItems("academy", "weekdays", "classEndTime", "targetPickup")));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules에 academy의 externalPlaceId · name · address · location이 빠지면 422 VALIDATION_FAILED와 fields의 academy.*를 돌려준다")
	void createScheduleWithMissingAcademyFieldsReturns422ValidationFailed() throws Exception {
		mockMvc.perform(createScheduleRequest(guardianBearer(), scheduleBody(
						"{}", "[\"MON\"]", "\"18:00\"", "5")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItems(
						"academy.externalPlaceId", "academy.name", "academy.address", "academy.location")));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules가 겹치면 409 SCHEDULE_OVERLAP과 overlapScheduleIds를 돌려준다")
	void createScheduleWhenOverlappingReturns409ScheduleOverlapWithIds() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willThrow(BusinessException.conflictWithOverlap(
				HttpStatus.CONFLICT, "SCHEDULE_OVERLAP", "겹치는 일정이 있습니다", List.of(801L, 802L)));

		mockMvc.perform(createScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(commonError(HttpStatus.CONFLICT, "SCHEDULE_OVERLAP"))
				.andExpect(jsonPath("$.error.overlapScheduleIds", contains(801, 802)));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules가 하루 한도를 넘으면 422 DAILY_SCHEDULE_LIMIT을 돌려준다")
	void createScheduleOverDailyLimitReturns422DailyScheduleLimit() throws Exception {
		given(createScheduleUseCase.createSchedule(any())).willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "DAILY_SCHEDULE_LIMIT", "하루 일정은 3개까지입니다"));

		mockMvc.perform(createScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "DAILY_SCHEDULE_LIMIT"));
	}

	@Test
	@DisplayName("POST /v1/children/{childId}/schedules를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void createScheduleWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(createScheduleRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE), validScheduleBody()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(createScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}는 Idempotency-Key 없이 200과 scheduleId · academyId · status를 돌려준다")
	void updateScheduleReturns200WithScheduleIdAcademyIdAndStatus() throws Exception {
		given(updateScheduleUseCase.updateSchedule(any())).willReturn(readyResult());

		mockMvc.perform(updateScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"scheduleId":801,"academyId":55,"status":"READY"}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}는 토큰의 familyId · 경로의 scheduleId · 요청 값을 UpdateScheduleCommand로 바꿔 유스케이스에 넘긴다(MON → MONDAY, 18:00 → LocalTime)")
	void updateSchedulePassesConvertedCommandToUseCase() throws Exception {
		given(updateScheduleUseCase.updateSchedule(any())).willReturn(readyResult());

		mockMvc.perform(updateScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(status().isOk());

		ArgumentCaptor<UpdateScheduleCommand> captor = ArgumentCaptor.forClass(UpdateScheduleCommand.class);
		then(updateScheduleUseCase).should().updateSchedule(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new UpdateScheduleCommand(
				FAMILY_ID,
				SCHEDULE_ID,
				new AcademyInput("kakao:12345", "해솔영어학원", "서울 강남구 테헤란로 1",
						new LatLng(37.501234, 127.039876), new LatLng(37.5013, 127.0399), "0212345678"),
				EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
				LocalTime.of(18, 0),
				5));
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}를 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void updateScheduleWithCoGuardianTokenReturns200() throws Exception {
		given(updateScheduleUseCase.updateSchedule(any())).willReturn(readyResult());

		mockMvc.perform(updateScheduleRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN), validScheduleBody()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduleId").value(SCHEDULE_ID));
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}에 targetPickup 61을 보내면 422 VALIDATION_FAILED와 fields의 targetPickup을 돌려주고 유스케이스를 부르지 않는다")
	void updateScheduleWithTargetPickup61Returns422ValidationFailed() throws Exception {
		mockMvc.perform(updateScheduleRequest(guardianBearer(), scheduleBody(
						FULL_ACADEMY_JSON, "[\"MON\"]", "\"18:00\"", "61")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(commonErrorKeys())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("targetPickup")));

		then(updateScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}가 겹치면 409 SCHEDULE_OVERLAP과 overlapScheduleIds를 돌려준다")
	void updateScheduleWhenOverlappingReturns409ScheduleOverlapWithIds() throws Exception {
		given(updateScheduleUseCase.updateSchedule(any())).willThrow(BusinessException.conflictWithOverlap(
				HttpStatus.CONFLICT, "SCHEDULE_OVERLAP", "겹치는 일정이 있습니다", List.of(802L, 803L)));

		mockMvc.perform(updateScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(commonError(HttpStatus.CONFLICT, "SCHEDULE_OVERLAP"))
				.andExpect(jsonPath("$.error.overlapScheduleIds", contains(802, 803)));
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}가 하루 한도를 넘으면 422 DAILY_SCHEDULE_LIMIT을 돌려준다")
	void updateScheduleOverDailyLimitReturns422DailyScheduleLimit() throws Exception {
		given(updateScheduleUseCase.updateSchedule(any())).willThrow(BusinessException.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "DAILY_SCHEDULE_LIMIT", "하루 일정은 3개까지입니다"));

		mockMvc.perform(updateScheduleRequest(guardianBearer(), validScheduleBody()))
				.andExpect(commonError(HttpStatus.UNPROCESSABLE_CONTENT, "DAILY_SCHEDULE_LIMIT"));
	}

	@Test
	@DisplayName("PUT /v1/schedules/{scheduleId}를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void updateScheduleWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(updateScheduleRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE), validScheduleBody()))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(updateScheduleUseCase).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/schedules는 200과 items[](scheduleId · academy{academyId, name} · weekdays · classEndTime · targetPickup · status)를 돌려준다")
	void listSchedulesReturns200WithItems() throws Exception {
		given(listSchedulesUseCase.listSchedules(any())).willReturn(List.of(new ScheduleView(
				SCHEDULE_ID,
				new ScheduleAcademy(ACADEMY_ID, "해솔영어학원"),
				List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
				LocalTime.of(18, 0),
				5,
				ScheduleStatus.READY)));

		mockMvc.perform(listSchedulesRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[
						  {"scheduleId":801,
						   "academy":{"academyId":55,"name":"해솔영어학원"},
						   "weekdays":["MON","WED"],
						   "classEndTime":"18:00",
						   "targetPickup":5,
						   "status":"READY"}
						]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/schedules는 토큰의 familyId · 경로의 childId를 ChildQuery로 바꿔 유스케이스에 넘긴다")
	void listSchedulesPassesChildQueryToUseCase() throws Exception {
		given(listSchedulesUseCase.listSchedules(any())).willReturn(List.of());

		mockMvc.perform(listSchedulesRequest(guardianBearer()))
				.andExpect(status().isOk());

		then(listSchedulesUseCase).should().listSchedules(new ChildQuery(FAMILY_ID, CHILD_USER_ID));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/schedules는 일정이 없으면 200과 빈 items[]를 돌려준다")
	void listSchedulesWithNoScheduleReturns200WithEmptyItems() throws Exception {
		given(listSchedulesUseCase.listSchedules(any())).willReturn(List.of());

		mockMvc.perform(listSchedulesRequest(guardianBearer()))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"items":[]}
						""", JsonCompareMode.STRICT));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/schedules를 CO_GUARDIAN 토큰으로 부르면 200을 돌려준다")
	void listSchedulesWithCoGuardianTokenReturns200() throws Exception {
		given(listSchedulesUseCase.listSchedules(any())).willReturn(List.of());

		mockMvc.perform(listSchedulesRequest(userBearer(CO_GUARDIAN_USER_ID, Role.CO_GUARDIAN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("GET /v1/children/{childId}/schedules를 DELEGATE 토큰으로 부르면 403 FORBIDDEN을 돌려준다")
	void listSchedulesWithDelegateTokenReturns403Forbidden() throws Exception {
		mockMvc.perform(listSchedulesRequest(userBearer(DELEGATE_USER_ID, Role.DELEGATE)))
				.andExpect(commonError(HttpStatus.FORBIDDEN, "FORBIDDEN"));

		then(listSchedulesUseCase).shouldHaveNoInteractions();
	}

	private MockHttpServletRequestBuilder updateScheduleRequest(String bearer, String body) {
		return put(SCHEDULE_PATH, SCHEDULE_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private MockHttpServletRequestBuilder listSchedulesRequest(String bearer) {
		return get(SCHEDULES_PATH, CHILD_USER_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer);
	}

	private MockHttpServletRequestBuilder createScheduleRequest(String bearer, String body) {
		return post(SCHEDULES_PATH, CHILD_USER_ID)
				.header(HttpHeaders.AUTHORIZATION, bearer)
				.header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
	}

	private static String validScheduleBody() {
		return scheduleBody(FULL_ACADEMY_JSON, "[\"MON\",\"WED\"]", "\"18:00\"", "5");
	}

	private static String scheduleBody(String academyJson, String weekdaysJson, String classEndTimeJson,
			String targetPickupJson) {
		return """
				{"academy":%s,"weekdays":%s,"classEndTime":%s,"targetPickup":%s}
				""".formatted(academyJson, weekdaysJson, classEndTimeJson, targetPickupJson);
	}

	private static ScheduleResult readyResult() {
		return new ScheduleResult(SCHEDULE_ID, ACADEMY_ID, ScheduleStatus.READY);
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
