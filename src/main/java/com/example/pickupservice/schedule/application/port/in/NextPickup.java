package com.example.pickupservice.schedule.application.port.in;

import java.time.Instant;

public record NextPickup(
		String academyName,
		String spotName,
		Instant targetAt
) {
}
