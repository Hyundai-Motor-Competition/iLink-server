package com.example.pickupservice.notification.fixture;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
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

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class, NotificationSeed.class})
class NotificationSeedTest extends IntegrationTestSupport {

	private final NotificationSeed notificationSeed;
	private final JdbcTemplate jdbcTemplate;

	private NotificationSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			notificationSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("테스트 시드로 CALL 알림 1건(자녀 기기, sentAt 있음 · deliveredAt 없음)을 만든다")
	void seedCreatesOneCallNotificationForChildDeviceSentButNotDelivered() {
		seeded = notificationSeed.seed();

		List<Map<String, Object>> notifications = jdbcTemplate.queryForList("""
				select notification_id, session_id, call_id, type, device_id,
				       sent_at is not null as has_sent_at,
				       delivered_at is null as no_delivered_at,
				       opened_at is null as no_opened_at
				from notification
				where session_id = ?
				""", seeded.sessionId());
		assertThat(notifications).singleElement().satisfies(notification -> {
			assertThat(((Number) notification.get("notification_id")).longValue()).isEqualTo(seeded.notificationId());
			assertThat(((Number) notification.get("session_id")).longValue()).isEqualTo(seeded.sessionId());
			assertThat(((Number) notification.get("call_id")).longValue()).isEqualTo(seeded.callId());
			assertThat(notification.get("type")).isEqualTo("CALL");
			assertThat(((Number) notification.get("device_id")).longValue()).isEqualTo(seeded.childDeviceId());
			assertThat(notification.get("has_sent_at")).as("sentAt은 있어야 한다").isEqualTo(true);
			assertThat(notification.get("no_delivered_at")).as("deliveredAt은 없어야 한다").isEqualTo(true);
			assertThat(notification.get("no_opened_at")).as("openedAt은 없어야 한다").isEqualTo(true);
		});

		Map<String, Object> device = jdbcTemplate.queryForMap(
				"select owner_type, owner_id, platform, revoked_at from device where device_id = ?",
				seeded.childDeviceId());
		assertThat(device.get("owner_type")).isEqualTo("USER");
		assertThat(((Number) device.get("owner_id")).longValue())
				.as("받는 기기는 자녀(민준)의 기기여야 한다")
				.isEqualTo(seeded.childUserId());
		assertThat(device.get("platform")).isIn("ANDROID", "IOS");
		assertThat(device.get("revoked_at")).isNull();

		Map<String, Object> childUser = jdbcTemplate.queryForMap(
				"select role, family_id from users where user_id = ?", seeded.childUserId());
		assertThat(childUser.get("role")).isEqualTo("CHILD");
		assertThat(((Number) childUser.get("family_id")).longValue()).isEqualTo(seeded.pickup().familyId());

		Map<String, Object> call = jdbcTemplate.queryForMap(
				"select session_id from pickup_call where call_id = ?", seeded.callId());
		assertThat(((Number) call.get("session_id")).longValue())
				.as("Call은 알림과 같은 세션의 것이어야 한다")
				.isEqualTo(seeded.sessionId());
	}

}
