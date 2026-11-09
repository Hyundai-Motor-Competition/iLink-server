package com.example.pickupservice.pickup.application.port.in;

public interface GetSessionUseCase {

	SessionView getSession(SessionQuery query);

}
