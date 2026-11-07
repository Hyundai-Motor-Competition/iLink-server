package com.example.pickupservice.schedule.fixture;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class})
class ScheduleSeedTest extends IntegrationTestSupport {

	private static final double COORDINATE_TOLERANCE = 0.000001;

	private final ScheduleSeed scheduleSeed;
	private final JdbcTemplate jdbcTemplate;

	private ScheduleSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			scheduleSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("테스트 시드로 학원 1곳(해솔영어학원, externalPlaceId kakao:12345, 출입구 있음)과 자녀 민준의 월 · 수 18:00 일정 1개를 만든다")
	void seedCreatesAcademyWithEntranceAndMinjunMondayWednesdaySchedule() {
		seeded = scheduleSeed.seed();

		List<Map<String, Object>> schedules = jdbcTemplate.queryForList("""
				select schedule_id, child_user_id, academy_id, weekdays, class_end_time::text as class_end_time,
				       target_pickup, status
				from pickup_schedule
				where child_user_id in (?, ?)
				""", seeded.user().minjunId(), seeded.user().seoyunId());
		assertThat(schedules).singleElement().satisfies(schedule -> {
			assertThat(((Number) schedule.get("schedule_id")).longValue()).isEqualTo(seeded.scheduleId());
			assertThat(((Number) schedule.get("child_user_id")).longValue()).isEqualTo(seeded.user().minjunId());
			assertThat(((Number) schedule.get("academy_id")).longValue()).isEqualTo(seeded.academyId());
			assertThat(schedule.get("weekdays")).isEqualTo("MON,WED");
			assertThat(schedule.get("class_end_time")).isEqualTo("18:00:00");
			assertThat(((Number) schedule.get("target_pickup")).intValue()).isEqualTo(5);
			assertThat(schedule.get("status")).isEqualTo("READY");
		});

		String childName = jdbcTemplate.queryForObject(
				"select name from users where user_id = ? and role = 'CHILD'", String.class, seeded.user().minjunId());
		assertThat(childName).isEqualTo("민준");

		List<Map<String, Object>> academies = jdbcTemplate.queryForList("""
				select academy_id, external_place_id, name, address, phone,
				       st_y(location::geometry) as location_lat, st_x(location::geometry) as location_lng,
				       st_y(entrance::geometry) as entrance_lat, st_x(entrance::geometry) as entrance_lng
				from academy
				where academy_id in (select academy_id from pickup_schedule where child_user_id in (?, ?))
				""", seeded.user().minjunId(), seeded.user().seoyunId());
		assertThat(academies).singleElement().satisfies(academy -> {
			assertThat(((Number) academy.get("academy_id")).longValue()).isEqualTo(seeded.academyId());
			assertThat(academy.get("external_place_id")).isEqualTo(ScheduleSeed.ACADEMY_EXTERNAL_PLACE_ID)
					.isEqualTo("kakao:12345");
			assertThat(academy.get("name")).isEqualTo("해솔영어학원");
			assertThat((String) academy.get("address")).startsWith("서울 강남구");
			assertThat(academy.get("phone")).isEqualTo(ScheduleSeed.ACADEMY_PHONE);
			assertThat((Double) academy.get("location_lat")).isCloseTo(37.501234, within(COORDINATE_TOLERANCE));
			assertThat((Double) academy.get("location_lng")).isCloseTo(127.039876, within(COORDINATE_TOLERANCE));
			assertThat(academy.get("entrance_lat")).as("출입구가 있어야 한다").isNotNull();
			assertThat(academy.get("entrance_lng")).as("출입구가 있어야 한다").isNotNull();
			assertThat((Double) academy.get("entrance_lat"))
					.isCloseTo(ScheduleSeed.ENTRANCE_LAT, within(COORDINATE_TOLERANCE));
			assertThat((Double) academy.get("entrance_lng"))
					.isCloseTo(ScheduleSeed.ENTRANCE_LNG, within(COORDINATE_TOLERANCE));
		});
	}

}
