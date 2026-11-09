package com.example.pickupservice.place.fixture;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.place.adapter.out.persistence.PickupSpotEntity;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

@TestComponent
@RequiredArgsConstructor
public class PlaceSeed {

	public static final String DEFAULT_SPOT_NAME = "정문 앞";
	public static final String DEFAULT_SPOT_MEMO = "편의점 앞";
	public static final double DEFAULT_STOP_LAT = 37.501234;
	public static final double DEFAULT_STOP_LNG = 127.039876;
	public static final double DEFAULT_MEETING_LAT = 37.501301;
	public static final double DEFAULT_MEETING_LNG = 127.039901;

	public static final String OTHER_SPOT_NAME = "후문 앞";
	public static final double OTHER_STOP_LAT = 37.502100;
	public static final double OTHER_STOP_LNG = 127.040500;
	public static final double OTHER_MEETING_LAT = 37.502150;
	public static final double OTHER_MEETING_LNG = 127.040550;

	private final ScheduleSeed scheduleSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	public Result seed() {
		ScheduleSeed.Result schedule = scheduleSeed.seed();
		long familyId = schedule.user().familyId();
		long academyId = schedule.academyId();
		long guardianId = schedule.user().guardianId();
		return transactionTemplate.execute(status -> {
			PickupSpotEntity defaultSpot = persist(new PickupSpotEntity(null, familyId, academyId, DEFAULT_SPOT_NAME,
					DEFAULT_SPOT_MEMO, GeoPoints.toPoint(DEFAULT_STOP_LAT, DEFAULT_STOP_LNG),
					GeoPoints.toPoint(DEFAULT_MEETING_LAT, DEFAULT_MEETING_LNG), true, guardianId));
			PickupSpotEntity otherSpot = persist(new PickupSpotEntity(null, familyId, academyId, OTHER_SPOT_NAME, null,
					GeoPoints.toPoint(OTHER_STOP_LAT, OTHER_STOP_LNG),
					GeoPoints.toPoint(OTHER_MEETING_LAT, OTHER_MEETING_LNG), false, guardianId));
			entityManager.flush();
			return new Result(schedule, defaultSpot.getSpotId(), otherSpot.getSpotId());
		});
	}

	public void clear(Result seeded) {
		transactionTemplate.executeWithoutResult(status -> entityManager
				.createQuery("delete from PickupSpotEntity s where s.familyId = :familyId")
				.setParameter("familyId", seeded.schedule().user().familyId())
				.executeUpdate());
		scheduleSeed.clear(seeded.schedule());
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	public record Result(ScheduleSeed.Result schedule, long defaultSpotId, long otherSpotId) {
	}

}
