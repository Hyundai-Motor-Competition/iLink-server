package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface PickupSessionJpaRepository extends JpaRepository<PickupSessionEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PickupSessionEntity> findForUpdateBySessionId(Long sessionId);

	List<PickupSessionEntity> findBySessionStatusAndBoardingCancelableUntilBeforeOrderBySessionId(
			SessionStatus sessionStatus, Instant now);

	@Query("""
			select s from PickupSessionEntity s
			where s.sessionStatus = :status
			  and s.etaUpdatedAt < :staleBefore
			  and s.boardingDetectedAt is null
			  and exists (select 1 from CallEntity c
			              where c.sessionId = s.sessionId and c.status <> :revoked)
			order by s.sessionId
			""")
	List<PickupSessionEntity> findSignalLost(@Param("status") SessionStatus status,
			@Param("staleBefore") Instant staleBefore, @Param("revoked") CallStatus revoked);

	Optional<PickupSessionEntity> findFirstByVehicleIdAndSessionStatusInOrderBySessionIdDesc(Long vehicleId,
			Collection<SessionStatus> statuses);

	Optional<PickupSessionEntity> findFirstByFamilyIdAndSessionStatusInOrderBySessionIdDesc(Long familyId,
			Collection<SessionStatus> statuses);

	List<PickupSessionEntity> findByFamilyIdAndDateOrderBySessionId(Long familyId, LocalDate date);

	Optional<PickupSessionEntity> findFirstBySpotIdAndSessionStatusInOrderBySessionIdDesc(Long spotId,
			Collection<SessionStatus> statuses);

	@Query("""
			select s from PickupSessionEntity s
			where s.sessionStatus in :statuses
			  and exists (select 1 from SessionChildEntity c
			              where c.sessionId = s.sessionId and c.childUserId = :childUserId)
			order by s.sessionId desc
			""")
	List<PickupSessionEntity> findByChildUserIdAndSessionStatusIn(@Param("childUserId") Long childUserId,
			@Param("statuses") Collection<SessionStatus> statuses, Pageable pageable);

	@Query("""
			select s from PickupSessionEntity s
			where s.sessionStatus in :statuses
			  and s.date between :fromDate and :toDate
			  and exists (select 1 from SessionChildEntity c
			              where c.sessionId = s.sessionId and c.childUserId = :childUserId)
			order by s.date, s.sessionId
			""")
	List<PickupSessionEntity> findByChildUserIdAndSessionStatusInAndDateBetween(
			@Param("childUserId") Long childUserId, @Param("statuses") Collection<SessionStatus> statuses,
			@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

}
