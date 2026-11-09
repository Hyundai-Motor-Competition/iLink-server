package com.example.pickupservice.place.application.port.in;

import java.util.List;

public record AcademySpots(
		long academyId,
		String academyName,
		List<SpotItem> spots
) {
}
