package com.example.pickupservice.place.domain;

import java.util.List;

public record RecommendedSpot(
		int rank,
		SpotCandidate candidate,
		Integer driveSeconds,
		Integer totalSeconds,
		List<SpotCaution> cautions) {
}
