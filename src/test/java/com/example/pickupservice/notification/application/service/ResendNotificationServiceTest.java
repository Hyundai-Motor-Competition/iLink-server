package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.notification.application.port.in.ResendCommand;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.LoadResendGuardPort;
import com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort;
import org.springframework.http.HttpStatus;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

	@Mock
	private CallProgressPort callProgressPort;

	@Mock
	private LoadResendGuardPort loadResendGuardPort;

	private ResendNotificationService service;

	@BeforeEach
	void setUp() {
		service = new ResendNotificationService(loadNotificationPort, saveNotificationPort, sendPushPort,
				loadPushTargetPort, loadSessionFamilyPort, callProgressPort, loadResendGuardPort,
				Clock.fixed(NOW, ZoneOffset.UTC));
		lenient().when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.of(FAMILY_ID));
	}

	// ── #54 CALL 발송 성공 시 markCallSent ──

	@Test
	@DisplayName("다시 보내기는 CALL 알림을 보내는 데 성공하면 callProgressPort.markCallSent(callId, 현재 시각)을 부른다")
	void marksCallSentWhenCallResendSucceeds() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(
				new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		service.resend(command());

		verify(callProgressPort, times(1)).markCallSent(CALL_ID, NOW);
	}

	@Test
	@DisplayName("다시 보내기는 CALL 알림 발송이 실패하면 markCallSent를 부르지 않는다")
	void doesNotMarkCallSentWhenCallResendFails() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(
				new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(false);

		service.resend(command());

		verify(callProgressPort, never()).markCallSent(anyLong(), any());
	}

	@Test
	@DisplayName("다시 보내기는 CALL이 아닌 알림을 보내는 데 성공해도 markCallSent를 부르지 않는다")
	void doesNotMarkCallSentForNonCallNotification() {
		Notification original = row(ORIGINAL_ID, "DEPARTURE", null, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(
				new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		service.resend(command());

		verify(callProgressPort, never()).markCallSent(anyLong(), any());
	}

	// ── #59 sentAt이 하나도 없으면 가장 최근 행의 createdAt으로 1분 제한 ──

	@Test
	@DisplayName("다시 보내기는 같은 묶음에 sentAt이 하나도 없으면 가장 최근 행의 createdAt을 마지막 시도로 보고 1분 안이면 RESEND_TOO_SOON으로 거절한다")
	void rejectsWithResendTooSoonByCreatedAtWhenNoRowWasEverSent() {
		Notification original = rowCreated(ORIGINAL_ID, null, Instant.parse("2026-11-09T06:00:00Z"));
		Notification latest = rowCreated(RESENT_ID, null, Instant.parse("2026-11-09T06:09:40Z"));
		givenOriginal(original, List.of(original, latest));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(40);
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("다시 보내기는 sentAt이 하나도 없어도 가장 최근 행의 createdAt이 1분 넘게 지났으면 다시 보내고, 다른 묶음의 행은 판단에 섞지 않는다")
	void resendsWhenNoRowWasSentButLatestCreatedAtIsOlderThanOneMinute() {
		Notification original = rowCreated(ORIGINAL_ID, null, Instant.parse("2026-11-09T06:05:00Z"));
		Notification otherGroup = Notification.restore(OTHER_GROUP_ID, SESSION_ID, CALL_ID, "CALL", OTHER_DEVICE_ID, null,
				null, null, Instant.parse("2026-11-09T06:09:55Z"));
		givenOriginal(original, List.of(original, otherGroup));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(
				new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		long newId = service.resend(command());

		assertThat(newId).isEqualTo(NEW_ID);
		verify(sendPushPort).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("다시 보내기는 묶음 마지막 행에 sentAt이 없으면 앞 행에 sentAt이 있어도 그 마지막 행의 createdAt을 기준으로 1분 안이면 RESEND_TOO_SOON으로 거절한다")
	void usesCreatedAtOfLastRowEvenWhenEarlierRowWasSent() {
		Notification sentRow = Notification.restore(ORIGINAL_ID, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID,
				Instant.parse("2026-11-09T06:00:00Z"), null, null, Instant.parse("2026-11-09T05:59:50Z"));
		Notification unsentRecent = rowCreated(RESENT_ID, null, Instant.parse("2026-11-09T06:09:50Z"));
		givenOriginal(sentRow, List.of(sentRow, unsentRecent));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(50);
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
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
	@DisplayName("다시 보내기는 다시 보낼 수 있는 시각까지 0.5초 남았으면 RESEND_TOO_SOON으로 거절하고 retryAfterSeconds 1을 담는다")
	void rejectsWithRetryAfterOneWhenHalfSecondLeft() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:09:00.500Z"));
		givenOriginal(original, List.of(original));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(1);
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("다시 보내기는 다시 보낼 수 있는 시각까지 29.5초 남았으면 retryAfterSeconds를 30으로 올려 담는다")
	void roundsUpRetryAfterWhenFractionalSecondsLeft() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:09:29.500Z"));
		givenOriginal(original, List.of(original));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(30);
				});
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

	@Test
	@DisplayName("다시 보내기는 폐기된 기기(살아 있는 기기 조회가 빈 값)에는 FCM을 보내지 않고 sentAt도 채우지 않으며 폐기 포함 조회를 쓰지 않는다")
	void resendDoesNotPushToRevokedDevice() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.empty());

		service.resend(command());

		verify(sendPushPort, never()).send(any());
		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort, atLeastOnce()).save(saved.capture());
		assertThat(saved.getAllValues()).allSatisfy(n -> assertThat(n.getSentAt()).isNull());
		verify(loadPushTargetPort, never()).loadPushTargetIncludingRevoked(anyLong());
	}

	@Test
	@DisplayName("다시 보내기는 세션이 끝났으면 409로 거절하고 새 행도 FCM도 만들지 않는다")
	void rejectsWith409WhenSessionEnded() {
		Notification original = row(ORIGINAL_ID, "DEPARTURE", null, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		when(loadResendGuardPort.isSessionEnded(SESSION_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("SESSION_ALREADY_FINISHED");
					assertThat(e.getMessage()).isEqualTo("이미 끝난 픽업입니다");
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
		verify(callProgressPort, never()).markCallSent(anyLong(), any());
	}

	@Test
	@DisplayName("다시 보내기는 callId가 있는 알림의 호출이 철회됐으면 409로 거절하고 새 행도 FCM도 만들지 않는다")
	void rejectsWith409WhenCallRevoked() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		when(loadResendGuardPort.isCallRevoked(CALL_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("CALL_ALREADY_REVOKED");
					assertThat(e.getMessage()).isEqualTo("이미 철회된 합류 호출입니다");
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
		verify(callProgressPort, never()).markCallSent(anyLong(), any());
	}

	@Test
	@DisplayName("다시 보내기는 callId가 없는 알림이면 호출 철회를 묻지 않고 세션이 끝나지 않았으면 그대로 다시 보낸다")
	void resendsNotificationWithoutCallIdWithoutAskingCallRevoked() {
		Notification original = row(ORIGINAL_ID, "DEPARTURE", null, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(
				new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		long newId = service.resend(command());

		assertThat(newId).isEqualTo(NEW_ID);
		verify(loadResendGuardPort, never()).isCallRevoked(anyLong());
	}

	@Test
	@DisplayName("다시 보내기는 다른 가족의 알림이면 세션 종료 여부와 상관없이 NOT_FOUND를 먼저 돌려준다")
	void checksFamilyBeforeSessionEnded() {
		Notification original = row(ORIGINAL_ID, "CALL", CALL_ID, CHILD_DEVICE_ID, Instant.parse("2026-11-09T06:00:00Z"));
		givenOriginal(original, List.of(original));
		when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.of(OTHER_FAMILY_ID));
		lenient().when(loadResendGuardPort.isSessionEnded(SESSION_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND.name()));
	}

	@Test
	@DisplayName("다시 보내기의 1분 제한은 묶음 마지막 행에 sentAt이 없으면 옛 행의 sentAt이 아니라 그 행의 createdAt 기준이다")
	void limitsByCreatedAtOfLastRowWhenLastRowHasNoSentAt() {
		Notification oldSent = Notification.restore(ORIGINAL_ID, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID,
				Instant.parse("2026-11-09T06:05:00Z"), null, null, Instant.parse("2026-11-09T06:04:59Z"));
		Notification lastUnsent = rowCreated(RESENT_ID, null, Instant.parse("2026-11-09T06:09:40Z"));
		givenOriginal(oldSent, List.of(oldSent, lastUnsent));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(40);
				});

		verify(saveNotificationPort, never()).save(any());
		verify(sendPushPort, never()).send(any());
	}

	@Test
	@DisplayName("다시 보내기의 1분 제한은 묶음 마지막 행에 sentAt이 있으면 그 행의 createdAt이 아니라 sentAt 기준이다")
	void limitsBySentAtOfLastRowWhenLastRowHasSentAt() {
		Notification first = rowCreated(ORIGINAL_ID, null, Instant.parse("2026-11-09T05:50:00Z"));
		Notification lastSent = Notification.restore(RESENT_ID, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID,
				Instant.parse("2026-11-09T06:08:00Z"), null, null, Instant.parse("2026-11-09T06:09:50Z"));
		givenOriginal(first, List.of(first, lastSent));
		givenSaveAssignsNewId();
		when(loadPushTargetPort.loadPushTarget(CHILD_DEVICE_ID)).thenReturn(Optional.of(
				new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "fcm-token")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(true);

		long newId = service.resend(command());

		assertThat(newId).isEqualTo(NEW_ID);
		verify(sendPushPort).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("다시 보내기의 마지막 행은 요청한 행이 아니라 묶음에서 notificationId가 가장 큰 행이다")
	void usesRowWithLargestNotificationIdEvenWhenOriginalIsOlder() {
		Notification original = Notification.restore(ORIGINAL_ID, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID,
				Instant.parse("2026-11-09T06:09:30Z"), null, null, Instant.parse("2026-11-09T06:09:30Z"));
		Notification last = Notification.restore(RESENT_ID, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID,
				Instant.parse("2026-11-09T06:09:40Z"), null, null, Instant.parse("2026-11-09T06:09:20Z"));
		givenOriginal(original, List.of(last, original));

		assertThatThrownBy(() -> service.resend(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo(ErrorCode.RESEND_TOO_SOON.name());
					assertThat(e.retryAfterSeconds()).isEqualTo(40);
				});
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

	private static Notification rowCreated(long id, Instant sentAt, Instant createdAt) {
		return Notification.restore(id, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID, sentAt, null, null, createdAt);
	}

	private static ResendCommand command() {
		AuthPrincipal principal = new AuthPrincipal(PrincipalType.USER, 11L, FAMILY_ID, Role.GUARDIAN);
		return new ResendCommand(principal, ORIGINAL_ID);
	}
}
