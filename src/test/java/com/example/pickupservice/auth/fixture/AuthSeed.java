package com.example.pickupservice.auth.fixture;

import com.example.pickupservice.auth.adapter.out.persistence.DeviceEntity;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.user.adapter.out.persistence.VehicleEntity;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@TestComponent
@RequiredArgsConstructor
public class AuthSeed {

	public static final String UNLINKED_VEHICLE_MODEL = "EV6";

	private static final String VIN_ALPHABET = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";
	private static final String TOKEN_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserSeed userSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	public Result seed() {
		UserSeed.Result user = userSeed.seed();
		return transactionTemplate.execute(status -> {
			String unlinkedVin = randomVin();
			VehicleEntity unlinkedVehicle = persist(
					new VehicleEntity(null, null, unlinkedVin, UNLINKED_VEHICLE_MODEL, null, null));
			String unlinkedRefreshToken = randomRefreshToken();
			DeviceEntity unlinkedDevice = persist(vehicleDevice(unlinkedVehicle.getVehicleId(), unlinkedRefreshToken));
			String linkedRefreshToken = randomRefreshToken();
			DeviceEntity linkedDevice = persist(vehicleDevice(user.vehicleId(), linkedRefreshToken));
			entityManager.flush();
			return new Result(user, user.familyId(),
					unlinkedVehicle.getVehicleId(), unlinkedVin, unlinkedDevice.getDeviceId(), unlinkedRefreshToken,
					user.vehicleId(), user.vin(), linkedDevice.getDeviceId(), linkedRefreshToken);
		});
	}

	public void clear(Result seeded) {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from DeviceEntity d where d.deviceId in :deviceIds")
					.setParameter("deviceIds", List.of(seeded.unlinkedDeviceId(), seeded.linkedDeviceId()))
					.executeUpdate();
			entityManager.createQuery("delete from VehicleEntity v where v.vehicleId = :vehicleId")
					.setParameter("vehicleId", seeded.unlinkedVehicleId())
					.executeUpdate();
		});
		userSeed.clear(seeded.user());
	}

	public static String sha256(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static DeviceEntity vehicleDevice(long vehicleId, String rawRefreshToken) {
		return new DeviceEntity(null, OwnerType.VEHICLE, vehicleId, Platform.VEHICLE, null, null,
				sha256(rawRefreshToken), null);
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	private static String randomVin() {
		return randomString(VIN_ALPHABET, 17);
	}

	private static String randomRefreshToken() {
		return "rt_" + randomString(TOKEN_ALPHABET, 40);
	}

	private static String randomString(String alphabet, int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
		}
		return builder.toString();
	}

	public record Result(
			UserSeed.Result user,
			long familyId,
			long unlinkedVehicleId,
			String unlinkedVin,
			long unlinkedDeviceId,
			String unlinkedRefreshToken,
			long linkedVehicleId,
			String linkedVin,
			long linkedDeviceId,
			String linkedRefreshToken) {
	}

}
