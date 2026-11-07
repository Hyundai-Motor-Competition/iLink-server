package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.RevokeVehicleDevicesUseCase;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RevokeVehicleDevicesService implements RevokeVehicleDevicesUseCase {

	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final Clock clock;

	@Override
	@Transactional
	public void revokeDevicesOfVehicle(long vehicleId) {
		Instant now = clock.instant();
		for (Device device : loadDevicePort.loadLiveDevices(OwnerType.VEHICLE, vehicleId)) {
			device.revoke(now);
			saveDevicePort.saveDevice(device);
		}
	}
}
