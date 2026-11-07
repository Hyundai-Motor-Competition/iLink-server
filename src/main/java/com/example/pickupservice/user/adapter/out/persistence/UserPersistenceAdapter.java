package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.application.port.out.SaveFamilyPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements LoadFamilyPort, SaveFamilyPort, LoadUserPort, SaveUserPort,
		LoadVehiclePort, SaveVehiclePort {

	private final FamilyJpaRepository familyJpaRepository;
	private final UserJpaRepository userJpaRepository;
	private final VehicleJpaRepository vehicleJpaRepository;

	@Override
	public Optional<Family> loadFamily(long familyId) {
		return familyJpaRepository.findById(familyId).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public Family saveFamily(Family family) {
		return toDomain(familyJpaRepository.save(toEntity(family)));
	}

	@Override
	public Optional<User> loadUser(long userId) {
		return userJpaRepository.findById(userId).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public List<User> loadUsers(long familyId) {
		return userJpaRepository.findByFamilyId(familyId).stream().map(UserPersistenceAdapter::toDomain).toList();
	}

	@Override
	public long countNonChildMembers(long familyId) {
		return userJpaRepository.countByFamilyIdAndRoleNot(familyId, Role.CHILD);
	}

	@Override
	public User saveUser(User user) {
		return toDomain(userJpaRepository.save(toEntity(user)));
	}

	@Override
	public void deleteUser(long userId) {
		userJpaRepository.deleteById(userId);
	}

	@Override
	public Optional<Vehicle> loadVehicle(long vehicleId) {
		return vehicleJpaRepository.findById(vehicleId).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<Vehicle> loadLinkedVehicleByVin(String vin) {
		return vehicleJpaRepository.findByVinAndUnlinkedAtIsNull(vin).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<Vehicle> loadLinkedVehicleOfFamily(long familyId) {
		return vehicleJpaRepository.findByFamilyIdAndUnlinkedAtIsNull(familyId).map(UserPersistenceAdapter::toDomain);
	}

	@Override
	public Vehicle saveVehicle(Vehicle vehicle) {
		return toDomain(vehicleJpaRepository.saveAndFlush(toEntity(vehicle, versionToSave(vehicle))));
	}

	private Long versionToSave(Vehicle vehicle) {
		if (vehicle.getVersion() != null || vehicle.getVehicleId() == null) {
			return vehicle.getVersion();
		}
		return vehicleJpaRepository.findVersionByVehicleId(vehicle.getVehicleId()).orElse(null);
	}

	private static FamilyEntity toEntity(Family family) {
		return new FamilyEntity(family.getFamilyId(), family.getJoinCallMode(), family.getSettingsVersion());
	}

	private static UserEntity toEntity(User user) {
		return new UserEntity(user.getUserId(), user.getFamilyId(), user.getRole(), user.getName(), user.getPhone());
	}

	private static VehicleEntity toEntity(Vehicle vehicle, Long version) {
		return new VehicleEntity(vehicle.getVehicleId(), vehicle.getFamilyId(), vehicle.getVin(), vehicle.getModel(),
				vehicle.getPlateNumber(), vehicle.getUnlinkedAt(), version);
	}

	private static Family toDomain(FamilyEntity entity) {
		return Family.restore(entity.getFamilyId(), entity.getJoinCallMode(), entity.getSettingsVersion());
	}

	private static User toDomain(UserEntity entity) {
		return User.restore(entity.getUserId(), entity.getFamilyId(), entity.getRole(), entity.getName(),
				entity.getPhone());
	}

	private static Vehicle toDomain(VehicleEntity entity) {
		return Vehicle.restore(entity.getVehicleId(), entity.getFamilyId(), entity.getVin(), entity.getModel(),
				entity.getPlateNumber(), entity.getUnlinkedAt(), entity.getVersion());
	}

}
