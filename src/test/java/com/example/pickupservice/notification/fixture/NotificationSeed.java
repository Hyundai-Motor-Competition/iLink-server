package com.example.pickupservice.notification.fixture;

import com.example.pickupservice.auth.adapter.out.persistence.DeviceEntity;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.auth.fixture.AuthSeed;
import com.example.pickupservice.notification.adapter.out.persistence.NotificationEntity;
import com.example.pickupservice.pickup.adapter.out.persistence.CallEntity;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@TestComponent
@RequiredArgsConstructor
public class NotificationSeed {

	public static final String CALL_TYPE = "CALL";
	public static final String CHILD_PUSH_TOKEN = "fcm-token-seed-child";
	public static final String CHILD_REFRESH_TOKEN = "rt_seed_child_device";

	private final PickupSeed pickupSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public Result seed() {
		PickupSeed.Result pickup = pickupSeed.seed();
		Instant sentAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
		return transactionTemplate.execute(status -> {
			CallEntity call = persist(new CallEntity(null, pickup.sessionId(), null, CallTriggerType.MANUAL,
					CallStatus.SENT, 0, sentAt, null, null, null, null));
			DeviceEntity device = persist(new DeviceEntity(null, OwnerType.USER, pickup.minjunId(), Platform.ANDROID,
					CHILD_PUSH_TOKEN, sentAt, AuthSeed.sha256(CHILD_REFRESH_TOKEN), null));
			entityManager.flush();
			NotificationEntity notification = persist(new NotificationEntity(null, pickup.sessionId(),
					call.getCallId(), CALL_TYPE, device.getDeviceId(), sentAt, null, null));
			entityManager.flush();
			return new Result(pickup, call.getCallId(), device.getDeviceId(), notification.getNotificationId());
		});
	}

	public void clear(Result seeded) {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from NotificationEntity n where n.sessionId = :sessionId")
					.setParameter("sessionId", seeded.sessionId())
					.executeUpdate();
			entityManager.createQuery("delete from DeviceEntity d where d.deviceId = :deviceId")
					.setParameter("deviceId", seeded.childDeviceId())
					.executeUpdate();
			entityManager.createQuery("delete from CallEntity c where c.sessionId = :sessionId")
					.setParameter("sessionId", seeded.sessionId())
					.executeUpdate();
		});
		pickupSeed.clear(seeded.pickup());
	}

	private <T> T persist(T entity) {
		entityManager.persist(entity);
		return entity;
	}

	public record Result(PickupSeed.Result pickup, long callId, long childDeviceId, long notificationId) {

		public long sessionId() {
			return pickup.sessionId();
		}

		public long childUserId() {
			return pickup.minjunId();
		}

	}

}
