package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SpotSnapshot;
import com.example.pickupservice.place.application.port.in.SpotInfo;

final class TemporarySpot {

	private TemporarySpot() {
	}

	static SpotSnapshot createAtCurrentAcademy(PickupSession session, String name, LatLng location,
			LoadSpotPort loadSpotPort, SaveTemporarySpotPort saveTemporarySpotPort) {
		long academyId = loadSpotPort.loadSpot(session.getSpotId())
				.map(SpotInfo::academyId)
				.orElseThrow(PickupErrors::spotNotSaved);
		long spotId = saveTemporarySpotPort.saveTemporarySpot(session.getFamilyId(), academyId, name, location,
				location);
		return new SpotSnapshot(spotId, name, location, location);
	}

}
