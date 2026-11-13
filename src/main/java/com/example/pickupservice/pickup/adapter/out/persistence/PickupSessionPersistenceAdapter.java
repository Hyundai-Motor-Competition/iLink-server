package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PickupSessionPersistenceAdapter implements LoadSessionPort, SaveSessionPort {

	private static final List<SessionStatus> IN_PROGRESS = List.of(SessionStatus.ACTIVE, SessionStatus.ON_HOLD);
	private static final PageRequest LATEST_ONE = PageRequest.of(0, 1);

	private final PickupSessionJpaRepository pickupSessionJpaRepository;
	private final SessionChildJpaRepository sessionChildJpaRepository;

	@Override
	public Optional<PickupSession> load(long sessionId) {
		return pickupSessionJpaRepository.findById(sessionId).map(this::toDomainWithChildren);
	}

	@Override
	public Optional<PickupSession> loadForUpdate(long sessionId) {
		return pickupSessionJpaRepository.findForUpdateBySessionId(sessionId).map(this::toDomainWithChildren);
	}

	@Override
	public Optional<PickupSession> loadActiveByVehicle(long vehicleId) {
		return pickupSessionJpaRepository.findFirstByVehicleIdAndSessionStatusInOrderBySessionIdDesc(vehicleId,
				IN_PROGRESS).map(this::toDomainWithChildren);
	}

	@Override
	public Optional<PickupSession> loadActiveByFamily(long familyId) {
		return pickupSessionJpaRepository.findFirstByFamilyIdAndSessionStatusInOrderBySessionIdDesc(familyId,
				IN_PROGRESS).map(this::toDomainWithChildren);
	}

	@Override
	public Optional<PickupSession> loadActiveByChild(long childUserId) {
		return pickupSessionJpaRepository.findByChildUserIdAndSessionStatusIn(childUserId, IN_PROGRESS, LATEST_ONE)
				.stream()
				.findFirst()
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<PickupSession> loadActiveBySpot(long spotId) {
		return pickupSessionJpaRepository.findFirstBySpotIdAndSessionStatusInOrderBySessionIdDesc(spotId,
				IN_PROGRESS).map(this::toDomainWithChildren);
	}

	@Override
	public List<PickupSession> loadBoardingExpired(Instant now) {
		return pickupSessionJpaRepository
				.findBySessionStatusAndBoardingCancelableUntilBeforeOrderBySessionId(SessionStatus.ACTIVE, now)
				.stream()
				.map(this::toDomainWithChildren)
				.toList();
	}

	@Override
	public List<PickupSession> loadSignalLost(Instant staleBefore) {
		return pickupSessionJpaRepository.findSignalLost(SessionStatus.ACTIVE, staleBefore, CallStatus.REVOKED)
				.stream()
				.map(this::toDomainWithChildren)
				.toList();
	}

	@Override
	@Transactional
	public PickupSession save(PickupSession session) {
		PickupSessionEntity savedSession = pickupSessionJpaRepository.save(toEntity(session));
		List<SessionChildEntity> savedChildren = sessionChildJpaRepository.saveAllAndFlush(session.getChildren()
				.stream()
				.map(child -> toEntity(savedSession.getSessionId(), child))
				.toList());
		return toDomain(savedSession, savedChildren);
	}

	private PickupSession toDomainWithChildren(PickupSessionEntity entity) {
		return toDomain(entity, sessionChildJpaRepository.findBySessionIdOrderBySessionChildId(entity.getSessionId()));
	}

	private static PickupSession toDomain(PickupSessionEntity entity, List<SessionChildEntity> children) {
		return PickupSession.restore(entity.getSessionId(), entity.getVehicleId(), entity.getFamilyId(),
				entity.getSpotId(), entity.getSpotName(), GeoPoints.toLatLng(entity.getStopPoint()),
				GeoPoints.toLatLng(entity.getMeetingPoint()), entity.getDate(), entity.getSessionStatus(),
				entity.getVehiclePhase(), entity.getSpotStatus(), entity.getStateVersion(), entity.getSpotVersion(),
				entity.getRouteVersion(), entity.getRouteSpotVersion(), entity.isCallHeld(), entity.getEtaAt(),
				entity.getRemainingMeters(), entity.getEtaUpdatedAt(), entity.getBoardingDetectedAt(),
				entity.getBoardingCancelableUntil(), entity.getBoardingDoor(), entity.getIncidentType(),
				entity.getIncidentCause(), entity.getIncidentChildUserId(), entity.getIncidentOpenedAt(),
				entity.getIncidentResolvedAt(), toLatLng(entity.getVehicleLat(), entity.getVehicleLng()),
				entity.getVehicleLocatedAt(), entity.getRouteTotalMeters(),
				children.stream().map(PickupSessionPersistenceAdapter::toDomain).toList());
	}

	private static SessionChild toDomain(SessionChildEntity entity) {
		return SessionChild.restore(entity.getSessionChildId(), entity.getChildUserId(), entity.getScheduleId(),
				entity.getChildPhase(), entity.getExtraPrepMinutes(), entity.getCallNotBefore(),
				toLatLng(entity.getLastLat(), entity.getLastLng()), entity.getLastLocatedAt(),
				entity.getAcknowledgedAt(), entity.getBoardedAt(), entity.getReadyAt());
	}

	private static PickupSessionEntity toEntity(PickupSession session) {
		LatLng vehicleLocation = session.getVehicleLocation();
		return new PickupSessionEntity(session.getSessionId(), session.getVehicleId(), session.getFamilyId(),
				session.getSpotId(), session.getSpotName(),
				GeoPoints.toPoint(session.getStopPoint().lat(), session.getStopPoint().lng()),
				GeoPoints.toPoint(session.getMeetingPoint().lat(), session.getMeetingPoint().lng()), session.getDate(),
				session.getSessionStatus(), session.getVehiclePhase(), session.getSpotStatus(),
				session.getStateVersion(), session.getSpotVersion(), session.getRouteVersion(),
				session.getRouteSpotVersion(), session.isCallHeld(), session.getEtaAt(), session.getRemainingMeters(),
				session.getEtaUpdatedAt(), session.getBoardingDetectedAt(), session.getBoardingCancelableUntil(),
				session.getBoardingDoor(), session.getIncidentType(), session.getIncidentCause(),
				session.getIncidentChildUserId(), session.getIncidentOpenedAt(), session.getIncidentResolvedAt(),
				vehicleLocation == null ? null : BigDecimal.valueOf(vehicleLocation.lat()),
				vehicleLocation == null ? null : BigDecimal.valueOf(vehicleLocation.lng()),
				session.getVehicleLocatedAt(), session.getRouteTotalMeters());
	}

	private static SessionChildEntity toEntity(Long sessionId, SessionChild child) {
		LatLng location = child.getLastLocation();
		return new SessionChildEntity(child.getSessionChildId(), sessionId, child.getChildUserId(),
				child.getScheduleId(), child.getChildPhase(), (short) child.getExtraPrepMinutes(),
				child.getCallNotBefore(), location == null ? null : BigDecimal.valueOf(location.lat()),
				location == null ? null : BigDecimal.valueOf(location.lng()), child.getLastLocatedAt(),
				child.getAcknowledgedAt(), child.getBoardedAt(), child.getReadyAt());
	}

	private static LatLng toLatLng(BigDecimal lat, BigDecimal lng) {
		if (lat == null && lng == null) {
			return null;
		}
		return new LatLng(lat.doubleValue(), lng.doubleValue());
	}

}
