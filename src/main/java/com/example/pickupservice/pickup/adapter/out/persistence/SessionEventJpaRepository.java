package com.example.pickupservice.pickup.adapter.out.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SessionEventJpaRepository extends JpaRepository<SessionEventEntity, Long> {

	Optional<SessionEventEntity> findFirstBySessionIdOrderBySeqDesc(Long sessionId);

	List<SessionEventEntity> findBySessionIdAndSeqGreaterThanOrderBySeqAsc(Long sessionId, Long afterSeq, Limit limit);

	List<SessionEventEntity> findBySessionIdAndTypeOrderBySeqDesc(Long sessionId, String type);

	Optional<SessionEventEntity> findFirstBySessionIdAndTypeOrderBySeqAsc(Long sessionId, String type);

}
