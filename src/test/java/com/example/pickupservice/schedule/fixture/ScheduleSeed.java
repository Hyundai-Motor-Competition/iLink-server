package com.example.pickupservice.schedule.fixture;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.schedule.adapter.out.persistence.AcademyEntity;
import com.example.pickupservice.schedule.adapter.out.persistence.PickupScheduleEntity;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;

@TestComponent
@RequiredArgsConstructor
public class ScheduleSeed {

	public static final String ACADEMY_EXTERNAL_PLACE_ID = "kakao:12345";
	public static final String ACADEMY_NAME = "해솔영어학원";
	public static final String ACADEMY_ADDRESS = "서울 강남구 테헤란로 152";
	public static final double ACADEMY_LAT = 37.501234;
	public static final double ACADEMY_LNG = 127.039876;
	public static final double ENTRANCE_LAT = 37.501150;
	public static final double ENTRANCE_LNG = 127.039790;
	public static final String ACADEMY_PHONE = "02-555-1234";
	public static final String WEEKDAYS = "MON,WED";
	public static final LocalTime CLASS_END_TIME = LocalTime.of(18, 0);
	public static final short TARGET_PICKUP = 5;

	private final UserSeed userSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	public Result seed() {
		return seedAcademy(GeoPoints.toPoint(ENTRANCE_LAT, ENTRANCE_LNG));
	}

	public Result seedWithoutEntrance() {
		return seedAcademy(null);
	}

	private Result seedAcademy(Point entrance) {
		UserSeed.Result user = userSeed.seed();
		return transactionTemplate.execute(status -> {
			AcademyEntity academy = persist(new AcademyEntity(null, user.familyId(), ACADEMY_EXTERNAL_PLACE_ID, ACADEMY_NAME, ACADEMY_ADDRESS,
					GeoPoints.toPoint(ACADEMY_LAT, ACADEMY_LNG), entrance,
					ACADEMY_PHONE));
			PickupScheduleEntity schedule = persist(new PickupScheduleEntity(null, user.minjunId(),
					academy.getAcademyId(), WEEKDAYS, CLASS_END_TIME, TARGET_PICKUP, ScheduleStatus.READY));
			entityManager.flush();
			return new Result(user, academy.getAcademyId(), schedule.getScheduleId());
		});
	}

	public void clear(Result seeded) {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from PickupScheduleEntity s where s.scheduleId = :scheduleId")
					.setParameter("scheduleId", seeded.scheduleId())
					.executeUpdate();
			entityManager.createQuery("delete from AcademyEntity a where a.academyId = :academyId")
					.setParameter("academyId", seeded.academyId())
					.executeUpdate();
		});
		userSeed.clear(seeded.user());
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	public record Result(UserSeed.Result user, long academyId, long scheduleId) {
	}

}
