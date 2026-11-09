package com.example.pickupservice.notification.application.port.out;

public interface SendPushPort {

	boolean send(PushMessage message);
}
