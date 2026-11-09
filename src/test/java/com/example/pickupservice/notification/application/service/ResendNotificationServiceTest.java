package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.notification.application.port.in.ResendCommand;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendNotificationServiceTest {

	private static final long ORIGINAL_ID = 7001L;
	private static final long RESENT_ID = 7002L;
	private static final long OTHER_GROUP_ID = 7003L;
	private static final long NEW_ID = 8001L;
	private static final long FAMILY_ID = 1L;
	private static final long OTHER_FAMILY_ID = 2L;
	private static final long SESSION_ID = 501L;
	private static final long CALL_ID = 3001L;
	private static final long CHILD_DEVICE_ID = 902L;
	private static final long OTHER_DEVICE_ID = 903L;
	private static final long CHILD_USER_ID = 21L;
	private static final Instant NOW = Instant.parse("2026-11-09T06:10:00Z");

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private SaveNotificationPort saveNotificationPort;
	@Mock
	private SendPushPort sendPushPort;
	@Mock
	private LoadPushTargetPort loadPushTargetPort;

	@Mock
	private LoadSessionFamilyPort loadSessionFamilyPort;

	private ResendNotificationService service;

	@BeforeEach
	void setUp() {
		service = new ResendNotificationService(loadNotificationPort, saveNotificationPort, sendPushPort,
				loadPushTargetPort, loadSessionFamilyPort, Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.of(FAMILY_ID));
	}

	@Test
	@DisplayName("다시 보내기는 알림의 세션이 요청자 가족의 것이 아니면 NOT_FOUND로 거절하고 새 행도 FCM도 만들지 않는다")
	void rejectsOtherFamilyNotificationWithNotFound() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.of(OTHER_FAMILY_ID));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND.name()));

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("다시 보내기는 알림의 세션을 찾을 수 없으면 NOT_FOUND로 거절하고 새 행도 FCM도 만들지 않는다")
	void rejectsNotificationOfMissingSessionWithNotFound() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND.name()));

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("다시 보내기는 같은 세션 · callId · type · 기기로 새 행을 만들고 FCM을 보내고 새 notificationId를 돌려준다")
	void resendCreatesNewRowSendsPushAndReturnsNewId() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		Notification otherGroup = row(OTHER_GROUP_ID, "CALL", CALL_ID, OTHER_DEVICE_ID,
				Instant.parse("2026-11-09T06:09:59Z"));
		givenOriginal(original, List.of(original, otherGroup));
		givenSaveAssignsNewId();
		PushTarget target = new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token");
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(target));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		long newId = service.resend(command());

		assertThat(newId).isEqualTo(NEW_ID);

		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort, atLeastOnce()).save(saved.capture());
		Notification created = saved.getAllValues().get(0);
		assertThat(created.getNotificationId()).isNull();
		assertThat(created.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(created.getCallId()).isEqualTo(CALL_ID);
		assertThat(created.getType()).isEqualTo("CALL");
		assertThat(created.getDeviceId()).isEqualTo(CHILD_DEVICE_ID);
		Notification last = saved.getAllValues().get(saved.getAllValues().size() - 1);
		assertThat(last.getNotificationId()).isEqualTo(NEW_ID);
		assertThat(last.getSentAt()).isEqualTo(NOW);

		ArgumentCaptor<PushMessage> pushed = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort).send(pushed.capture());
		assertThat(pushed.getValue().target()).isEqualTo(target);
		assertThat(pushed.getValue().title()).isEqualTo("이제 나와요");
		assertThat(pushed.getValue().data()).containsEntry("notificationId", String.valueOf(NEW_ID));
	}

	@Test
	@DisplayName("다시 보내기는 같은 알림의 마지막 발송 뒤 1분 안이면 RESEND_TOO_SOON과 retryAfterSeconds로 거절한다")
	void rejectsWithResendTooSoonWithinOneMinute() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:09:30Z"));
		givenOriginal(original, List.of(original));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(30);
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("다시 보내기는 같은 묶음의 가장 최근 sentAt을 마지막 발송으로 본다")
	void usesLatestSentAtOfSameGroupAsLastSend() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		Notification resent = row(RESENT_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:09:20Z"));
		givenOriginal(original, List.of(original, resent));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(20);
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("없는 알림을 다시 보내면 NOT_FOUND로 거절한다")
	void rejectsMissingNotificationWithNotFound() {
		when(loadNotificationPort.load(ORIGINAL_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code())
						.isEqualTo(ErrorCode.NOT_FOUND.name()));

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	private void givenOriginal(Notification original, List<Notification> sessionRows) {
		when(loadNotificationPort.load(ORIGINAL_ID)).thenReturn(Optional.of(original));
		lenient().when(loadNotificationPort.loadBySession(SESSION_ID)).thenReturn(sessionRows);
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

	private static Notification row(long id, String type, Long callId, long deviceId, Instant sentAt) {
		return Notification.restore(id, SESSION_ID, callId, type, deviceId, sentAt, null, null);
	}

	private static ResendCommand command() {
		AuthPrincipal principal = new AuthPrincipal(PrincipalType.USER, 11L, FAMILY_ID, Role.GUARDIAN);
		return new ResendCommand(principal, ORIGINAL_ID);
	}
}
