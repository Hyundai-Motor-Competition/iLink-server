package com.example.pickupservice.pickup.domain;

import com.example.pickupservice.common.geo.LatLng;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PickupSession {

	private static final int APPROACHING_METERS = 700;
	private static final int APPROACHING_ETA_SECONDS = 60;

	private Long sessionId;
	private long vehicleId;
	private long familyId;
	private long spotId;
	private String spotName;
	private LatLng stopPoint;
	private LatLng meetingPoint;
	private LocalDate date;
	private SessionStatus sessionStatus;
	private VehiclePhase vehiclePhase;
	private SpotStatus spotStatus;
	private int stateVersion;
	private int spotVersion;
	private int routeVersion;
	private Integer routeSpotVersion;
	private boolean callHeld;
	private Instant etaAt;
	private Integer remainingMeters;
	private Instant etaUpdatedAt;
	private Instant boardingDetectedAt;
	private Instant boardingCancelableUntil;
	private BoardingDoor boardingDoor;
	private IncidentType incidentType;
	private String incidentCause;
	private Long incidentChildUserId;
	private Instant incidentOpenedAt;
	private Instant incidentResolvedAt;
	private LatLng vehicleLocation;
	private Instant vehicleLocatedAt;
	private List<SessionChild> children;

	public static PickupSession start(long vehicleId, long familyId, LocalDate date, SpotSnapshot spot,
			List<ChildSchedule> childSchedules) {
		List<SessionChild> children = childSchedules.stream()
				.map(schedule -> SessionChild.waiting(schedule.childUserId(), schedule.scheduleId()))
				.toList();
		return new PickupSession(null, vehicleId, familyId, spot.spotId(), spot.name(), spot.stopPoint(),
				spot.meetingPoint(), date, SessionStatus.ACTIVE, VehiclePhase.ROUTING, SpotStatus.SELECTED, 0, 0, 0,
				null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, children);
	}

	public static PickupSession restore(Long sessionId, long vehicleId, long familyId, long spotId, String spotName,
			LatLng stopPoint, LatLng meetingPoint, LocalDate date, SessionStatus sessionStatus,
			VehiclePhase vehiclePhase, SpotStatus spotStatus, int stateVersion, int spotVersion, int routeVersion,
			Integer routeSpotVersion, boolean callHeld, Instant etaAt, Integer remainingMeters, Instant etaUpdatedAt,
			Instant boardingDetectedAt, Instant boardingCancelableUntil, BoardingDoor boardingDoor,
			IncidentType incidentType, String incidentCause, Long incidentChildUserId, Instant incidentOpenedAt,
			Instant incidentResolvedAt, List<SessionChild> children) {
		return restore(sessionId, vehicleId, familyId, spotId, spotName, stopPoint, meetingPoint, date,
				sessionStatus, vehiclePhase, spotStatus, stateVersion, spotVersion, routeVersion, routeSpotVersion,
				callHeld, etaAt, remainingMeters, etaUpdatedAt, boardingDetectedAt, boardingCancelableUntil,
				boardingDoor, incidentType, incidentCause, incidentChildUserId, incidentOpenedAt, incidentResolvedAt,
				null, null, children);
	}

	public static PickupSession restore(Long sessionId, long vehicleId, long familyId, long spotId, String spotName,
			LatLng stopPoint, LatLng meetingPoint, LocalDate date, SessionStatus sessionStatus,
			VehiclePhase vehiclePhase, SpotStatus spotStatus, int stateVersion, int spotVersion, int routeVersion,
			Integer routeSpotVersion, boolean callHeld, Instant etaAt, Integer remainingMeters, Instant etaUpdatedAt,
			Instant boardingDetectedAt, Instant boardingCancelableUntil, BoardingDoor boardingDoor,
			IncidentType incidentType, String incidentCause, Long incidentChildUserId, Instant incidentOpenedAt,
			Instant incidentResolvedAt, LatLng vehicleLocation, Instant vehicleLocatedAt,
			List<SessionChild> children) {
		return new PickupSession(sessionId, vehicleId, familyId, spotId, spotName, stopPoint, meetingPoint, date,
				sessionStatus, vehiclePhase, spotStatus, stateVersion, spotVersion, routeVersion, routeSpotVersion,
				callHeld, etaAt, remainingMeters, etaUpdatedAt, boardingDetectedAt, boardingCancelableUntil,
				boardingDoor, incidentType, incidentCause, incidentChildUserId, incidentOpenedAt, incidentResolvedAt,
				vehicleLocation, vehicleLocatedAt, children);
	}

	public void applyTelemetry(Instant observedAt, int navEtaSeconds, int remainingMeters, LatLng location) {
		if (etaUpdatedAt != null && observedAt.isBefore(etaUpdatedAt)) {
			return;
		}
		this.etaAt = observedAt.plusSeconds(navEtaSeconds);
		this.remainingMeters = remainingMeters;
		this.etaUpdatedAt = observedAt;
		if (location != null) {
			this.vehicleLocation = location;
			this.vehicleLocatedAt = observedAt;
		}
		if (vehiclePhase == VehiclePhase.ROUTING
				&& (remainingMeters <= APPROACHING_METERS || navEtaSeconds <= APPROACHING_ETA_SECONDS)) {
			vehiclePhase = VehiclePhase.APPROACHING;
			raiseStateVersion();
		}
	}

	public void openIncident(IncidentType type, long childUserId, Instant now) {
		childOf(childUserId);
		rejectIfFinished();
		if (hasOpenIncident()) {
			throw new PickupRuleException(PickupViolation.INCIDENT_ALREADY_OPEN);
		}
		sessionStatus = SessionStatus.ON_HOLD;
		incidentType = type;
		incidentOpenedAt = now;
		incidentResolvedAt = null;
		raiseStateVersion();
		incidentChildUserId = childUserId;
	}

	public Instant boardedAtOf(long childUserId) {
		return childOf(childUserId).getBoardedAt();
	}

	public boolean detectBoarding(BoardingDoor door, Instant at, boolean callSent, Duration cancelWindow) {
		rejectIfFinished();
		if (vehiclePhase != VehiclePhase.STOPPED || !callSent) {
			return false;
		}
		boardingDetectedAt = at;
		boardingCancelableUntil = at.plus(cancelWindow);
		boardingDoor = door;
		children.forEach(child -> child.board(at));
		raiseStateVersion();
		return true;
	}

	public void rejectBoarding(long childUserId, Instant now) {
		if (boardingCancelableUntil == null || now.isAfter(boardingCancelableUntil)) {
			throw new PickupRuleException(PickupViolation.REJECTION_WINDOW_CLOSED);
		}
		childOf(childUserId).clearBoarded();
		raiseStateVersion();
	}

	public void complete(BoardingMethod method, List<Long> notBoardedChildUserIds, Instant now) {
		rejectIfFinished();
		boolean manual = method == BoardingMethod.MANUAL;
		if (manual && vehiclePhase != VehiclePhase.STOPPED) {
			throw new PickupRuleException(PickupViolation.VEHICLE_NOT_STOPPED);
		}
		if (!manual) {
			boolean someoneLeft = children.stream()
					.anyMatch(child -> child.getBoardedAt() == null
							&& !notBoardedChildUserIds.contains(child.getChildUserId()));
			if (someoneLeft) {
				throw new PickupRuleException(PickupViolation.CHILDREN_NOT_ALL_BOARDED);
			}
		}
		for (SessionChild child : children) {
			if (notBoardedChildUserIds.contains(child.getChildUserId())) {
				child.markNotBoarded();
			} else if (manual) {
				child.board(now);
			}
		}
		children.forEach(SessionChild::clearLocation);
		sessionStatus = SessionStatus.COMPLETED;
		raiseStateVersion();
	}

	public void applyTelemetry(Instant observedAt, int navEtaSeconds, int remainingMeters) {
		applyTelemetry(observedAt, navEtaSeconds, remainingMeters, null);
	}

	public boolean isEtaStale(Instant now, Duration validFor) {
		if (etaUpdatedAt == null) {
			return true;
		}
		return now.isAfter(etaUpdatedAt.plus(validFor));
	}

	public void hold() {
		rejectIfFinished();
		sessionStatus = SessionStatus.ON_HOLD;
		raiseStateVersion();
	}

	public void releaseHold(Instant now) {
		rejectIfFinished();
		if (hasOpenIncident()) {
			incidentResolvedAt = now;
		}
		sessionStatus = SessionStatus.ACTIVE;
		raiseStateVersion();
	}

	public void markStopped() {
		vehiclePhase = VehiclePhase.STOPPED;
		raiseStateVersion();
	}

	public void markDeparted() {
		vehiclePhase = VehiclePhase.DEPARTED;
		raiseStateVersion();
	}

	public void holdCall() {
		rejectIfFinished();
		callHeld = true;
		raiseStateVersion();
	}

	public void releaseCallHold() {
		rejectIfFinished();
		callHeld = false;
		raiseStateVersion();
	}

	public void cancel() {
		rejectIfFinished();
		sessionStatus = SessionStatus.CANCELLED;
		raiseStateVersion();
	}

	public void confirmRoute() {
		spotStatus = SpotStatus.CONFIRMED;
		routeVersion++;
		routeSpotVersion = spotVersion;
		raiseStateVersion();
	}

	public void changeSpot(SpotSnapshot spot) {
		boolean childMoving = children.stream().anyMatch(child -> child.getChildPhase() == ChildPhase.MEETING);
		if (childMoving) {
			throw new PickupRuleException(PickupViolation.CHILD_ALREADY_MOVING);
		}
		spotStatus = SpotStatus.SELECTED;
		spotVersion++;
		spotId = spot.spotId();
		spotName = spot.name();
		stopPoint = spot.stopPoint();
		meetingPoint = spot.meetingPoint();
		raiseStateVersion();
	}

	public void markChildrenCalled() {
		children.forEach(SessionChild::markCalled);
		raiseStateVersion();
	}

	public void revertChildrenToWaiting() {
		children.forEach(SessionChild::revertToWaiting);
		raiseStateVersion();
	}

	public void acknowledgeChild(long childUserId, Instant at) {
		childOf(childUserId).acknowledge(at);
		raiseStateVersion();
	}

	public void markChildReady(long childUserId, Instant at) {
		SessionChild child = childOf(childUserId);
		if (child.getReadyAt() != null) {
			return;
		}
		child.markReady(at);
		raiseStateVersion();
	}

	public void extendChildPrep(long childUserId, int minutes, Instant now, int limitMinutes) {
		childOf(childUserId).extendPrep(minutes, now, limitMinutes);
		raiseStateVersion();
	}

	public void updateChildLocation(long childUserId, LatLng location, Instant at) {
		childOf(childUserId).updateLocation(location, at);
	}

	private SessionChild childOf(long childUserId) {
		return children.stream()
				.filter(child -> child.getChildUserId() == childUserId)
				.findFirst()
				.orElseThrow(() -> new PickupRuleException(PickupViolation.CHILD_NOT_IN_SESSION));
	}

	private boolean hasOpenIncident() {
		return incidentOpenedAt != null && incidentResolvedAt == null;
	}

	public boolean isFinished() {
		return sessionStatus == SessionStatus.COMPLETED || sessionStatus == SessionStatus.CANCELLED;
	}

	private void rejectIfFinished() {
		if (isFinished()) {
			throw new PickupRuleException(PickupViolation.SESSION_ALREADY_FINISHED);
		}
	}

	private void raiseStateVersion() {
		stateVersion++;
	}
}
