package com.example.pickupservice.pickup.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SessionChildJpaRepository extends JpaRepository<SessionChildEntity, Long> {

	List<SessionChildEntity> findBySessionIdOrderBySessionChildId(Long sessionId);

}
