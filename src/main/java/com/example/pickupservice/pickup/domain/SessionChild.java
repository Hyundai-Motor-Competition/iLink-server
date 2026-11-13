package com.example.pickupservice.pickup.domain;

import com.example.pickupservice.common.geo.LatLng;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionChild {

	private Long sessionChildId;
	private long childUserId;
	private long scheduleId;
	private ChildPhase childPhase;
	private int extraPrepMinutes;
	private Instant callNotBefore;
	private LatLng lastLocation;
	private Instant lastLocatedAt;
	private Instant acknowledgedAt;
	private Instant boardedAt;
	private Instant readyAt;

	public static SessionChild waiting(long childUserId, long scheduleId) {
		return new SessionChild(null, childUserId, scheduleId, ChildPhase.WAITING, 0, null, null, null, null, null,
				null);
	}

	public static SessionChild restore(Long sessionChildId, long childUserId, long scheduleId, ChildPhase childPhase,
			int extraPrepMinutes, Instant callNotBefore, LatLng lastLocation, Instant lastLocatedAt,
			Instant acknowledgedAt, Instant boardedAt) {
		return restore(sessionChildId, childUserId, scheduleId, childPhase, extraPrepMinutes, callNotBefore,
				lastLocation, lastLocatedAt, acknowledgedAt, boardedAt, null);
	}

	public static SessionChild restore(Long sessionChildId, long childUserId, long scheduleId, ChildPhase childPhase,
			int extraPrepMinutes, Instant callNotBefore, LatLng lastLocation, Instant lastLocatedAt,
			Instant acknowledgedAt, Instant boardedAt, Instant readyAt) {
		return new SessionChild(sessionChildId, childUserId, scheduleId, childPhase, extraPrepMinutes, callNotBefore,
				lastLocation, lastLocatedAt, acknowledgedAt, boardedAt, readyAt);
	}

	public void board(Instant at) {
		boardedAt = at;
	}

	public void clearBoarded() {
		boardedAt = null;
	}

	public void markNotBoarded() {
		childPhase = ChildPhase.NOT_BOARDED;
	}

	public void updateLocation(LatLng location, Instant at) {
		if (lastLocatedAt != null && at.isBefore(lastLocatedAt)) {
			return;
		}
		lastLocation = location;
		lastLocatedAt = at;
	}

	public void clearLocation() {
		lastLocation = null;
		lastLocatedAt = null;
	}

	public void acknowledge(Instant at) {
		if (acknowledgedAt != null) {
			return;
		}
		acknowledgedAt = at;
		childPhase = ChildPhase.MEETING;
	}

	public void markReady(Instant at) {
		if (readyAt != null) {
			return;
		}
		readyAt = at;
	}

	public void markCalled() {
		childPhase = ChildPhase.CALLED;
	}

	public void revertToWaiting() {
		childPhase = ChildPhase.WAITING;
		acknowledgedAt = null;
		clearLocation();
	}

	public void extendPrep(int minutes, Instant now, int limitMinutes) {
		if (extraPrepMinutes + minutes > limitMinutes) {
			throw new PickupRuleException(PickupViolation.PREP_EXTENSION_LIMIT);
		}
		extraPrepMinutes += minutes;
		callNotBefore = now.plus(Duration.ofMinutes(extraPrepMinutes));
	}
}
