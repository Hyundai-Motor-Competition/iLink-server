package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.SessionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record HistoryItem(
		LocalDate date,
		long sessionId,
		SessionStatus status,
		Instant completedAt
) {
}
