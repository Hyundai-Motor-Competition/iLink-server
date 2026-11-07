package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CreateGuardianCommand;
import com.example.pickupservice.user.application.port.in.CreateGuardianUseCase;
import com.example.pickupservice.user.application.port.in.GuardianAccount;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateGuardianService implements CreateGuardianUseCase {

	private final SaveFamilyPort saveFamilyPort;
	private final SaveUserPort saveUserPort;
	private final LoadVehiclePort loadVehiclePort;
	private final SaveVehiclePort saveVehiclePort;

	@Override
	@Transactional
	public GuardianAccount createGuardian(CreateGuardianCommand command) {
		Vehicle vehicle = loadVehiclePort.loadVehicle(command.vehicleId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		Family family = saveFamilyPort.saveFamily(Family.create());
		long familyId = family.getFamilyId();
		User guardian = saveUserPort.saveUser(User.create(familyId, Role.GUARDIAN, command.name(), command.phone()));
		vehicle.linkTo(familyId);
		saveVehiclePort.saveVehicle(vehicle);
		return new GuardianAccount(familyId, guardian.getUserId());
	}
}
