package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.ChildPhase;

import java.time.Instant;

public record SessionChildView(
		long childUserId,
		String name,
		ChildPhase phase,
		int extraPrepMinutes,
		Instant callNotBefore,
		Instant acknowledgedAt,
		Instant boardedAt,
		ChildLocationView location,
		Instant readyAt,
		String profileImageUrl
) {

	public SessionChildView(long childUserId, String name, ChildPhase phase, int extraPrepMinutes,
			Instant callNotBefore, Instant acknowledgedAt, Instant boardedAt, ChildLocationView location,
			Instant readyAt) {
		this(childUserId, name, phase, extraPrepMinutes, callNotBefore, acknowledgedAt, boardedAt, location, readyAt,
				null);
	}

	public SessionChildView(long childUserId, String name, ChildPhase phase, int extraPrepMinutes,
			Instant callNotBefore, Instant acknowledgedAt, Instant boardedAt, ChildLocationView location) {
		this(childUserId, name, phase, extraPrepMinutes, callNotBefore, acknowledgedAt, boardedAt, location, null);
	}
}
