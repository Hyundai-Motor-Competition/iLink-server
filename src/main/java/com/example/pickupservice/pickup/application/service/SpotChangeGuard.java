package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.domain.PickupRuleException;
import com.example.pickupservice.pickup.domain.PickupSession;

final class SpotChangeGuard {

	private SpotChangeGuard() {
	}

	// 장소를 바꿀 수 있는지: 이동 중(MEETING) 자녀가 있으면 CHILD_ALREADY_MOVING, 살아 있는 호출이 있으면 CALL_ALREADY_SENT
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
