package com.example.pickupservice.notification.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.LoadPushTargetByDeviceUseCase;
import com.example.pickupservice.auth.application.port.in.LoadPushTargetUseCase;
import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationPushTargetAdapter implements LoadPushTargetPort {

	private final LoadPushTargetUseCase loadPushTargetUseCase;
	private final LoadPushTargetByDeviceUseCase loadPushTargetByDeviceUseCase;

	@Override
	public List<PushTarget> loadPushTargets(List<Long> userIds) {
		return loadPushTargetUseCase.findPushTargets(userIds);
	}

	@Override
	public Optional<PushTarget> loadPushTarget(long deviceId) {
		return loadPushTargetByDeviceUseCase.findPushTargetByDeviceId(deviceId);
	}
}
