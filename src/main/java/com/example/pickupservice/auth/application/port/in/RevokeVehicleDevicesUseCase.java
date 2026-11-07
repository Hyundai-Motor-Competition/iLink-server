package com.example.pickupservice.auth.application.port.in;

public interface RevokeVehicleDevicesUseCase {

	void revokeDevicesOfVehicle(long vehicleId);
}
