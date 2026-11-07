package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.UpdatePushTokenCommand;
import com.example.pickupservice.auth.application.port.in.UpdatePushTokenUseCase;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UpdatePushTokenService implements UpdatePushTokenUseCase {

	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final Clock clock;

	@Override
	@Transactional
	public void updatePushToken(UpdatePushTokenCommand command) {
		Device device = loadDevicePort.loadDevice(command.deviceId())
				.filter(Device::isLive)
				.orElseThrow(() -> BusinessException.of(ErrorCode.UNAUTHORIZED));
		device.updatePushToken(command.pushToken(), clock.instant());
		saveDevicePort.saveDevice(device);
	}
}
