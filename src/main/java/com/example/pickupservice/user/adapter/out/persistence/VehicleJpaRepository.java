package com.example.pickupservice.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {

	Optional<VehicleEntity> findByVinAndUnlinkedAtIsNull(String vin);

	Optional<VehicleEntity> findByFamilyIdAndUnlinkedAtIsNull(Long familyId);

}
