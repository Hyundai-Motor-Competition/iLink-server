package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.LoadSpotCandidatePort;
import com.example.pickupservice.place.domain.CurbStopCandidate;
import com.example.pickupservice.place.domain.PublicParking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class SpotCandidatePersistenceAdapter implements LoadSpotCandidatePort {

	private final PublicParkingJpaRepository publicParkingRepository;
	private final CurbStopCandidateJpaRepository curbStopRepository;

	@Override
	public List<PublicParking> loadPublicParkingWithin(LatLng center, int radiusM) {
		return publicParkingRepository.findWithin(center.lat(), center.lng(), radiusM).stream()
				.map(e -> new PublicParking(e.getParkingId(), e.getName(), GeoPoints.toLatLng(e.getLocation()),
						e.getSpaces(), e.getBaseMinutes(), e.getBaseFeeWon()))
				.toList();
	}

	@Override
	public List<CurbStopCandidate> loadCurbStopsWithin(LatLng center, int radiusM) {
		return curbStopRepository.findWithin(center.lat(), center.lng(), radiusM).stream()
				.map(e -> new CurbStopCandidate(e.getCandidateId(), GeoPoints.toLatLng(e.getLocation()),
						e.getRoadName(), e.getLineKind(), e.isNearEnforcementCctv()))
				.toList();
	}
}
