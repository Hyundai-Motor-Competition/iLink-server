package com.example.pickupservice.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

	private static final long NOTIFICATION_ID = 7001L;
	private static final long SESSION_ID = 11L;
	private static final long CALL_ID = 55L;
	private static final String TYPE = "CALL";
	private static final long DEVICE_ID = 301L;
	private static final Instant SENT_AT = Instant.parse("2026-11-05T09:09:02Z");
	private static final Instant FIRST_RECEIVED_AT = Instant.parse("2026-11-05T09:09:04Z");
	private static final Instant SECOND_RECEIVED_AT = Instant.parse("2026-11-05T09:09:30Z");
	private static final Instant OPENED_AT = Instant.parse("2026-11-05T09:09:10Z");

	@Test
	@DisplayName("markDelivered는 deliveredAt이 비어 있으면 receivedAt을 채운다")
	void markDeliveredFillsDeliveredAtWhenEmpty() {
		Notification notification = Notification.restore(NOTIFICATION_ID, SESSION_ID, CALL_ID, TYPE, DEVICE_ID,
				SENT_AT, null, null);

		notification.markDelivered(FIRST_RECEIVED_AT);

		assertThat(notification.getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
	}

	@Test
	@DisplayName("markDelivered는 deliveredAt이 이미 있으면 다시 와도 바꾸지 않는다")
	void markDeliveredKeepsFirstDeliveredAtWhenAlreadyFilled() {
		Notification notification = Notification.restore(NOTIFICATION_ID, SESSION_ID, CALL_ID, TYPE, DEVICE_ID,
				SENT_AT, FIRST_RECEIVED_AT, null);

		notification.markDelivered(SECOND_RECEIVED_AT);

		assertThat(notification.getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
	}

	@Test
	@DisplayName("markDelivered를 두 번 부르면 첫 receivedAt만 남는다")
	void markDeliveredTwiceKeepsOnlyFirstReceivedAt() {
		Notification notification = Notification.restore(NOTIFICATION_ID, SESSION_ID, CALL_ID, TYPE, DEVICE_ID,
				SENT_AT, null, OPENED_AT);

		notification.markDelivered(FIRST_RECEIVED_AT);
		notification.markDelivered(SECOND_RECEIVED_AT);

		assertThat(notification.getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
		assertThat(notification.getSentAt()).isEqualTo(SENT_AT);
		assertThat(notification.getOpenedAt()).isEqualTo(OPENED_AT);
	}

	@Test
	@DisplayName("markSent는 sentAt을 채운다")
	void markSentFillsSentAt() {
		Notification notification = Notification.restore(NOTIFICATION_ID, SESSION_ID, CALL_ID, TYPE, DEVICE_ID,
				null, null, null);

		notification.markSent(SENT_AT);

		assertThat(notification.getSentAt()).isEqualTo(SENT_AT);
		assertThat(notification.getDeliveredAt()).isNull();
		assertThat(notification.getOpenedAt()).isNull();
	}
}
