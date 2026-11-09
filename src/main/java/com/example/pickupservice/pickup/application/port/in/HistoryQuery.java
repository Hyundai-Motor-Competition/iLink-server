package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

import java.time.YearMonth;

public record HistoryQuery(
		AuthPrincipal principal,
		long childId,
		YearMonth month
) {
}
