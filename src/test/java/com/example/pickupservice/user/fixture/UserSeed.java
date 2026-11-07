package com.example.pickupservice.user.fixture;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.adapter.out.persistence.FamilyEntity;
import com.example.pickupservice.user.adapter.out.persistence.UserEntity;
import com.example.pickupservice.user.adapter.out.persistence.VehicleEntity;
import com.example.pickupservice.user.domain.JoinCallMode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

@TestComponent
@RequiredArgsConstructor
public class UserSeed {

	public static final String GUARDIAN_NAME = "김보호";
	public static final String GUARDIAN_PHONE = "+821012345678";
	public static final String MINJUN_NAME = "민준";
	public static final String SEOYUN_NAME = "서윤";
	public static final String VEHICLE_MODEL = "아이오닉 5";
	public static final String PLATE_NUMBER = "12가 3456";
	public static final String PLATE_NUMBER_MASKED = "12가 ****";

	private static final String VIN_PREFIX = "KMHKN81BFNU";
	private static final AtomicInteger VIN_SEQUENCE = new AtomicInteger(123456);

	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	public Result seed() {
		return transactionTemplate.execute(status -> {
			FamilyEntity family = persist(new FamilyEntity(null, JoinCallMode.AUTO, 0));
			long familyId = family.getFamilyId();
			UserEntity guardian = persist(new UserEntity(null, familyId, Role.GUARDIAN, GUARDIAN_NAME, GUARDIAN_PHONE));
			UserEntity minjun = persist(new UserEntity(null, familyId, Role.CHILD, MINJUN_NAME, null));
			UserEntity seoyun = persist(new UserEntity(null, familyId, Role.CHILD, SEOYUN_NAME, null));
			String vin = nextVin();
			VehicleEntity vehicle = persist(new VehicleEntity(null, familyId, vin, VEHICLE_MODEL, PLATE_NUMBER, null));
			entityManager.flush();
			return new Result(familyId, guardian.getUserId(), minjun.getUserId(), seoyun.getUserId(),
					vehicle.getVehicleId(), vin);
		});
	}

	public void clear(Result seeded) {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from UserEntity u where u.familyId = :familyId")
					.setParameter("familyId", seeded.familyId())
					.executeUpdate();
			entityManager.createQuery("delete from VehicleEntity v where v.familyId = :familyId")
					.setParameter("familyId", seeded.familyId())
					.executeUpdate();
			entityManager.createQuery("delete from FamilyEntity f where f.familyId = :familyId")
					.setParameter("familyId", seeded.familyId())
					.executeUpdate();
		});
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private static String nextVin() {
		return VIN_PREFIX + String.format("%06d", VIN_SEQUENCE.getAndIncrement() % 1_000_000);
	}

	public record Result(long familyId, long guardianId, long minjunId, long seoyunId, long vehicleId, String vin) {
	}

}
