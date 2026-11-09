package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.util.Optional;

public interface FindActiveSessionUseCase {

	Optional<ActiveSessionResult> findActive(AuthPrincipal principal);

}
