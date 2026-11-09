package com.example.pickupservice.pickup.fixture;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.adapter.out.persistence.PickupSessionEntity;
import com.example.pickupservice.pickup.adapter.out.persistence.SessionChildEntity;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.adapter.out.persistence.PickupScheduleEntity;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;

@TestComponent
@RequiredArgsConstructor
public class PickupSeed {

	public static final String SPOT_NAME = PlaceSeed.DEFAULT_SPOT_NAME;
	public static final double STOP_LAT = PlaceSeed.DEFAULT_STOP_LAT;
	public static final double STOP_LNG = PlaceSeed.DEFAULT_STOP_LNG;
	public static final double MEETING_LAT = PlaceSeed.DEFAULT_MEETING_LAT;
	public static final double MEETING_LNG = PlaceSeed.DEFAULT_MEETING_LNG;
	public static final int STATE_VERSION = 0;

	private final PlaceSeed placeSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public Result seed() {
		PlaceSeed.Result place = placeSeed.seed();
		ScheduleSeed.Result schedule = place.schedule();
		UserSeed.Result user = schedule.user();
		LocalDate today = TimeUtils.todayKst(clock);
		return transactionTemplate.execute(status -> {
			PickupScheduleEntity seoyunSchedule = persist(new PickupScheduleEntity(null, user.seoyunId(),
					schedule.academyId(), ScheduleSeed.WEEKDAYS, ScheduleSeed.CLASS_END_TIME,
					ScheduleSeed.TARGET_PICKUP, ScheduleStatus.READY));
			PickupSessionEntity session = persist(new PickupSessionEntity(null, user.vehicleId(), user.familyId(),
					place.defaultSpotId(), SPOT_NAME, GeoPoints.toPoint(STOP_LAT, STOP_LNG),
					GeoPoints.toPoint(MEETING_LAT, MEETING_LNG), today, SessionStatus.ACTIVE, VehiclePhase.ROUTING,
					SpotStatus.SELECTED, STATE_VERSION, 0, 0, null, false, null, null, null, null, null, null, null,
					null, null, null, null));
			long sessionId = session.getSessionId();
			SessionChildEntity minjun = persist(waitingChild(sessionId, user.minjunId(), schedule.scheduleId()));
			SessionChildEntity seoyun = persist(waitingChild(sessionId, user.seoyunId(),
					seoyunSchedule.getScheduleId()));
			entityManager.flush();
			return new Result(place, seoyunSchedule.getScheduleId(), sessionId, today,
					minjun.getSessionChildId(), seoyun.getSessionChildId());
		});
	}

	public void clear(Result seeded) {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from SessionChildEntity c where c.sessionId = :sessionId")
					.setParameter("sessionId", seeded.sessionId())
					.executeUpdate();
			entityManager.createQuery("delete from PickupSessionEntity s where s.sessionId = :sessionId")
					.setParameter("sessionId", seeded.sessionId())
					.executeUpdate();
			entityManager.createQuery("delete from PickupScheduleEntity s where s.scheduleId = :scheduleId")
					.setParameter("scheduleId", seeded.seoyunScheduleId())
					.executeUpdate();
		});
		placeSeed.clear(seeded.place());
	}

	private static SessionChildEntity waitingChild(long sessionId, long childUserId, long scheduleId) {
		return new SessionChildEntity(null, sessionId, childUserId, scheduleId, ChildPhase.WAITING, (short) 0, null,
				null, null, null, null, null);
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	public record Result(PlaceSeed.Result place, long seoyunScheduleId, long sessionId, LocalDate date,
			long minjunSessionChildId, long seoyunSessionChildId) {

		public long familyId() {
			return place.schedule().user().familyId();
		}

		public long guardianId() {
			return place.schedule().user().guardianId();
		}

		public long minjunId() {
			return place.schedule().user().minjunId();
		}

		public long seoyunId() {
			return place.schedule().user().seoyunId();
		}

		public long vehicleId() {
			return place.schedule().user().vehicleId();
		}

		public String vin() {
			return place.schedule().user().vin();
		}

		public long academyId() {
			return place.schedule().academyId();
		}

		public long minjunScheduleId() {
			return place.schedule().scheduleId();
		}

		public long spotId() {
			return place.defaultSpotId();
		}

		public long otherSpotId() {
			return place.otherSpotId();
		}

	}

}
