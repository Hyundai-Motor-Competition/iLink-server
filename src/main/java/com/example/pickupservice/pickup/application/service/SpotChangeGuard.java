package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;

final class SpotChangeGuard {

	private SpotChangeGuard() {
	}

	static void requireSpotChangeable(PickupSession session, LoadCallPort loadCallPort) {
		try {
			session.requireSpotChangeable();
		} catch (PickupRuleException exception) {
			throw PickupErrors.from(exception);
		}
		if (loadCallPort.loadLiveCall(session.getSessionId()).isPresent()) {
			throw PickupErrors.callAlreadySent();
		}
	}

}
