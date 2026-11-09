package com.example.pickupservice.pickup.adapter.in.scheduler;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 DB로 「취소 창이 지나고 모두 탄 세션만 DOOR로 완료」를 본다. 스케줄러 메서드를 직접 불러 10초를 기다리지 않는다
@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class AutoCompleteBoardingSchedulerTest extends IntegrationTestSupport {

	private static final String INTERVAL_PROPERTY = "pickup.session.auto-complete-interval";

	private final AutoCompleteBoardingScheduler scheduler;
	private final PickupSeed pickupSeed;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;
	private final Environment environment;

	private PickupSeed.Result seeded;

	@BeforeEach
	void setUp() {
		seeded = pickupSeed.seed();
		jdbcTemplate.update("update pickup_session set vehicle_phase = 'STOPPED' where session_id = ?",
				seeded.sessionId());
	}

	@AfterEach
	void tearDown() {
		for (String table : List.of("session_event", "pickup_call", "pickup_outcome", "session_child")) {
			jdbcTemplate.update("delete from " + table + " where session_id = ?", seeded.sessionId());
		}
		jdbcTemplate.update("delete from pickup_session where session_id = ?", seeded.sessionId());
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("취소 창이 지나고 세션 자녀가 모두 탄 ACTIVE 세션은 스케줄러 실행 뒤 COMPLETED가 되고 PickupOutcome(boardingMethod DOOR · completedAt)이 저장된다")
	void sessionWithExpiredWindowAndAllBoardedIsCompletedWithDoorOutcome() {
		markBoardingDetected(clock.instant().minusSeconds(1));
		markAllChildrenBoarded();

		scheduler.completeExpiredBoardings();

		assertThat(sessionStatus()).isEqualTo("COMPLETED");
		Map<String, Object> outcome = jdbcTemplate.queryForMap(
				"select boarding_method, completed_at from pickup_outcome where session_id = ?", seeded.sessionId());
		assertThat(outcome.get("boarding_method")).isEqualTo("DOOR");
		assertThat(outcome.get("completed_at")).isNotNull();
	}

	@Test
	@DisplayName("취소 창이 아직 지나지 않은 세션은 모두 탔어도 ACTIVE 그대로이고 PickupOutcome이 없다")
	void sessionWithinCancelWindowIsNotCompleted() {
		markBoardingDetected(clock.instant().plusSeconds(60));
		markAllChildrenBoarded();

		scheduler.completeExpiredBoardings();

		assertThat(sessionStatus()).isEqualTo("ACTIVE");
		assertThat(outcomeCount()).isZero();
	}

	@Test
	@DisplayName("취소 창이 지났어도 세션 자녀 중 boardedAt이 비어 있는 자녀가 있으면 ACTIVE 그대로이고 PickupOutcome이 없다")
	void sessionWithUnboardedChildIsNotCompleted() {
		markBoardingDetected(clock.instant().minusSeconds(1));
		jdbcTemplate.update("update session_child set boarded_at = ? where session_child_id = ?",
				Timestamp.from(clock.instant().minusSeconds(11)), seeded.minjunSessionChildId());

		scheduler.completeExpiredBoardings();

		assertThat(sessionStatus()).isEqualTo("ACTIVE");
		assertThat(outcomeCount()).isZero();
	}

	@Test
	@DisplayName("탑승이 인식되지 않은 세션(boardingCancelableUntil 없음)은 스케줄러 실행 뒤에도 ACTIVE 그대로이다")
	void sessionWithoutBoardingDetectedIsNotCompleted() {
		scheduler.completeExpiredBoardings();

		assertThat(sessionStatus()).isEqualTo("ACTIVE");
		assertThat(outcomeCount()).isZero();
	}

	@Test
	@DisplayName("completeExpiredBoardings에는 @Scheduled(fixedDelayString = ${pickup.session.auto-complete-interval})가 붙어 있고 그 설정값은 10초이다")
	void completeExpiredBoardingsIsScheduledEveryTenSecondsFromConfiguration() throws NoSuchMethodException {
		Method method = ClassUtils.getUserClass(scheduler).getDeclaredMethod("completeExpiredBoardings");

		Scheduled scheduled = method.getAnnotation(Scheduled.class);

		assertThat(scheduled).isNotNull();
		assertThat(scheduled.fixedDelayString()).isEqualTo("${" + INTERVAL_PROPERTY + "}");
		assertThat(environment.getProperty(INTERVAL_PROPERTY, Duration.class)).isEqualTo(Duration.ofSeconds(10));
	}

	private void markBoardingDetected(Instant cancelableUntil) {
		jdbcTemplate.update("""
				update pickup_session
				set boarding_detected_at = ?, boarding_cancelable_until = ?, boarding_door = 'FRONT_RIGHT'
				where session_id = ?
				""", Timestamp.from(cancelableUntil.minusSeconds(10)), Timestamp.from(cancelableUntil),
				seeded.sessionId());
	}

	private void markAllChildrenBoarded() {
		jdbcTemplate.update("update session_child set boarded_at = ? where session_id = ?",
				Timestamp.from(clock.instant().minusSeconds(11)), seeded.sessionId());
	}

	private String sessionStatus() {
		return jdbcTemplate.queryForObject("select session_status from pickup_session where session_id = ?",
				String.class, seeded.sessionId());
	}

	private long outcomeCount() {
		Long count = jdbcTemplate.queryForObject("select count(*) from pickup_outcome where session_id = ?",
				Long.class, seeded.sessionId());
		return count == null ? 0 : count;
	}

}
