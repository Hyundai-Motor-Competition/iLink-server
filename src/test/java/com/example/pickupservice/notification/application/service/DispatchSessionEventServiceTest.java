package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.notification.application.port.in.SessionEventMessage;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchSessionEventServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-09T01:00:00Z");
	private static final Instant OCCURRED_AT = Instant.parse("2026-11-09T00:59:30Z");
	private static final long SESSION_ID = 501L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long PHONE_DEVICE_ID = 901L;
	private static final long TABLET_DEVICE_ID = 902L;
	private static final long CO_PHONE_DEVICE_ID = 903L;
	private static final long EVENT_ID = 9001L;
	private static final long SEQ = 12L;
	private static final int STATE_VERSION = 7;

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

	private DispatchSessionEventService service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		service = new DispatchSessionEventService(loadNotificationPort, saveNotificationPort, sendPushPort,
				loadPushTargetPort, callProgressPort, clock);
	}

	@Test
	@DisplayName("발송 리스너는 FCM 성공 때 sentAt을 채우고 실패 때 sentAt을 비워 둔다")
	void fillsSentAtOnSuccessAndKeepsItEmptyOnFailure() {
		Notification succeeded = Notification.restore(7001L, SESSION_ID, null, "SESSION_STARTED", PHONE_DEVICE_ID,
				null, null, null);
		Notification failed = Notification.restore(7002L, SESSION_ID, null, "SESSION_STARTED", TABLET_DEVICE_ID,
				null, null, null);
		PushTarget phone = target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone");
		PushTarget tablet = target(GUARDIAN_ID, TABLET_DEVICE_ID, "token-tablet");
		when(loadNotificationPort.loadPending(SESSION_ID, "SESSION_STARTED", null))
				.thenReturn(List.of(succeeded, failed));
		when(loadPushTargetPort.loadPushTarget(PHONE_DEVICE_ID)).thenReturn(Optional.of(phone));
		when(loadPushTargetPort.loadPushTarget(TABLET_DEVICE_ID)).thenReturn(Optional.of(tablet));
		when(sendPushPort.send(any(PushMessage.class))).thenAnswer(invocation -> {
			PushMessage message = invocation.getArgument(0);
			return message.target().deviceId() == PHONE_DEVICE_ID;
		});
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.dispatch(rowEvent());

		assertThat(succeeded.getSentAt()).isEqualTo(NOW);
		assertThat(failed.getSentAt()).isNull();
		verify(saveNotificationPort).save(succeeded);
	}

	@Test
	@DisplayName("발송 리스너가 보내는 FCM 데이터에는 notificationId가 들어 있다")
	void sendsNotificationIdInPushData() {
		Notification row = Notification.restore(7001L, SESSION_ID, null, "SESSION_STARTED", PHONE_DEVICE_ID,
				null, null, null);
		Notification otherRow = Notification.restore(7002L, SESSION_ID, null, "SESSION_STARTED", TABLET_DEVICE_ID,
				null, null, null);
		when(loadNotificationPort.loadPending(SESSION_ID, "SESSION_STARTED", null))
				.thenReturn(List.of(row, otherRow));
		when(loadPushTargetPort.loadPushTarget(PHONE_DEVICE_ID))
				.thenReturn(Optional.of(target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone")));
		when(loadPushTargetPort.loadPushTarget(TABLET_DEVICE_ID))
				.thenReturn(Optional.of(target(GUARDIAN_ID, TABLET_DEVICE_ID, "token-tablet")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.dispatch(rowEvent());

		ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort, times(2)).send(captor.capture());
		PushMessage toPhone = pushTo(captor.getAllValues(), PHONE_DEVICE_ID);
		PushMessage toTablet = pushTo(captor.getAllValues(), TABLET_DEVICE_ID);
		assertThat(toPhone.data()).containsEntry("notificationId", "7001");
		assertThat(toTablet.data()).containsEntry("notificationId", "7002");
		assertThat(toPhone.title()).isNotBlank();
		assertThat(toPhone.body()).isNotBlank();
	}

	@Test
	@DisplayName("발송 리스너는 행이 없는 상태 변화 이벤트를 보호자 · 자녀 기기에 eventId · seq · stateVersion · occurredAt · payload 데이터 메시지로 보낸다")
	void sendsDataMessageToReceiverDevicesWhenEventHasNoRows() {
		PushTarget guardianPhone = target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone");
		PushTarget guardianTablet = target(GUARDIAN_ID, TABLET_DEVICE_ID, "token-tablet");
		PushTarget coGuardianPhone = target(CO_GUARDIAN_ID, CO_PHONE_DEVICE_ID, "token-co-phone");
		when(loadPushTargetPort.loadPushTargets(List.of(GUARDIAN_ID, CO_GUARDIAN_ID)))
				.thenReturn(List.of(guardianPhone, guardianTablet, coGuardianPhone));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		service.dispatch(new SessionEventMessage(SESSION_ID, EVENT_ID, SEQ, "VEHICLE_STOPPED", STATE_VERSION,
				OCCURRED_AT, Map.of("spotId", 77), List.of(GUARDIAN_ID, CO_GUARDIAN_ID)));

		ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort, times(3)).send(captor.capture());
		assertThat(captor.getAllValues()).extracting(message -> message.target().deviceId())
				.containsExactlyInAnyOrder(PHONE_DEVICE_ID, TABLET_DEVICE_ID, CO_PHONE_DEVICE_ID);
		for (PushMessage message : captor.getAllValues()) {
			assertThat(message.title()).isNull();
			assertThat(message.body()).isNull();
			assertThat(message.data()).containsEntry("eventId", "9001")
					.containsEntry("seq", "12")
					.containsEntry("stateVersion", "7")
					.containsKeys("occurredAt", "payload")
					.doesNotContainKey("notificationId");
			assertThat(message.data().get("occurredAt")).contains("2026-11-09T");
			assertThat(message.data().get("payload")).contains("spotId").contains("77");
		}
		verify(saveNotificationPort, never()).save(any(Notification.class));
	}

	@Test
	@DisplayName("발송 리스너는 ETA_UPDATED와 VEHICLE_LOCATION을 FCM으로 보내지 않는다")
	void doesNotSendEtaUpdatedAndVehicleLocation() {
		service.dispatch(new SessionEventMessage(SESSION_ID, null, null, "ETA_UPDATED", STATE_VERSION, OCCURRED_AT,
				Map.of("remainingMeters", 800), List.of(GUARDIAN_ID, CO_GUARDIAN_ID)));
		service.dispatch(new SessionEventMessage(SESSION_ID, null, null, "VEHICLE_LOCATION", STATE_VERSION,
				OCCURRED_AT, Map.of("lat", 37.5), List.of(GUARDIAN_ID, CO_GUARDIAN_ID)));

		verifyNoInteractions(sendPushPort, saveNotificationPort);
	}

	@Test
	@DisplayName("발송 리스너는 받는 쪽이 차량 · 보호자인 이벤트를 message의 receiverUserIds(보호자)에게만 보내고 자녀 기기에는 보내지 않는다")
	void sendsGuardianOnlyEventsOnlyToReceiverUserIds() {
		long childId = 41L;
		long childDeviceId = 911L;
		PushTarget guardianPhone = target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone");
		when(loadPushTargetPort.loadPushTargets(List.of(GUARDIAN_ID))).thenReturn(List.of(guardianPhone));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		for (String type : List.of("CALL_SENT", "CALL_DELIVERED", "CALL_ACKNOWLEDGED", "BOARDING_DETECTED",
				"SESSION_HELD", "SESSION_RESUMED")) {
			service.dispatch(new SessionEventMessage(SESSION_ID, EVENT_ID, SEQ, type, STATE_VERSION, OCCURRED_AT,
					Map.of("callId", 5001), List.of(GUARDIAN_ID)));
		}

		ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort, times(6)).send(captor.capture());
		assertThat(captor.getAllValues()).extracting(message -> message.target().userId())
				.containsOnly(GUARDIAN_ID);
		assertThat(captor.getAllValues()).extracting(message -> message.target().deviceId())
				.doesNotContain(childDeviceId);
		verify(loadPushTargetPort, never()).loadPushTargets(List.of(GUARDIAN_ID, childId));
		verify(loadPushTargetPort, never()).loadPushTarget(childDeviceId);
	}

	@Test
	@DisplayName("발송 리스너는 차량 기기(platform VEHICLE)에는 데이터 메시지도 알림도 FCM으로 보내지 않는다")
	void doesNotSendPushToVehicleDevices() {
		long vehicleDeviceId = 950L;
		PushTarget guardianPhone = target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone");
		PushTarget vehicle = new PushTarget(GUARDIAN_ID, vehicleDeviceId, Platform.VEHICLE, "token-vehicle");
		when(loadPushTargetPort.loadPushTargets(List.of(GUARDIAN_ID))).thenReturn(List.of(guardianPhone, vehicle));
		Notification vehicleRow = Notification.restore(7003L, SESSION_ID, null, "SESSION_STARTED", vehicleDeviceId,
				null, null, null);
		when(loadNotificationPort.loadPending(SESSION_ID, "SESSION_STARTED", null)).thenReturn(List.of(vehicleRow));
		when(loadPushTargetPort.loadPushTarget(vehicleDeviceId)).thenReturn(Optional.of(vehicle));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		service.dispatch(new SessionEventMessage(SESSION_ID, EVENT_ID, SEQ, "VEHICLE_STOPPED", STATE_VERSION,
				OCCURRED_AT, Map.of("spotId", 77), List.of(GUARDIAN_ID)));
		service.dispatch(rowEvent());

		ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort, times(1)).send(captor.capture());
		assertThat(captor.getValue().target().deviceId()).isEqualTo(PHONE_DEVICE_ID);
		assertThat(captor.getValue().target().platform()).isNotEqualTo(Platform.VEHICLE);
	}

	@Test
	@DisplayName("발송 리스너는 CALL 알림 발송이 성공하면 callProgressPort.markCallSent(callId, 지금)을 부른다")
	void marksCallSentWhenCallPushSucceeds() {
		long callId = 5001L;
		Notification row = Notification.restore(7101L, SESSION_ID, callId, "CALL", PHONE_DEVICE_ID, null, null, null);
		when(loadNotificationPort.loadPending(SESSION_ID, "CALL", callId)).thenReturn(List.of(row));
		when(loadPushTargetPort.loadPushTarget(PHONE_DEVICE_ID))
				.thenReturn(Optional.of(target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.dispatch(callEvent(callId));

		verify(callProgressPort).markCallSent(callId, NOW);
	}

	@Test
	@DisplayName("발송 리스너는 CALL 알림 발송이 실패하면 callProgressPort.markCallSent를 부르지 않는다")
	void doesNotMarkCallSentWhenCallPushFails() {
		long callId = 5001L;
		Notification row = Notification.restore(7101L, SESSION_ID, callId, "CALL", PHONE_DEVICE_ID, null, null, null);
		when(loadNotificationPort.loadPending(SESSION_ID, "CALL", callId)).thenReturn(List.of(row));
		when(loadPushTargetPort.loadPushTarget(PHONE_DEVICE_ID))
				.thenReturn(Optional.of(target(GUARDIAN_ID, PHONE_DEVICE_ID, "token-phone")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(false);

		service.dispatch(callEvent(callId));

		verify(callProgressPort, never()).markCallSent(anyLong(), any(Instant.class));
	}

	private static SessionEventMessage callEvent(long callId) {
		return new SessionEventMessage(SESSION_ID, EVENT_ID, SEQ, "CALL_TRIGGERED", STATE_VERSION, OCCURRED_AT,
				Map.of("callId", callId), List.of(GUARDIAN_ID));
	}

	private static SessionEventMessage rowEvent() {
		return new SessionEventMessage(SESSION_ID, EVENT_ID, SEQ, "SESSION_STARTED", STATE_VERSION, OCCURRED_AT,
				Map.of("stage", "STARTED"), List.of(GUARDIAN_ID));
	}

	private static PushTarget target(long userId, long deviceId, String pushToken) {
		return new PushTarget(userId, deviceId, Platform.ANDROID, pushToken);
	}

	private static PushMessage pushTo(List<PushMessage> messages, long deviceId) {
		return messages.stream()
				.filter(message -> message.target().deviceId() == deviceId)
				.findFirst()
				.orElseThrow();
	}
}
