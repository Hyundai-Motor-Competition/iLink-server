package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.Vehicle;

import java.util.Optional;

public interface LoadVehiclePort {

	Optional<Vehicle> loadVehicle(long vehicleId);

	Optional<Vehicle> loadLinkedVehicleByVin(String vin);

	Optional<Vehicle> loadLinkedVehicleOfFamily(long familyId);
}
