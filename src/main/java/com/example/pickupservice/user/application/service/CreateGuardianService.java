package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CreateGuardianCommand;
import com.example.pickupservice.user.application.port.in.CreateGuardianUseCase;
import com.example.pickupservice.user.application.port.in.GuardianAccount;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateGuardianService implements CreateGuardianUseCase {

	private final SaveFamilyPort saveFamilyPort;
	private final SaveUserPort saveUserPort;
	private final LoadVehiclePort loadVehiclePort;
	private final SaveVehiclePort saveVehiclePort;
	private final ProfileImageStoragePort profileImageStoragePort;

	@Override
	@Transactional
	public GuardianAccount createGuardian(CreateGuardianCommand command) {
		Vehicle vehicle = loadVehiclePort.loadVehicle(command.vehicleId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		if (vehicle.getFamilyId() != null || vehicle.getUnlinkedAt() != null) {
			throw BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED);
		}
		Family family = saveFamilyPort.saveFamily(Family.create());
		long familyId = family.getFamilyId();
		User newGuardian = User.create(familyId, Role.GUARDIAN, command.name(), command.phone());
		ProfileImage image = command.profileImage();
		if (image != null) {
			newGuardian.changeProfileImageKey(ProfileImageUploads.upload(profileImageStoragePort, image));
		}
		User guardian = saveUserPort.saveUser(newGuardian);
		vehicle.linkTo(familyId);
		try {
			saveVehiclePort.saveVehicle(vehicle);
		} catch (OptimisticLockingFailureException e) {
			throw BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED);
		}
		return new GuardianAccount(familyId, guardian.getUserId());
	}
}
