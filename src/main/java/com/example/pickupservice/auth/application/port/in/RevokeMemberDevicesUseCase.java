package com.example.pickupservice.auth.application.port.in;

public interface RevokeMemberDevicesUseCase {

	void revokeDevicesOf(long userId);
}
