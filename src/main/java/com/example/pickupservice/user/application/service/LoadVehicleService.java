package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadVehicleService implements LoadVehicleUseCase {

	private final LoadVehiclePort loadVehiclePort;

	@Override
	public Optional<VehicleInfo> findVehicle(long vehicleId) {
		return loadVehiclePort.loadVehicle(vehicleId).map(LoadVehicleService::toInfo);
	}

	@Override
	public Optional<VehicleInfo> findLinkedVehicleByVin(String vin) {
		return loadVehiclePort.loadLinkedVehicleByVin(vin).map(LoadVehicleService::toInfo);
	}

	private static VehicleInfo toInfo(Vehicle vehicle) {
		return new VehicleInfo(vehicle.getVehicleId(), vehicle.getFamilyId(), vehicle.getVin(),
				vehicle.getModel(), vehicle.getPlateNumber(), vehicle.isLinked());
	}
}
