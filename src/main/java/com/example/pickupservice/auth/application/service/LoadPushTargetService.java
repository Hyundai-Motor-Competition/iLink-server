package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.LoadPushTargetByDeviceUseCase;
import com.example.pickupservice.auth.application.port.in.LoadPushTargetUseCase;
import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoadPushTargetService implements LoadPushTargetUseCase, LoadPushTargetByDeviceUseCase {

	private final LoadDevicePort loadDevicePort;

	@Override
	@Transactional(readOnly = true)
	public List<PushTarget> findPushTargets(List<Long> userIds) {
		return userIds.stream()
				.flatMap(userId -> loadDevicePort.loadLiveDevices(OwnerType.USER, userId).stream())
				.filter(Device::isLive)
				.filter(device -> device.getPushToken() != null && !device.getPushToken().isEmpty())
				.map(device -> new PushTarget(device.getOwnerId(), device.getDeviceId(), device.getPlatform(),
						device.getPushToken()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<PushTarget> findPushTargetByDeviceId(long deviceId) {
		return loadDevicePort.loadDevice(deviceId)
				.filter(device -> device.getOwnerType() == OwnerType.USER)
				.filter(device -> device.getPushToken() != null && !device.getPushToken().isEmpty())
				.map(device -> new PushTarget(device.getOwnerId(), device.getDeviceId(), device.getPlatform(),
						device.getPushToken()));
	}
}
