package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.RegisterVehicleUseCase;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterVehicleService implements RegisterVehicleUseCase {

	private final SaveVehiclePort saveVehiclePort;

	@Override
	@Transactional
	public long registerVehicle(String vin, String model) {
		return saveVehiclePort.saveVehicle(Vehicle.register(vin, model)).getVehicleId();
	}
}
