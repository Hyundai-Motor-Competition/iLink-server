package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleCommand;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleResult;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Import(UserSeed.class)
class AcademyEntranceRejudgeIntegrationTest extends IntegrationTestSupport {

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	private final CreateScheduleUseCase createScheduleUseCase;
	private final UserSeed userSeed;
	private final JdbcTemplate jdbcTemplate;

	private UserSeed.Result family;
	private UserSeed.Result otherFamily;
	private String placeId;

	@BeforeEach
	void seed() {
		family = userSeed.seed();
		otherFamily = userSeed.seed();
		placeId = "test:" + UUID.randomUUID();
	}

	@AfterEach
	void clear() {
		jdbcTemplate.update("delete from pickup_schedule where child_user_id in (?, ?, ?, ?)", family.minjunId(),
				family.seoyunId(), otherFamily.minjunId(), otherFamily.seoyunId());
		jdbcTemplate.update("delete from academy where family_id in (?, ?)", family.familyId(),
				otherFamily.familyId());
		userSeed.clear(family);
		userSeed.clear(otherFamily);
	}

	@Test
	@DisplayName("출입구 없는 학원으로 DRAFT인 민준의 일정이 있을 때 서윤이 같은 학원을 출입구와 함께 등록하면 민준의 일정 행이 READY로 바뀌고 같은 externalPlaceId를 쓰는 다른 가족의 DRAFT 일정은 그대로다")
	void registeringEntranceRejudgesDraftSchedulesOfSameFamilyAcademyToReady() {
		ScheduleResult otherFamilyDraft = createScheduleUseCase.createSchedule(
				command(otherFamily.familyId(), otherFamily.minjunId(), null, DayOfWeek.MONDAY));
		ScheduleResult minjunDraft = createScheduleUseCase.createSchedule(
				command(family.familyId(), family.minjunId(), null, DayOfWeek.MONDAY));
		assertThat(minjunDraft.status()).as("출입구 없이 등록한 민준의 일정").isEqualTo(ScheduleStatus.DRAFT);

		ScheduleResult seoyunReady = createScheduleUseCase.createSchedule(
				command(family.familyId(), family.seoyunId(), ENTRANCE, DayOfWeek.TUESDAY));

		assertThat(seoyunReady.academyId()).as("서윤이 쓴 학원 (민준과 같은 학원)").isEqualTo(minjunDraft.academyId());
		assertThat(seoyunReady.status()).as("서윤의 일정").isEqualTo(ScheduleStatus.READY);
		assertThat(statusOf(minjunDraft.scheduleId())).as("민준의 일정 행 status").isEqualTo("READY");
		assertThat(statusOf(otherFamilyDraft.scheduleId())).as("다른 가족 일정 행 status").isEqualTo("DRAFT");
	}

	private CreateScheduleCommand command(long familyId, long childId, LatLng entrance, DayOfWeek weekday) {
		AcademyInput academy = new AcademyInput(placeId, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, entrance,
				"02-555-1234");
		return new CreateScheduleCommand(familyId, childId, academy, Set.of(weekday), LocalTime.of(18, 0), 5);
	}

	private String statusOf(long scheduleId) {
		return jdbcTemplate.queryForObject("select status from pickup_schedule where schedule_id = ?", String.class,
				scheduleId);
	}

}
