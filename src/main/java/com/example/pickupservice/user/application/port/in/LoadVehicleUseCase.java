package com.example.pickupservice.user.application.port.in;

import java.util.Optional;

public interface LoadVehicleUseCase {

	Optional<VehicleInfo> findVehicle(long vehicleId);

	Optional<VehicleInfo> findLinkedVehicleByVin(String vin);

}
