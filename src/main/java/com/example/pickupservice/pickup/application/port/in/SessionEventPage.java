package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.pickup.domain.SessionEvent;

import java.util.List;

public record SessionEventPage(
		List<SessionEvent> items,
		boolean hasMore
) {
}
