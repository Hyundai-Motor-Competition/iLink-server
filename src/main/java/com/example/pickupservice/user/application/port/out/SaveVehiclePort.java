package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.Vehicle;

public interface SaveVehiclePort {

	Vehicle saveVehicle(Vehicle vehicle);
}
