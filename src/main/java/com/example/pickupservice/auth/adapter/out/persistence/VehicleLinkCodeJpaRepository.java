package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.domain.LinkCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface VehicleLinkCodeJpaRepository extends JpaRepository<VehicleLinkCodeEntity, Long> {

	Optional<VehicleLinkCodeEntity> findFirstByCodeOrderByCodeIdDesc(String code);

	List<VehicleLinkCodeEntity> findByVehicleIdAndStatus(Long vehicleId, LinkCodeStatus status);

}
