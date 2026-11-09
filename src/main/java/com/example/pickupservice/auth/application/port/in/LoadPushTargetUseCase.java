package com.example.pickupservice.auth.application.port.in;

import java.util.List;

public interface LoadPushTargetUseCase {

	List<PushTarget> findPushTargets(List<Long> userIds);
}
