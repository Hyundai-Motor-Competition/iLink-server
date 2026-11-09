package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.domain.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CallJpaRepository extends JpaRepository<CallEntity, Long> {

	Optional<CallEntity> findFirstBySessionIdAndStatusNotOrderByCallIdDesc(Long sessionId, CallStatus status);

	Optional<CallEntity> findFirstBySessionIdOrderByCallIdDesc(Long sessionId);

}
