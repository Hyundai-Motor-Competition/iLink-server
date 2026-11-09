package com.example.pickupservice.notification.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

	List<NotificationEntity> findBySessionIdOrderByNotificationIdAsc(Long sessionId);

	long countBySessionIdAndCallIdAndTypeAndDeviceId(Long sessionId, Long callId, String type, Long deviceId);

	long countBySessionIdAndCallIdIsNullAndTypeAndDeviceId(Long sessionId, String type, Long deviceId);

	List<NotificationEntity> findBySessionIdAndTypeAndCallIdAndSentAtIsNull(Long sessionId, String type, Long callId);

	List<NotificationEntity> findBySessionIdAndTypeAndCallIdIsNullAndSentAtIsNull(Long sessionId, String type);

	@Query("select n from NotificationEntity n "
			+ "where ((n.sentAt is null or n.deliveredAt is null) or n.type = 'CALL') "
			+ "and coalesce(n.sentAt, n.createdAt) <= :threshold "
			+ "and n.createdAt >= :cutoff")
	List<NotificationEntity> findResendTargets(@Param("threshold") Instant threshold, @Param("cutoff") Instant cutoff);

}
