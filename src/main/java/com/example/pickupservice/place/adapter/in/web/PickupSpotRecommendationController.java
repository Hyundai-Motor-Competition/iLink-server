package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.place.adapter.in.web.doc.PickupSpotRecommendationApiDoc;
import com.example.pickupservice.place.application.port.in.RecommendPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.RecommendSpotsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequiredArgsConstructor
public class PickupSpotRecommendationController implements PickupSpotRecommendationApiDoc {

	private final RecommendPickupSpotsUseCase recommendPickupSpotsUseCase;
	private final Clock clock;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping("/v1/pickup-spot-recommendations")
	public SpotRecommendationsResponse recommendSpots(
			@RequestParam(required = false) String academyId,
			@RequestParam(required = false) String origin,
			@RequestParam(required = false) String arriveAt) {
		long academy = parseAcademyId(academyId);
		if (origin == null || origin.isBlank()) {
			throw BusinessException.invalidField("origin", "필수 값입니다");
		}
		RecommendSpotsQuery query = new RecommendSpotsQuery(academy, LatLngParam.parse("origin", origin),
				parseFutureArriveAt(arriveAt));
		return SpotRecommendationsResponse.from(recommendPickupSpotsUseCase.recommendSpots(query));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping("/v1/pickup-spot-recommendations/nearby")
	public NearbySpotsResponse recommendNearbySpots(@RequestParam(required = false) String academyId) {
		long academy = parseAcademyId(academyId);
		return NearbySpotsResponse.from(recommendPickupSpotsUseCase.recommendNearbySpots(academy));
	}

	private static long parseAcademyId(String academyId) {
		if (academyId == null || academyId.isBlank()) {
			throw BusinessException.invalidField("academyId", "필수 값입니다");
		}
		try {
			return Long.parseLong(academyId.trim());
		} catch (NumberFormatException e) {
			throw BusinessException.invalidField("academyId", "숫자여야 합니다");
		}
	}

	private Instant parseFutureArriveAt(String arriveAt) {
		if (arriveAt == null) {
			return null;
		}
		Instant parsed;
		try {
			parsed = OffsetDateTime.parse(arriveAt.trim()).toInstant();
		} catch (DateTimeParseException e) {
			throw BusinessException.invalidField("arriveAt", "ISO-8601 시각 형식이어야 합니다");
		}
		if (parsed.isBefore(clock.instant())) {
			throw BusinessException.invalidField("arriveAt", "현재 이후여야 합니다");
		}
		return parsed;
	}

}
