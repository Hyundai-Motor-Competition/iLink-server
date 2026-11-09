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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
	private static final long OTHER_DEVICE_ID = 903L;
	private static final long OTHER_ROW_ID = 7002L;
	private static final long PREVIOUS_CALL_ID = 2999L;
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
	@DisplayName("같은 호출의 다른 기기 CALL 행이 이미 수신됐으면 이 행이 첫 수신이어도 markCallDelivered를 다시 부르지 않고 이 행의 deliveredAt은 채워 저장한다")
	void doesNotMarkCallDeliveredAgainWhenOtherDeviceRowOfSameCallWasAlreadyDelivered() {
		Notification notification = notification("CALL", CALL_ID, null);
		Notification otherDeviceRow = row(OTHER_ROW_ID, OTHER_DEVICE_ID, "CALL", CALL_ID, FIRST_RECEIVED_AT);
		givenOwnedNotification(notification);
		givenSessionRows(notification, otherDeviceRow);

		service.recordDelivery(command(CHILD_USER_ID, SECOND_RECEIVED_AT));

		assertThat(notification.getDeliveredAt()).isEqualTo(SECOND_RECEIVED_AT);
		verify(saveNotificationPort).save(notification);
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	@Test
	@DisplayName("같은 호출의 앞선 행(재발송 전 행)이 이미 수신됐으면 재발송 행의 첫 수신은 markCallDelivered를 다시 부르지 않는다")
	void doesNotMarkCallDeliveredAgainWhenEarlierAttemptRowOfSameCallWasAlreadyDelivered() {
		Notification resent = notification("CALL", CALL_ID, null);
		Notification earlierAttempt = row(OTHER_ROW_ID, CHILD_DEVICE_ID, "CALL", CALL_ID, FIRST_RECEIVED_AT);
		givenOwnedNotification(resent);
		givenSessionRows(earlierAttempt, resent);

		service.recordDelivery(command(CHILD_USER_ID, SECOND_RECEIVED_AT));

		assertThat(resent.getDeliveredAt()).isEqualTo(SECOND_RECEIVED_AT);
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	@Test
	@DisplayName("같은 호출의 다른 CALL 행이 아직 수신 전이면 이 행의 첫 수신에서 markCallDelivered를 한 번 부른다")
	void marksCallDeliveredWhenOtherRowsOfSameCallAreStillUndelivered() {
		Notification notification = notification("CALL", CALL_ID, null);
		Notification otherDeviceRow = row(OTHER_ROW_ID, OTHER_DEVICE_ID, "CALL", CALL_ID, null);
		givenOwnedNotification(notification);
		givenSessionRows(notification, otherDeviceRow);

		service.recordDelivery(command(CHILD_USER_ID, FIRST_RECEIVED_AT));

		verify(callProgressPort, times(1)).markCallDelivered(CALL_ID, FIRST_RECEIVED_AT);
	}

	@Test
	@DisplayName("다른 호출(callId)의 CALL 행이나 CALL이 아닌 행이 수신됐어도 이 호출의 첫 수신은 markCallDelivered를 부른다")
	void marksCallDeliveredWhenOnlyOtherCallsOrOtherTypesWereDelivered() {
		Notification notification = notification("CALL", CALL_ID, null);
		Notification previousCallRow = row(OTHER_ROW_ID, CHILD_DEVICE_ID, "CALL", PREVIOUS_CALL_ID, FIRST_RECEIVED_AT);
		Notification arrivedRow = row(OTHER_ROW_ID + 1, CHILD_DEVICE_ID, "ARRIVED", null, FIRST_RECEIVED_AT);
		givenOwnedNotification(notification);
		givenSessionRows(previousCallRow, arrivedRow, notification);

		service.recordDelivery(command(CHILD_USER_ID, SECOND_RECEIVED_AT));

		verify(callProgressPort).markCallDelivered(CALL_ID, SECOND_RECEIVED_AT);
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
		when(loadPushTargetPort.loadPushTargetIncludingRevoked(CHILD_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));

		assertThatThrownBy(() -> service.recordDelivery(command(OTHER_USER_ID, FIRST_RECEIVED_AT)))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code())
						.isEqualTo(ErrorCode.NOT_FOUND.name()));

		assertThat(notification.getDeliveredAt()).isNull();
		verify(saveNotificationPort, never()).save(any());
		verify(callProgressPort, never()).markCallDelivered(anyLong(), any());
	}

	@Test
	@DisplayName("수신 확인의 주인 확인은 폐기된 기기도 찾는 폐기 포함 조회를 쓰고 살아 있는 기기만 보는 조회는 쓰지 않는다")
	void ownerCheckUsesRevokedInclusiveLookupOnly() {
		Notification notification = notification("CALL", CALL_ID, null);
		givenOwnedNotification(notification);

		service.recordDelivery(command(CHILD_USER_ID, FIRST_RECEIVED_AT));

		assertThat(notification.getDeliveredAt()).isEqualTo(FIRST_RECEIVED_AT);
		verify(loadPushTargetPort).loadPushTargetIncludingRevoked(CHILD_DEVICE_ID);
		verify(loadPushTargetPort, never()).loadPushTarget(anyLong());
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
		when(loadPushTargetPort.loadPushTargetIncludingRevoked(CHILD_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	// 같은 세션의 알림 행 목록(구현이 loadBySession으로 읽는다고 보고 준비. 안 읽는 테스트가 있어도 실패하지 않게 lenient)
	private void givenSessionRows(Notification... rows) {
		lenient().when(loadNotificationPort.loadBySession(SESSION_ID)).thenReturn(List.of(rows));
	}

	private static Notification row(long notificationId, long deviceId, String type, Long callId,
			Instant deliveredAt) {
		return Notification.restore(notificationId, SESSION_ID, callId, type, deviceId, SENT_AT, deliveredAt, null);
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
