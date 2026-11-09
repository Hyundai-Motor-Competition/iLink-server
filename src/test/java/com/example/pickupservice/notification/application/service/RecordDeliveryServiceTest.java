package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.notification.application.port.in.DeliveryReceiptCommand;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.domain.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordDeliveryServiceTest {

	private static final long NOTIFICATION_ID = 7001L;
	private static final long SESSION_ID = 501L;
	private static final long CALL_ID = 3001L;
	private static final long CHILD_USER_ID = 21L;
	private static final long CHILD_DEVICE_ID = 902L;
	private static final long OTHER_USER_ID = 22L;
	private static final Instant SENT_AT = Instant.parse("2026-11-09T06:00:00Z");
	private static final Instant FIRST_RECEIVED_AT = Instant.parse("2026-11-09T06:00:05Z");
	private static final Instant SECOND_RECEIVED_AT = Instant.parse("2026-11-09T06:00:40Z");

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private SaveNotificationPort saveNotificationPort;
	@Mock
	private LoadPushTargetPort loadPushTargetPort;
	@Mock
	private CallProgressPort callProgressPort;

	private RecordDeliveryService service;

	@BeforeEach
	void setUp() {
		service = new RecordDeliveryService(loadNotificationPort, saveNotificationPort, loadPushTargetPort,
				callProgressPort);
	}

	@Test
	@DisplayName("수신 확인은 두 번째 receivedAt이 와도 첫 deliveredAt을 유지한다")
	void keepsFirstDeliveredAtWhenSecondReceiptArrives() {
		Notification notification = notification("CALL", CALL_ID, FIRST_RECEIVED_AT);
		givenOwnedNotification(notification);

		service.recordDelivery(command(CHILD_USER_ID, SECOND_RECEIVED_AT));

		assertThat(notification.getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort).save(saved.capture());
		assertThat(saved.getValue().getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	@Test
	@DisplayName("수신 확인은 첫 receivedAt을 deliveredAt에 채워 저장한다")
	void savesFirstReceivedAtAsDeliveredAt() {
		Notification notification = notification("CALL", CALL_ID, null);
		givenOwnedNotification(notification);

		service.recordDelivery(command(CHILD_USER_ID, FIRST_RECEIVED_AT));

		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort).save(saved.capture());
		assertThat(saved.getValue().getNotificationId()).isEqualTo(NOTIFICATION_ID);
		assertThat(saved.getValue().getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
	}

	@Test
	@DisplayName("CALL 알림의 첫 수신 확인은 CallProgressPort.markCallDelivered를 부른다")
	void firstDeliveryOfCallNotificationMarksCallDelivered() {
		Notification notification = notification("CALL", CALL_ID, null);
		givenOwnedNotification(notification);

		service.recordDelivery(command(CHILD_USER_ID, FIRST_RECEIVED_AT));

		verify(callProgressPort).markCallDelivered(CALL_ID, FIRST_RECEIVED_AT);
	}

	@Test
	@DisplayName("CALL이 아닌 알림의 첫 수신 확인은 markCallDelivered를 부르지 않는다")
	void firstDeliveryOfNonCallNotificationDoesNotMarkCallDelivered() {
		Notification notification = notification("ARRIVED", null, null);
		givenOwnedNotification(notification);

		service.recordDelivery(command(CHILD_USER_ID, FIRST_RECEIVED_AT));

		assertThat(notification.getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	@Test
	@DisplayName("다른 기기의 알림에 수신 확인을 보내면 NOT_FOUND로 거절한다")
	void rejectsReceiptForOthersDeviceWithNotFound() {
		Notification notification = notification("CALL", CALL_ID, null);
		when(loadNotificationPort.load(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));

		assertThatThrownBy(() -> service.recordDelivery(command(OTHER_USER_ID, FIRST_RECEIVED_AT)))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code())
						.isEqualTo(ErrorCode.NOT_FOUND.name()));

		assertThat(notification.getDeliveredAt()).isNull();
		verify(saveNotificationPort, never()).save(any());
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	@Test
	@DisplayName("없는 알림에 수신 확인을 보내면 NOT_FOUND로 거절한다")
	void rejectsReceiptForMissingNotificationWithNotFound() {
		when(loadNotificationPort.load(NOTIFICATION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.recordDelivery(command(CHILD_USER_ID, FIRST_RECEIVED_AT)))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code())
						.isEqualTo(ErrorCode.NOT_FOUND.name()));

		verify(saveNotificationPort, never()).save(any());
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	private void givenOwnedNotification(Notification notification) {
		when(loadNotificationPort.load(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private static Notification notification(String type, Long callId, Instant deliveredAt) {
		return Notification.restore(NOTIFICATION_ID, SESSION_ID, callId, type, CHILD_DEVICE_ID, SENT_AT, deliveredAt,
				null);
	}

	private static DeliveryReceiptCommand command(long userId, Instant receivedAt) {
		AuthPrincipal principal = new AuthPrincipal(PrincipalType.USER, userId, 1L, Role.CHILD);
		return new DeliveryReceiptCommand(principal, NOTIFICATION_ID, receivedAt);
	}
}
