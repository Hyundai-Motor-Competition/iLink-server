package com.example.pickupservice.user.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.RevokeVehicleDevicesUseCase;
import com.example.pickupservice.user.application.port.out.RevokeVehicleDevicesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AuthVehicleDeviceAdapter implements RevokeVehicleDevicesPort {

	private final RevokeVehicleDevicesUseCase revokeVehicleDevicesUseCase;

	@Override
	public void revokeDevicesOfVehicle(long vehicleId) {
		revokeVehicleDevicesUseCase.revokeDevicesOfVehicle(vehicleId);
	}
}
