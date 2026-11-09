package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.application.port.out.LoadHistoryPort;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.ChildSchedule;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class PickupHistoryPersistenceAdapterTest extends IntegrationTestSupport {

	/** 서비스가 month 2026-11을 바꾼 한국 날짜 기간(양 끝 포함). */
	private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
	private static final LocalDate TO = LocalDate.of(2026, 11, 30);
	private static final LocalDate NOV_10 = LocalDate.of(2026, 11, 10);
	private static final LocalDate NOV_15 = LocalDate.of(2026, 11, 15);
	private static final LocalDate NOV_20 = LocalDate.of(2026, 11, 20);
	private static final LocalDate NOV_21 = LocalDate.of(2026, 11, 21);
	private static final Instant FROM_COMPLETED_AT = Instant.parse("2026-11-01T07:15:00Z");
	private static final Instant NOV_10_COMPLETED_AT = Instant.parse("2026-11-10T07:20:00Z");
	private static final Instant NOV_15_COMPLETED_AT = Instant.parse("2026-11-15T07:25:00Z");
	private static final Instant OCT_31_COMPLETED_AT = Instant.parse("2026-10-31T07:15:00Z");
	private static final Instant DEC_01_COMPLETED_AT = Instant.parse("2026-12-01T07:15:00Z");

	private final LoadHistoryPort loadHistoryPort;
	private final PickupSeed pickupSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	private PickupSeed.Result seeded;

	@BeforeEach
	void seed() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("""
							delete from PickupOutcomeEntity o
							where o.sessionId in (select s.sessionId from PickupSessionEntity s where s.vehicleId = :vehicleId)
							""")
					.setParameter("vehicleId", seeded.vehicleId())
					.executeUpdate();
			entityManager.createQuery("""
							delete from SessionChildEntity c
							where c.sessionId in (select s.sessionId from PickupSessionEntity s where s.vehicleId = :vehicleId)
							""")
					.setParameter("vehicleId", seeded.vehicleId())
					.executeUpdate();
			entityManager.createQuery("delete from PickupSessionEntity s where s.vehicleId = :vehicleId")
					.setParameter("vehicleId", seeded.vehicleId())
					.executeUpdate();
		});
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("loadHistory는 그 자녀가 포함된 세션 중 date(한국 날짜)가 from~to(양 끝 포함) 안인 COMPLETED · CANCELLED 세션만 date 오름차순으로 돌려주고, completedAt은 COMPLETED면 PickupOutcome.completedAt · CANCELLED면 null이며, 기간 밖 · ACTIVE · ON_HOLD · 다른 자녀만 탄 세션은 빠진다")
	void loadHistoryReturnsCompletedAndCancelledSessionsOfThatChildWithinPeriod() {
		long fromDayCompleted = persistSession(FROM, SessionStatus.COMPLETED, minjun());
		persistOutcome(fromDayCompleted, FROM_COMPLETED_AT);
		long seoyunOnlyCompleted = persistSession(NOV_10, SessionStatus.COMPLETED, seoyun());
		persistOutcome(seoyunOnlyCompleted, NOV_10_COMPLETED_AT);
		long bothCompleted = persistSession(NOV_15, SessionStatus.COMPLETED, minjun(), seoyun());
		persistOutcome(bothCompleted, NOV_15_COMPLETED_AT);
		persistSession(NOV_20, SessionStatus.ACTIVE, minjun());
		persistSession(NOV_21, SessionStatus.ON_HOLD, minjun());
		long toDayCancelled = persistSession(TO, SessionStatus.CANCELLED, minjun());
		long dayBeforeFrom = persistSession(FROM.minusDays(1), SessionStatus.COMPLETED, minjun());
		persistOutcome(dayBeforeFrom, OCT_31_COMPLETED_AT);
		long dayAfterTo = persistSession(TO.plusDays(1), SessionStatus.COMPLETED, minjun());
		persistOutcome(dayAfterTo, DEC_01_COMPLETED_AT);

		List<HistoryItem> minjunHistory = loadHistoryPort.loadHistory(seeded.minjunId(), FROM, TO);
		List<HistoryItem> seoyunHistory = loadHistoryPort.loadHistory(seeded.seoyunId(), FROM, TO);

		assertThat(minjunHistory).as("민준 11월 이력").containsExactly(
				new HistoryItem(FROM, fromDayCompleted, SessionStatus.COMPLETED, FROM_COMPLETED_AT),
				new HistoryItem(NOV_15, bothCompleted, SessionStatus.COMPLETED, NOV_15_COMPLETED_AT),
				new HistoryItem(TO, toDayCancelled, SessionStatus.CANCELLED, null));
		assertThat(seoyunHistory).as("서윤 11월 이력: 둘이 같이 탄 세션도 한 번만").containsExactly(
				new HistoryItem(NOV_10, seoyunOnlyCompleted, SessionStatus.COMPLETED, NOV_10_COMPLETED_AT),
				new HistoryItem(NOV_15, bothCompleted, SessionStatus.COMPLETED, NOV_15_COMPLETED_AT));
	}

	private ChildSchedule minjun() {
		return new ChildSchedule(seeded.minjunId(), seeded.minjunScheduleId());
	}

	private ChildSchedule seoyun() {
		return new ChildSchedule(seeded.seoyunId(), seeded.seoyunScheduleId());
	}

	private long persistSession(LocalDate date, SessionStatus status, ChildSchedule... children) {
		return transactionTemplate.execute(tx -> {
			PickupSessionEntity session = new PickupSessionEntity(null, seeded.vehicleId(), seeded.familyId(),
					seeded.spotId(), PickupSeed.SPOT_NAME, GeoPoints.toPoint(PickupSeed.STOP_LAT, PickupSeed.STOP_LNG),
					GeoPoints.toPoint(PickupSeed.MEETING_LAT, PickupSeed.MEETING_LNG), date, status,
					VehiclePhase.ROUTING, SpotStatus.SELECTED, 0, 0, 0, null, false, null, null, null, null, null,
					null, null, null, null, null, null);
			entityManager.persist(session);
			for (ChildSchedule child : children) {
				entityManager.persist(new SessionChildEntity(null, session.getSessionId(), child.childUserId(),
						child.scheduleId(), ChildPhase.WAITING, (short) 0, null, null, null, null, null, null));
			}
			entityManager.flush();
			return session.getSessionId();
		});
	}

	/** loadHistory를 거치지 않고 결과 행을 넣는다. completedAt 말고는 이력과 상관없는 값이다. */
	private void persistOutcome(long sessionId, Instant completedAt) {
		transactionTemplate.executeWithoutResult(tx -> {
			entityManager.persist(new PickupOutcomeEntity(sessionId, completedAt, BoardingMethod.DOOR,
					completedAt.minusSeconds(300), completedAt.minusSeconds(240), null, null, null, null, null));
			entityManager.flush();
		});
	}

}
