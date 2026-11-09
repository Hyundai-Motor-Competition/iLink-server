package com.example.pickupservice.pickup.fixture;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class PickupSeedTest extends IntegrationTestSupport {

	private static final double COORDINATE_TOLERANCE = 0.000001;
	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	private final PickupSeed pickupSeed;
	private final JdbcTemplate jdbcTemplate;

	private PickupSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			pickupSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("테스트 시드로 차량 1대 · 자녀 2명(민준 · 서윤) · 기본 장소 정문 앞이 있는 오늘(한국) ACTIVE 세션 1개를 만든다")
	void seedCreatesTodayActiveSessionWithOneVehicleTwoChildrenAndDefaultSpot() {
		seeded = pickupSeed.seed();
		LocalDate todayKorea = LocalDate.now(KOREA);

		List<Map<String, Object>> vehicles = jdbcTemplate.queryForList(
				"select vehicle_id from vehicle where family_id = ?", seeded.familyId());
		assertThat(vehicles).singleElement()
				.satisfies(vehicle -> assertThat(((Number) vehicle.get("vehicle_id")).longValue())
						.isEqualTo(seeded.vehicleId()));

		List<Map<String, Object>> sessions = jdbcTemplate.queryForList("""
				select s.session_id, s.family_id, s.spot_id, s.spot_name, s."date"::text as session_date,
				       s.session_status, s.vehicle_phase, s.spot_status,
				       s.state_version, s.spot_version, s.route_version, s.route_spot_version, s.call_held,
				       st_y(s.stop_point::geometry) as stop_lat, st_x(s.stop_point::geometry) as stop_lng,
				       st_y(s.meeting_point::geometry) as meeting_lat, st_x(s.meeting_point::geometry) as meeting_lng
				from pickup_session s
				where s.vehicle_id = ?
				""", seeded.vehicleId());
		assertThat(sessions).singleElement().satisfies(session -> {
			assertThat(((Number) session.get("session_id")).longValue()).isEqualTo(seeded.sessionId());
			assertThat(((Number) session.get("family_id")).longValue())
					.as("세션 family_id는 차량의 가족이어야 한다")
					.isEqualTo(seeded.familyId());
			assertThat(((Number) session.get("spot_id")).longValue()).isEqualTo(seeded.spotId());
			assertThat(session.get("spot_name")).isEqualTo("정문 앞");
			assertThat(session.get("session_date")).isEqualTo(todayKorea.toString());
			assertThat(session.get("session_status")).isEqualTo("ACTIVE");
			assertThat(session.get("vehicle_phase")).isEqualTo("ROUTING");
			assertThat(session.get("spot_status")).isEqualTo("SELECTED");
			assertThat(((Number) session.get("state_version")).intValue()).isZero();
			assertThat(((Number) session.get("spot_version")).intValue()).isZero();
			assertThat(((Number) session.get("route_version")).intValue()).isZero();
			assertThat(session.get("route_spot_version")).isNull();
			assertThat(session.get("call_held")).isEqualTo(false);
			assertThat((Double) session.get("stop_lat")).isCloseTo(37.501234, within(COORDINATE_TOLERANCE));
			assertThat((Double) session.get("stop_lng")).isCloseTo(127.039876, within(COORDINATE_TOLERANCE));
			assertThat((Double) session.get("meeting_lat")).isCloseTo(37.501301, within(COORDINATE_TOLERANCE));
			assertThat((Double) session.get("meeting_lng")).isCloseTo(127.039901, within(COORDINATE_TOLERANCE));
		});
		assertThat(seeded.date()).isEqualTo(todayKorea);

		Map<String, Object> spot = jdbcTemplate.queryForMap(
				"select family_id, academy_id, name, is_default from pickup_spot where spot_id = ?", seeded.spotId());
		assertThat(((Number) spot.get("family_id")).longValue()).isEqualTo(seeded.familyId());
		assertThat(((Number) spot.get("academy_id")).longValue()).isEqualTo(seeded.academyId());
		assertThat(spot.get("name")).isEqualTo(PlaceSeed.DEFAULT_SPOT_NAME);
		assertThat(spot.get("is_default")).as("세션 장소는 기본 장소여야 한다").isEqualTo(true);

		List<Map<String, Object>> children = jdbcTemplate.queryForList("""
				select sc.session_child_id, sc.child_user_id, sc.schedule_id, sc.child_phase, sc.extra_prep_minutes,
				       sc.call_not_before, sc.acknowledged_at, sc.boarded_at,
				       u.name, u.role, u.family_id,
				       ps.child_user_id as schedule_child_user_id, ps.academy_id, ps.weekdays,
				       ps.class_end_time::text as class_end_time, ps.status as schedule_status
				from session_child sc
				join users u on u.user_id = sc.child_user_id
				join pickup_schedule ps on ps.schedule_id = sc.schedule_id
				where sc.session_id = ?
				order by sc.session_child_id
				""", seeded.sessionId());
		assertThat(children)
				.extracting(row -> ((Number) row.get("session_child_id")).longValue(),
						row -> ((Number) row.get("child_user_id")).longValue(),
						row -> ((Number) row.get("schedule_id")).longValue(),
						row -> row.get("name"))
				.containsExactly(
						tuple(seeded.minjunSessionChildId(), seeded.minjunId(), seeded.minjunScheduleId(), "민준"),
						tuple(seeded.seoyunSessionChildId(), seeded.seoyunId(), seeded.seoyunScheduleId(), "서윤"));
		assertThat(children).allSatisfy(child -> {
			assertThat(child.get("child_phase")).isEqualTo("WAITING");
			assertThat(((Number) child.get("extra_prep_minutes")).intValue()).isZero();
			assertThat(child.get("call_not_before")).isNull();
			assertThat(child.get("acknowledged_at")).isNull();
			assertThat(child.get("boarded_at")).isNull();
			assertThat(child.get("role")).isEqualTo("CHILD");
			assertThat(((Number) child.get("family_id")).longValue()).isEqualTo(seeded.familyId());
			assertThat(((Number) child.get("schedule_child_user_id")).longValue())
					.as("일정은 그 자녀의 것이어야 한다")
					.isEqualTo(((Number) child.get("child_user_id")).longValue());
			assertThat(((Number) child.get("academy_id")).longValue()).isEqualTo(seeded.academyId());
			assertThat(child.get("weekdays")).isEqualTo(ScheduleSeed.WEEKDAYS);
			assertThat(child.get("class_end_time")).isEqualTo("18:00:00");
			assertThat(child.get("schedule_status")).isEqualTo("READY");
		});
	}

}
