package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.OffsetDateTime;

public record CorrectOutcomeRequest(
		@NotNull OffsetDateTime correctedStopAt,
		@NotNull @Pattern(regexp = "WRONG_STOP_TIME|OTHER") String reasonCode,
		@Size(max = 200) String note
) {

	public CorrectOutcomeCommand toCommand(AuthPrincipal principal, long sessionId) {
		Instant correctedStopAtUtc = correctedStopAt == null ? null : TimeUtils.toUtc(correctedStopAt);
		return new CorrectOutcomeCommand(principal, sessionId, correctedStopAtUtc, reasonCode, note);
	}

}
