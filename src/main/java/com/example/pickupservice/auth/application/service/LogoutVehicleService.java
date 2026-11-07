package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.LogoutVehicleUseCase;
import com.example.pickupservice.auth.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class LogoutVehicleService implements LogoutVehicleUseCase {

	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final CheckActivePickupPort checkActivePickupPort;
	private final Clock clock;

	@Override
	@Transactional
	public void logoutVehicle(long deviceId) {
		Device device = loadDevicePort.loadDevice(deviceId)
				.filter(found -> found.getOwnerType() == OwnerType.VEHICLE)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		if (checkActivePickupPort.hasActivePickup(device.getOwnerId())) {
			throw AuthErrors.activePickupExists();
		}
		device.revoke(clock.instant());
		saveDevicePort.saveDevice(device);
	}
}
