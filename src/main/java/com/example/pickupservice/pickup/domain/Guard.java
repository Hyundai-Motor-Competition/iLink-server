package com.example.pickupservice.pickup.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Guard(Duration etaValidFor) {

	public GuardResult evaluate(PickupSession session, Instant now) {
		List<GuardId> unmet = new ArrayList<>();
		if (session.getSessionStatus() != SessionStatus.ACTIVE) {
			unmet.add(GuardId.G1);
		}
		if (session.getSpotStatus() != SpotStatus.CONFIRMED) {
			unmet.add(GuardId.G2);
		}
		if (!Objects.equals(session.getRouteSpotVersion(), session.getSpotVersion())) {
			unmet.add(GuardId.G3);
		}
		if (session.isEtaStale(now, etaValidFor)) {
			unmet.add(GuardId.G4);
		}
		boolean prepNotPassed = session.getChildren().stream()
				.anyMatch(child -> child.getCallNotBefore() != null && child.getCallNotBefore().isAfter(now));
		if (prepNotPassed) {
			unmet.add(GuardId.G5);
		}
		if (session.getVehiclePhase() != VehiclePhase.STOPPED) {
			unmet.add(GuardId.G6);
		}
		return GuardResult.of(unmet);
	}
}
