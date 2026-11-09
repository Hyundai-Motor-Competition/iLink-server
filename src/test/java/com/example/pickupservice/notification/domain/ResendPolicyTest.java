package com.example.pickupservice.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResendPolicyTest {

	private static final long SESSION_ID = 11L;
	private static final long CALL_ID = 55L;
	private static final long DEVICE_ID = 301L;
	private static final Instant SENT_AT = Instant.parse("2026-11-05T09:09:02Z");
	private static final Instant DELIVERED_AT = Instant.parse("2026-11-05T09:09:04Z");

	private static Notification notification(String type, Instant sentAt, Instant deliveredAt) {
		return Notification.restore(7001L, SESSION_ID, CALL_ID, type, DEVICE_ID, sentAt, deliveredAt, null);
	}

	@Test
	@DisplayName("attempt가 3이면 deliveredAt이 비어 있어도 더 보내지 않는다")
	void doesNotResendWhenAttemptIsThree() {
		Notification notification = notification("CALL", SENT_AT, null);

		assertThat(ResendPolicy.shouldResend(notification, 3, false)).isFalse();
	}

	@Test
	@DisplayName("attempt가 3을 넘어도 더 보내지 않는다")
	void doesNotResendWhenAttemptExceedsThree() {
		Notification notification = notification("CALL", SENT_AT, null);

		assertThat(ResendPolicy.shouldResend(notification, 4, false)).isFalse();
	}

	@Test
	@DisplayName("attempt가 2이고 deliveredAt이 비어 있으면 CALL이 아닌 알림도 다시 보낸다")
	void resendsWhenAttemptIsTwoAndNotDelivered() {
		Notification notification = notification("PICKUP_STARTED", SENT_AT, null);

		assertThat(ResendPolicy.shouldResend(notification, 2, false)).isTrue();
	}

	@Test
	@DisplayName("CALL 알림은 deliveredAt이 있어도 호출이 확인되지 않았으면 재발송 대상이다")
	void resendsCallNotificationDeliveredButNotAcknowledged() {
		Notification notification = notification("CALL", SENT_AT, DELIVERED_AT);

		assertThat(ResendPolicy.shouldResend(notification, 1, false)).isTrue();
	}

	@Test
	@DisplayName("CALL 알림은 호출이 확인되었으면 deliveredAt이 비어 있어도 재발송 대상이 아니다")
	void doesNotResendCallNotificationWhenAcknowledged() {
		Notification notification = notification("CALL", SENT_AT, null);

		assertThat(ResendPolicy.shouldResend(notification, 1, true)).isFalse();
	}

	@Test
	@DisplayName("CALL이 아닌 알림은 deliveredAt이 있으면 재발송 대상에서 뺀다")
	void doesNotResendNonCallNotificationWhenDelivered() {
		Notification notification = notification("PICKUP_STARTED", SENT_AT, DELIVERED_AT);

		assertThat(ResendPolicy.shouldResend(notification, 1, false)).isFalse();
	}

	@Test
	@DisplayName("CALL이 아닌 알림은 sentAt이 비어 있고 deliveredAt도 비어 있으면 재발송 대상이다")
	void resendsNonCallNotificationWhenNeverSent() {
		Notification notification = notification("PICKUP_STARTED", null, null);

		assertThat(ResendPolicy.shouldResend(notification, 1, false)).isTrue();
	}

	@Test
	@DisplayName("마지막 발송 뒤 30초가 지났으면 RESEND_TOO_SOON으로 거절하고 retryAfterSeconds는 남은 30초다")
	void rejectsResendWithin30SecondsAfterLastSent() {
		Instant now = SENT_AT.plusSeconds(30);

		assertThatThrownBy(() -> ResendPolicy.ensureResendAllowed(SENT_AT, now))
				.isInstanceOfSatisfying(NotificationRuleException.class, exception -> {
					assertThat(exception.violation()).isEqualTo(NotificationViolation.RESEND_TOO_SOON);
					assertThat(exception.retryAfterSeconds()).isEqualTo(30);
				});
	}

	@Test
	@DisplayName("마지막 발송 뒤 59초가 지났으면 RESEND_TOO_SOON으로 거절한다")
	void rejectsResendAt59SecondsAfterLastSent() {
		Instant now = SENT_AT.plusSeconds(59);

		assertThatThrownBy(() -> ResendPolicy.ensureResendAllowed(SENT_AT, now))
				.isInstanceOfSatisfying(NotificationRuleException.class,
						exception -> assertThat(exception.violation()).isEqualTo(NotificationViolation.RESEND_TOO_SOON));
	}

	@Test
	@DisplayName("마지막 발송 뒤 정확히 1분이 지났으면 거절하지 않는다")
	void allowsResendExactlyOneMinuteAfterLastSent() {
		Instant now = SENT_AT.plusSeconds(60);

		assertThatCode(() -> ResendPolicy.ensureResendAllowed(SENT_AT, now)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("마지막 발송 뒤 1분이 넘게 지났으면 거절하지 않는다")
	void allowsResendAfterMoreThanOneMinute() {
		Instant now = SENT_AT.plusSeconds(61);

		assertThatCode(() -> ResendPolicy.ensureResendAllowed(SENT_AT, now)).doesNotThrowAnyException();
	}
}
