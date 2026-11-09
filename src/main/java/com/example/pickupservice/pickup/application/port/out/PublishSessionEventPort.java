package com.example.pickupservice.pickup.application.port.out;

public interface PublishSessionEventPort {

	void publish(SessionEventMessage message);

}
