package com.example.pickupservice.notification.application.port.out;

import com.example.pickupservice.auth.application.port.in.PushTarget;

import java.util.List;
import java.util.Optional;

public interface LoadPushTargetPort {

	List<PushTarget> loadPushTargets(List<Long> userIds);

	Optional<PushTarget> loadPushTarget(long deviceId);

	Optional<PushTarget> loadPushTargetIncludingRevoked(long deviceId);
}
