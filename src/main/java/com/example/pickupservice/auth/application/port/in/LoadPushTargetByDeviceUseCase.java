package com.example.pickupservice.auth.application.port.in;

import java.util.Optional;

public interface LoadPushTargetByDeviceUseCase {

	Optional<PushTarget> findPushTargetByDeviceId(long deviceId);
}
