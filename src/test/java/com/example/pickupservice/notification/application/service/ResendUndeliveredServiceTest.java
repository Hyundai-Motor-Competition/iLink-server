package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import com.example.pickupservice.notification.domain.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendUndeliveredServiceTest {

	private static final long FIRST_ID = 7001L;
	private static final long SECOND_ID = 7002L;
	private static final long OTHER_ID = 7003L;
	private static final long NEW_ID = 8001L;
	private static final long SESSION_ID = 501L;
	private static final long CALL_ID = 3001L;
	private static final long CHILD_DEVICE_ID = 902L;
	private static final long GUARDIAN_DEVICE_ID = 901L;
	private static final long CHILD_USER_ID = 21L;
	private static final long GUARDIAN_USER_ID = 11L;
	private static final Instant NOW = Instant.parse("2026-11-09T06:10:00Z");
	private static final Instant SENT_AT = Instant.parse("2026-11-09T06:00:00Z");

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private SaveNotificationPort saveNotificationPort;
	@Mock
	private SendPushPort sendPushPort;
	@Mock
	private LoadPushTargetPort loadPushTargetPort;
	@Mock
	private CallProgressPort callProgressPort;

	private ResendUndeliveredService service;

	@BeforeEach
	void setUp() {
		service = new ResendUndeliveredService(loadNotificationPort, saveNotificationPort, sendPushPort,
				loadPushTargetPort, callProgressPort, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	@DisplayName("재발송 작업은 deliveredAt이 빈 알림을 같은 세션 · callId · type · 기기로 새 행을 만들어 다시 보내고 새 notificationId를 data에 담는다")
	void resendsUndeliveredNotificationAsNewRow() {
		Notification undelivered = row(FIRST_ID, "DEPARTURE", null, GUARDIAN_DEVICE_ID, SENT_AT, null);
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(undelivered));
		when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(1);
		givenSaveAssignsNewId();
		PushTarget target = new PushTarget(GUARDIAN_USER_ID, GUARDIAN_DEVICE_ID, Platform.ANDROID, "fcm-token");
		when(loadPushTargetPort.loadPushTarget(GUARDIAN_DEVICE_ID)).thenReturn(Optional.of(target));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isEqualTo(1);

		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort, atLeastOnce()).save(saved.capture());
		Notification created = saved.getAllValues().get(0);
		assertThat(created.getNotificationId()).isNull();
		assertThat(created.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(created.getCallId()).isNull();
		assertThat(created.getType()).isEqualTo("DEPARTURE");
		assertThat(created.getDeviceId()).isEqualTo(GUARDIAN_DEVICE_ID);
		Notification last = saved.getAllValues().get(saved.getAllValues().size() - 1);
		assertThat(last.getNotificationId()).isEqualTo(NEW_ID);
		assertThat(last.getSentAt()).isEqualTo(NOW);

		ArgumentCaptor<PushMessage> pushed = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort).send(pushed.capture());
		assertThat(pushed.getValue().target()).isEqualTo(target);
		assertThat(pushed.getValue().title()).isEqualTo(NotificationTexts.title("DEPARTURE"));
		assertThat(pushed.getValue().body()).isEqualTo(NotificationTexts.body("DEPARTURE"));
		assertThat(pushed.getValue().data()).containsEntry("notificationId", String.valueOf(NEW_ID));
	}

	@Test
	@DisplayName("재발송 작업은 같은 묶음(세션 · callId · type · 기기)의 행이 둘 다 대상으로 와도 한 번만 보낸다")
	void sendsOnlyOnceWhenTwoRowsOfSameGroupAreTargets() {
		Notification first = row(FIRST_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, SENT_AT, null);
		Notification second = row(SECOND_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:05:00Z"),
				null);
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(first, second));
		lenient().when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(2);
		when(callProgressPort.isCallAcknowledged(CALL_ID)).thenReturn(false);
		givenSaveAssignsNewId();
		PushTarget target = new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token");
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(target));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isEqualTo(1);
		verify(sendPushPort, times(1)).send(any(PushMessage.class));
		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort, atLeastOnce()).save(saved.capture());
		assertThat(saved.getAllValues()).filteredOn(n -> n.getNotificationId() == null).hasSize(1);
	}

	@Test
	@DisplayName("재발송 작업은 서로 다른 기기의 행은 묶음이 달라 각각 한 번씩 보낸다")
	void sendsOncePerGroupWhenDevicesDiffer() {
		Notification childRow = row(FIRST_ID, "DEPARTURE", null, CHILD_DEVICE_ID, SENT_AT, null);
		Notification guardianRow = row(OTHER_ID, "DEPARTURE", null, GUARDIAN_DEVICE_ID, SENT_AT, null);
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(childRow, guardianRow));
		when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(1);
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(
				Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "child-token")));
		when(loadPushTargetPort.loadPushTarget(GUARDIAN_DEVICE_ID)).thenReturn(
				Optional.of(new PushTarget(GUARDIAN_USER_ID, GUARDIAN_DEVICE_ID, Platform.IOS, "guardian-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isEqualTo(2);
		verify(sendPushPort, times(2)).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("재발송 작업은 확인된 호출(isCallAcknowledged true)의 CALL 알림은 다시 보내지 않고 새 행도 만들지 않는다")
	void doesNotResendCallNotificationOfAcknowledgedCall() {
		Notification call = row(FIRST_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, SENT_AT, null);
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(call));
		lenient().when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(1);
		when(callProgressPort.isCallAcknowledged(CALL_ID)).thenReturn(true);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isZero();
		verify(sendPushPort, never()).send(any());
		verify(saveNotificationPort, never()).save(any());
	}

	@Test
	@DisplayName("재발송 작업은 CALL 알림이 수신 확인(deliveredAt)됐어도 호출이 확인되지 않았으면 다시 보낸다")
	void resendsCallNotificationDeliveredButNotAcknowledged() {
		Notification call = row(FIRST_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, SENT_AT, Instant.parse("2026-11-09T06:01:00Z"));
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(call));
		when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(1);
		when(callProgressPort.isCallAcknowledged(CALL_ID)).thenReturn(false);
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(
				Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isEqualTo(1);
		verify(sendPushPort, times(1)).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("재발송 작업은 같은 알림의 행이 3개(처음 발송 + 재발송 2번)면 더 보내지 않고 새 행도 만들지 않는다")
	void doesNotResendWhenAttemptsReachedThree() {
		Notification third = row(OTHER_ID, "DEPARTURE", null, GUARDIAN_DEVICE_ID, SENT_AT, null);
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(third));
		when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(3);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isZero();
		verify(sendPushPort, never()).send(any());
		verify(saveNotificationPort, never()).save(any());
	}

	@Test
	@DisplayName("재발송 작업은 같은 알림의 행이 2개일 때까지는 한 번 더 보내 재발송이 처음 발송 뒤 2번이 되게 한다")
	void resendsWhenAttemptsAreTwo() {
		Notification second = row(SECOND_ID, "DEPARTURE", null, GUARDIAN_DEVICE_ID, SENT_AT, null);
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of(second));
		when(loadNotificationPort.countAttempts(any(Notification.class))).thenReturn(2);
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(GUARDIAN_DEVICE_ID)).thenReturn(
				Optional.of(new PushTarget(GUARDIAN_USER_ID, GUARDIAN_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isEqualTo(1);
		verify(sendPushPort, times(1)).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("재발송 작업은 대상이 없으면 0건을 돌려주고 아무것도 보내지 않는다")
	void returnsZeroWhenNoTargets() {
		when(loadNotificationPort.loadResendTargets(NOW)).thenReturn(List.of());

		int sent = service.resendUndelivered(NOW);

		assertThat(sent).isZero();
		verify(sendPushPort, never()).send(any());
		verify(saveNotificationPort, never()).save(any());
	}

	private void givenSaveAssignsNewId() {
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> {
			Notification n = invocation.getArgument(0);
			if (n.getNotificationId() != null) {
				return n;
			}
			return Notification.restore(NEW_ID, n.getSessionId(), n.getCallId(), n.getType(), n.getDeviceId(),
					n.getSentAt(), n.getDeliveredAt(), n.getOpenedAt());
		});
	}

	private static Notification row(long id, String type, Long callId, long deviceId, Instant sentAt,
			Instant deliveredAt) {
		return Notification.restore(id, SESSION_ID, callId, type, deviceId, sentAt, deliveredAt, null);
	}
}
