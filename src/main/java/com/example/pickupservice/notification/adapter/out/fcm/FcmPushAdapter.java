package com.example.pickupservice.notification.adapter.out.fcm;

import com.example.pickupservice.notification.application.port.out.PushMessage;
import com.example.pickupservice.notification.application.port.out.SendPushPort;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FcmPushAdapter implements SendPushPort {

	private final FirebaseMessaging firebaseMessaging;

	public FcmPushAdapter(@Nullable FirebaseMessaging firebaseMessaging) {
		this.firebaseMessaging = firebaseMessaging;
	}

	@Override
	public boolean send(PushMessage message) {
		if (firebaseMessaging == null) {
			log.warn("FCM 자격 증명이 설정되지 않아 푸시를 보내지 않는다. userId={}, deviceId={}",
					message.target().userId(), message.target().deviceId());
			return false;
		}
		try {
			Message.Builder builder = Message.builder().setToken(message.target().pushToken());
			if (message.data() != null) {
				builder.putAllData(message.data());
			}
			if (message.title() != null || message.body() != null) {
				builder.setNotification(Notification.builder()
						.setTitle(message.title())
						.setBody(message.body())
						.build());
			}
			firebaseMessaging.send(builder.build());
			return true;
		} catch (Exception e) {
			log.warn("FCM 푸시 전송에 실패했다. userId={}, deviceId={}, reason={}",
					message.target().userId(), message.target().deviceId(), e.getMessage());
			return false;
		}
	}
}
