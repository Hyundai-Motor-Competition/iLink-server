package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.notification.application.port.in.NotificationRequest;
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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordNotificationServiceTest {

	private static final long SESSION_ID = 501L;
	private static final long CALL_ID = 3001L;
	private static final String TYPE = "CALL";
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long GUARDIAN_PHONE_ID = 901L;
	private static final long GUARDIAN_TABLET_ID = 902L;
	private static final long CO_GUARDIAN_PHONE_ID = 903L;
	private static final long FIRST_NOTIFICATION_ID = 7001L;

	@Mock
	private LoadPushTargetPort loadPushTargetPort;
	@Mock
	private SaveNotificationPort saveNotificationPort;

	private RecordNotificationService service;
	private AtomicLong idSequence;

	@BeforeEach
	void setUp() {
		service = new RecordNotificationService(loadPushTargetPort, saveNotificationPort);
		idSequence = new AtomicLong(FIRST_NOTIFICATION_ID);
	}

	@Test
	@DisplayName("행 기록은 받는 사람의 푸시 대상 기기마다 Notification 행을 하나씩 만든다")
	void recordCreatesOneNotificationPerPushTargetDevice() {
		List<Long> receivers = List.of(GUARDIAN_ID, CO_GUARDIAN_ID);
		when(loadPushTargetPort.loadPushTargets(receivers)).thenReturn(List.of(
				target(GUARDIAN_ID, GUARDIAN_PHONE_ID, "fcm-guardian-phone"),
				target(GUARDIAN_ID, GUARDIAN_TABLET_ID, "fcm-guardian-tablet"),
				target(CO_GUARDIAN_ID, CO_GUARDIAN_PHONE_ID, "fcm-co-guardian-phone")));
		stubSaveAssigningIds();

		List<Long> ids = service.record(new NotificationRequest(SESSION_ID, CALL_ID, TYPE, receivers));

		List<Notification> saved = capturedSaves(3);
		assertThat(saved).extracting(Notification::getDeviceId)
				.containsExactly(GUARDIAN_PHONE_ID, GUARDIAN_TABLET_ID, CO_GUARDIAN_PHONE_ID);
		assertThat(saved).allSatisfy(notification -> {
			assertThat(notification.getSessionId()).isEqualTo(SESSION_ID);
			assertThat(notification.getCallId()).isEqualTo(CALL_ID);
			assertThat(notification.getType()).isEqualTo(TYPE);
			assertThat(notification.getSentAt()).isNull();
			assertThat(notification.getDeliveredAt()).isNull();
			assertThat(notification.getOpenedAt()).isNull();
		});
		assertThat(ids).containsExactly(FIRST_NOTIFICATION_ID, FIRST_NOTIFICATION_ID + 1, FIRST_NOTIFICATION_ID + 2);
	}

	@Test
	@DisplayName("행 기록은 pushToken이 없거나 폐기된 기기에는 행을 만들지 않는다")
	void recordSkipsDevicesWithoutPushToken() {
		List<Long> receivers = List.of(GUARDIAN_ID, CO_GUARDIAN_ID);
		when(loadPushTargetPort.loadPushTargets(receivers)).thenReturn(List.of(
				target(GUARDIAN_ID, GUARDIAN_PHONE_ID, null),
				target(GUARDIAN_ID, GUARDIAN_TABLET_ID, ""),
				target(CO_GUARDIAN_ID, CO_GUARDIAN_PHONE_ID, "fcm-co-guardian-phone")));
		stubSaveAssigningIds();

		List<Long> ids = service.record(new NotificationRequest(SESSION_ID, CALL_ID, TYPE, receivers));

		List<Notification> saved = capturedSaves(1);
		assertThat(saved).extracting(Notification::getDeviceId).containsExactly(CO_GUARDIAN_PHONE_ID);
		assertThat(ids).containsExactly(FIRST_NOTIFICATION_ID);
	}

	@Test
	@DisplayName("행 기록은 받는 사람의 기기가 모두 푸시 대상이 아니면 행을 하나도 저장하지 않고 빈 목록을 돌려준다")
	void recordSavesNothingWhenNoUsablePushTarget() {
		List<Long> receivers = List.of(GUARDIAN_ID);
		when(loadPushTargetPort.loadPushTargets(receivers)).thenReturn(List.of(
				target(GUARDIAN_ID, GUARDIAN_PHONE_ID, null),
				target(GUARDIAN_ID, GUARDIAN_TABLET_ID, "")));

		List<Long> ids = service.record(new NotificationRequest(SESSION_ID, CALL_ID, TYPE, receivers));

		assertThat(ids).isEmpty();
		verify(saveNotificationPort, never()).save(any());
	}

	@Test
	@DisplayName("행 기록은 부른 쪽 트랜잭션에 참여한다: @Transactional(REQUIRED)이고 읽기 전용이 아니다")
	void recordJoinsCallerTransaction() throws NoSuchMethodException {
		Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(
				RecordNotificationService.class.getMethod("record", NotificationRequest.class), Transactional.class);
		if (transactional == null) {
			transactional = AnnotatedElementUtils.findMergedAnnotation(RecordNotificationService.class,
					Transactional.class);
		}

		assertThat(transactional).isNotNull();
		assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRED);
		assertThat(transactional.readOnly()).isFalse();
	}

	@Test
	@DisplayName("행 기록 중 저장이 실패하면 예외를 삼키지 않고 그대로 던져 부른 쪽 트랜잭션이 롤백되게 한다")
	void recordPropagatesSaveFailureSoCallerTransactionRollsBack() {
		List<Long> receivers = List.of(GUARDIAN_ID);
		when(loadPushTargetPort.loadPushTargets(receivers)).thenReturn(List.of(
				target(GUARDIAN_ID, GUARDIAN_PHONE_ID, "fcm-guardian-phone"),
				target(GUARDIAN_ID, GUARDIAN_TABLET_ID, "fcm-guardian-tablet")));
		IllegalStateException failure = new IllegalStateException("db down");
		when(saveNotificationPort.save(any(Notification.class))).thenThrow(failure);

		assertThatThrownBy(() -> service.record(new NotificationRequest(SESSION_ID, CALL_ID, TYPE, receivers)))
				.isSameAs(failure);

		verify(saveNotificationPort, times(1)).save(any(Notification.class));
	}

	private void stubSaveAssigningIds() {
		when(saveNotificationPort.save(any(Notification.class))).thenAnswer(invocation -> {
			Notification n = invocation.getArgument(0);
			return Notification.restore(idSequence.getAndIncrement(), n.getSessionId(), n.getCallId(), n.getType(),
					n.getDeviceId(), n.getSentAt(), n.getDeliveredAt(), n.getOpenedAt());
		});
	}

	private List<Notification> capturedSaves(int expectedCount) {
		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(saveNotificationPort, times(expectedCount)).save(captor.capture());
		return new ArrayList<>(captor.getAllValues());
	}

	private static PushTarget target(long userId, long deviceId, String pushToken) {
		return new PushTarget(userId, deviceId, Platform.ANDROID, pushToken);
	}
}
