package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.SessionChild;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivePickupQueryService implements ActivePickupQueryUseCase {

	private final LoadSessionPort loadSessionPort;

	@Override
	public boolean hasActivePickup(long vehicleId) {
		return loadSessionPort.loadActiveByVehicle(vehicleId).isPresent();
	}

	@Override
	public boolean isInActivePickup(long userId) {
		return loadSessionPort.loadActiveByChild(userId).isPresent();
	}

	@Override
	public boolean isSpotInUse(long spotId) {
		return loadSessionPort.loadActiveBySpot(spotId).isPresent();
	}

	@Override
	public Optional<LatLng> activeChildLocation(long familyId) {
		return loadSessionPort.loadActiveByFamily(familyId)
				.flatMap(session -> session.getChildren().stream()
						.filter(child -> child.getLastLocation() != null)
						.max(Comparator.comparing(SessionChild::getLastLocatedAt,
								Comparator.nullsFirst(Comparator.naturalOrder())))
						.map(SessionChild::getLastLocation));
	}

}
