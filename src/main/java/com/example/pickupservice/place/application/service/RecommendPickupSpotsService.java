package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.RecommendPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.RecommendSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotRecommendations;
import com.example.pickupservice.place.application.port.out.DriveTimePort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotCandidatePort;
import com.example.pickupservice.place.application.port.out.ServiceAreaPort;
import com.example.pickupservice.place.domain.SpotCandidate;
import com.example.pickupservice.place.domain.SpotCandidateMerger;
import com.example.pickupservice.place.domain.SpotRankingRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendPickupSpotsService implements RecommendPickupSpotsUseCase {

	private static final int PARKING_RADIUS_M = 500;
	private static final int CURB_RADIUS_M = 450;

	private final LoadAcademyPort loadAcademyPort;
	private final ServiceAreaPort serviceAreaPort;
	private final LoadSpotCandidatePort loadSpotCandidatePort;
	private final DriveTimePort driveTimePort;

	@Override
	public SpotRecommendations recommendSpots(RecommendSpotsQuery q) {
		LatLng academy = loadAcademyPort.loadAcademy(q.academyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND)).location();
		if (!serviceAreaPort.isSupported(academy)) {
			return new SpotRecommendations(false, List.of());
		}

		List<SpotCandidate> candidates = SpotCandidateMerger.merge(academy,
				loadSpotCandidatePort.loadPublicParkingWithin(academy, PARKING_RADIUS_M),
				loadSpotCandidatePort.loadCurbStopsWithin(academy, CURB_RADIUS_M));
		if (candidates.isEmpty()) {
			return new SpotRecommendations(true, List.of());
		}
		List<LatLng> destinations = candidates.stream().map(SpotCandidate::location).toList();
		List<Integer> driveSeconds = q.arriveAt() == null
				? driveTimePort.driveSecondsNow(q.origin(), destinations)
				: driveTimePort.driveSecondsAt(q.origin(), destinations, q.arriveAt());
		return new SpotRecommendations(true, SpotRankingRule.rank(candidates, driveSeconds));
	}
}
