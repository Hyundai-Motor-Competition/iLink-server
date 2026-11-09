package com.example.pickupservice.pickup.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface ProcessedVehicleEventJpaRepository extends JpaRepository<ProcessedVehicleEventEntity, String> {

	@Transactional
	@Modifying
	@Query(value = """
			INSERT INTO processed_vehicle_event (event_id, session_id, created_at, updated_at)
			VALUES (:eventId, :sessionId, now(), now())
			ON CONFLICT (event_id) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(@Param("eventId") String eventId, @Param("sessionId") long sessionId);

}
