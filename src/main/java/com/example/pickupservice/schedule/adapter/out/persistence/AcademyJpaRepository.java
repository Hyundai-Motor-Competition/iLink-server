package com.example.pickupservice.schedule.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AcademyJpaRepository extends JpaRepository<AcademyEntity, Long> {

	Optional<AcademyEntity> findByExternalPlaceId(String externalPlaceId);
}
