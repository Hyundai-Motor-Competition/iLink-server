package com.example.pickupservice.notification.adapter.out.fcm;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FcmPushAdapterTest {

	private static final PushTarget TARGET = new PushTarget(1L, 10L, Platform.ANDROID, "push-token-abc");

	private final FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
	private final FcmPushAdapter adapter = new FcmPushAdapter(firebaseMessaging);

	@Test
	@DisplayName("FCM 전송이 성공하면 true를 돌려준다")
	void returnsTrueWhenFcmSendSucceeds() throws Exception {
		when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/p/messages/1");

		boolean sent = adapter.send(new PushMessage(TARGET, "제목", "본문", Map.of("sessionId", "5")));

		assertThat(sent).isTrue();
		verify(firebaseMessaging).send(any(Message.class));
	}

	@Test
	@DisplayName("FCM 전송이 예외를 던지면 예외를 밖으로 내지 않고 false를 돌려준다")
	void returnsFalseWhenFcmSendThrows() throws Exception {
		when(firebaseMessaging.send(any(Message.class))).thenThrow(new IllegalStateException("fcm down"));

		boolean sent = adapter.send(new PushMessage(TARGET, "제목", "본문", Map.of()));

		assertThat(sent).isFalse();
	}

	@Test
	@DisplayName("PushMessage의 푸시 토큰 · 제목 · 본문 · data를 FCM Message로 옮긴다")
	void mapsPushMessageToFcmMessage() throws Exception {
		when(firebaseMessaging.send(any(Message.class))).thenReturn("id");

		adapter.send(new PushMessage(TARGET, "제목", "본문", Map.of("sessionId", "5", "type", "CALL")));

		Message sent = capturedMessage();
		assertThat(ReflectionTestUtils.getField(sent, "token")).isEqualTo("push-token-abc");
		Object notification = ReflectionTestUtils.getField(sent, "notification");
		assertThat(notification).isNotNull();
		assertThat(ReflectionTestUtils.getField(notification, "title")).isEqualTo("제목");
		assertThat(ReflectionTestUtils.getField(notification, "body")).isEqualTo("본문");
		assertThat(ReflectionTestUtils.getField(sent, "data")).isEqualTo(Map.of("sessionId", "5", "type", "CALL"));
	}

	@Test
	@DisplayName("제목과 본문이 둘 다 null이면 notification 없이 data 메시지만 보낸다")
	void sendsDataOnlyMessageWhenTitleAndBodyAreNull() throws Exception {
		when(firebaseMessaging.send(any(Message.class))).thenReturn("id");

		boolean sent = adapter.send(new PushMessage(TARGET, null, null, Map.of("type", "DEVICE_REPLACED")));

		assertThat(sent).isTrue();
		Message message = capturedMessage();
		assertThat(ReflectionTestUtils.getField(message, "notification")).isNull();
		assertThat(ReflectionTestUtils.getField(message, "token")).isEqualTo("push-token-abc");
		assertThat(ReflectionTestUtils.getField(message, "data")).isEqualTo(Map.of("type", "DEVICE_REPLACED"));
	}

	@Test
	@DisplayName("data가 null이어도 예외 없이 전송한다")
	void sendsWhenDataIsNull() throws Exception {
		when(firebaseMessaging.send(any(Message.class))).thenReturn("id");

		boolean sent = adapter.send(new PushMessage(TARGET, "제목", "본문", null));

		assertThat(sent).isTrue();
	}

	@Test
	@DisplayName("FirebaseMessaging이 없으면(자격 증명 미설정) 전송하지 않고 false를 돌려준다")
	void returnsFalseWhenFirebaseMessagingIsNotConfigured() {
		FcmPushAdapter unconfigured = new FcmPushAdapter(null);

		boolean sent = unconfigured.send(new PushMessage(TARGET, "제목", "본문", Map.of()));

		assertThat(sent).isFalse();
		verifyNoInteractions(firebaseMessaging);
	}

	private Message capturedMessage() throws Exception {
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(firebaseMessaging).send(captor.capture());
		return captor.getValue();
	}
}
