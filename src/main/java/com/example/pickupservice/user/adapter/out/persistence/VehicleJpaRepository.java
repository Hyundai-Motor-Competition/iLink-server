package com.example.pickupservice.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {

	@Query("select v.version from VehicleEntity v where v.vehicleId = :vehicleId")
	Optional<Long> findVersionByVehicleId(Long vehicleId);

	Optional<VehicleEntity> findByVinAndUnlinkedAtIsNull(String vin);

	Optional<VehicleEntity> findByFamilyIdAndUnlinkedAtIsNull(Long familyId);

}
