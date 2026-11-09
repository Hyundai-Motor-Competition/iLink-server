package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyDeviceReplacedServiceTest {

	private static final String TITLE = "다른 기기에서 로그인했어요";
	private static final long DEVICE_A = 701L;
	private static final long DEVICE_B = 702L;
	private static final long USER_ID = 11L;

	@Mock
	private LoadPushTargetPort loadPushTargetPort;
	@Mock
	private SendPushPort sendPushPort;

	private NotifyDeviceReplacedService service;

	@BeforeEach
	void setUp() {
		service = new NotifyDeviceReplacedService(loadPushTargetPort, sendPushPort);
	}

	@Test
	@DisplayName("RELINK 이전 기기 이벤트를 받으면 이전 기기의 pushToken으로 「다른 기기에서 로그인했어요」를 보낸다")
	void sendsDeviceReplacedPushToPreviousDevicePushToken() {
		PushTarget target = target(DEVICE_A, "token-a");
		when(loadPushTargetPort.loadPushTarget(DEVICE_A)).thenReturn(Optional.of(target));

		service.notifyDeviceReplaced(List.of(DEVICE_A));

		ArgumentCaptor<PushMessage> sent = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort).send(sent.capture());
		assertThat(sent.getValue().target()).isEqualTo(target);
		assertThat(sent.getValue().target().pushToken()).isEqualTo("token-a");
		assertThat(sent.getValue().title()).isEqualTo(TITLE);
		assertThat(sent.getValue().body()).isNotBlank();
		assertThat(sent.getValue().data()).isNotNull();
	}

	@Test
	@DisplayName("RELINK 이전 기기가 여러 대면 기기마다 한 번씩 보낸다")
	void sendsOnePushPerPreviousDevice() {
		when(loadPushTargetPort.loadPushTarget(DEVICE_A)).thenReturn(Optional.of(target(DEVICE_A, "token-a")));
		when(loadPushTargetPort.loadPushTarget(DEVICE_B)).thenReturn(Optional.of(target(DEVICE_B, "token-b")));

		service.notifyDeviceReplaced(List.of(DEVICE_A, DEVICE_B));

		ArgumentCaptor<PushMessage> sent = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort, times(2)).send(sent.capture());
		assertThat(sent.getAllValues()).extracting(m -> m.target().pushToken())
				.containsExactly("token-a", "token-b");
		assertThat(sent.getAllValues()).extracting(PushMessage::title).containsOnly(TITLE);
	}

	@Test
	@DisplayName("RELINK 이전 기기의 푸시 대상을 못 찾으면 그 기기는 건너뛰고 나머지에는 보낸다")
	void skipsDeviceWithoutPushTargetAndSendsToTheRest() {
		when(loadPushTargetPort.loadPushTarget(DEVICE_A)).thenReturn(Optional.empty());
		when(loadPushTargetPort.loadPushTarget(DEVICE_B)).thenReturn(Optional.of(target(DEVICE_B, "token-b")));

		service.notifyDeviceReplaced(List.of(DEVICE_A, DEVICE_B));

		ArgumentCaptor<PushMessage> sent = ArgumentCaptor.forClass(PushMessage.class);
		verify(sendPushPort).send(sent.capture());
		assertThat(sent.getValue().target().deviceId()).isEqualTo(DEVICE_B);
	}

	@Test
	@DisplayName("RELINK 이전 기기 푸시가 한 대 실패해도 나머지 기기에는 계속 보낸다")
	void continuesWithNextDeviceWhenSendFails() {
		when(loadPushTargetPort.loadPushTarget(DEVICE_A)).thenReturn(Optional.of(target(DEVICE_A, "token-a")));
		when(loadPushTargetPort.loadPushTarget(DEVICE_B)).thenReturn(Optional.of(target(DEVICE_B, "token-b")));
		when(sendPushPort.send(any(PushMessage.class))).thenReturn(false, true);

		service.notifyDeviceReplaced(List.of(DEVICE_A, DEVICE_B));

		verify(sendPushPort, times(2)).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("RELINK 이전 기기 목록이 비어 있으면 아무것도 조회하거나 보내지 않는다")
	void doesNothingWhenNoPreviousDevices() {
		service.notifyDeviceReplaced(List.of());

		verifyNoInteractions(loadPushTargetPort, sendPushPort);
	}

	@Test
	@DisplayName("RELINK 이전 기기 푸시를 못 찾으면 보내지 않는다")
	void doesNotSendWhenNoDeviceHasPushTarget() {
		when(loadPushTargetPort.loadPushTarget(DEVICE_A)).thenReturn(Optional.empty());

		service.notifyDeviceReplaced(List.of(DEVICE_A));

		verify(loadPushTargetPort).loadPushTarget(DEVICE_A);
		verify(sendPushPort, never()).send(any(PushMessage.class));
	}

	@Test
	@DisplayName("RELINK 이전 기기 푸시는 Notification 행을 만들지 않는다(SaveNotificationPort에 의존하지 않는다)")
	void doesNotDependOnSaveNotificationPort() {
		boolean dependsOnSavePort = Arrays.stream(NotifyDeviceReplacedService.class.getDeclaredFields())
				.map(Field::getType)
				.anyMatch(SaveNotificationPort.class::isAssignableFrom);

		assertThat(dependsOnSavePort).isFalse();
	}

	private static PushTarget target(long deviceId, String pushToken) {
		return new PushTarget(USER_ID, deviceId, Platform.ANDROID, pushToken);
	}
}
