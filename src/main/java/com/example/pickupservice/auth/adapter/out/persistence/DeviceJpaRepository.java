package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.domain.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface DeviceJpaRepository extends JpaRepository<DeviceEntity, Long> {

	Optional<DeviceEntity> findByRefreshTokenHash(String refreshTokenHash);

	List<DeviceEntity> findByOwnerTypeAndOwnerIdAndRevokedAtIsNull(OwnerType ownerType, Long ownerId);

}
