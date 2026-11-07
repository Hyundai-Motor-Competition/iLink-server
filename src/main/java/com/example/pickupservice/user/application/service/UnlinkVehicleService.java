package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleCommand;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleUseCase;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UnlinkVehicleService implements UnlinkVehicleUseCase {

	private final LoadVehiclePort loadVehiclePort;
	private final SaveVehiclePort saveVehiclePort;
	private final CheckActivePickupPort checkActivePickupPort;
	private final Clock clock;

	@Override
	@Transactional
	public void unlinkVehicle(UnlinkVehicleCommand command) {
		Vehicle vehicle = loadVehiclePort.loadVehicle(command.vehicleId())
				.filter(found -> Objects.equals(found.getFamilyId(), command.familyId()))
				.filter(Vehicle::isLinked)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		if (checkActivePickupPort.hasActivePickup(command.vehicleId())) {
			throw UserErrors.activePickupExists();
		}
		vehicle.unlink(Instant.now(clock));
		saveVehiclePort.saveVehicle(vehicle);
	}
}
