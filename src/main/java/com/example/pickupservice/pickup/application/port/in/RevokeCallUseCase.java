package com.example.pickupservice.pickup.application.port.in;

public interface RevokeCallUseCase {

	SessionView revoke(RevokeCallCommand command);

}
