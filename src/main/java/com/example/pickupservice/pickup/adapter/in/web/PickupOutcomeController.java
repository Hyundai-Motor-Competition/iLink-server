package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.doc.PickupOutcomeApiDoc;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeUseCase;
import com.example.pickupservice.pickup.application.port.in.GetOutcomeUseCase;
import com.example.pickupservice.pickup.application.port.in.GetPickupHistoryUseCase;
import com.example.pickupservice.pickup.application.port.in.HistoryQuery;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
public class PickupOutcomeController implements PickupOutcomeApiDoc {

	private final GetPickupHistoryUseCase getPickupHistoryUseCase;
	private final GetOutcomeUseCase getOutcomeUseCase;
	private final CorrectOutcomeUseCase correctOutcomeUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/children/{childId}/pickup-history")
	public ResponseEntity<PickupHistoryResponse> history(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long childId,
			@RequestParam @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month) {
		HistoryQuery query = new HistoryQuery(principal, childId, YearMonth.parse(month));
		return ResponseEntity.ok(PickupHistoryResponse.from(getPickupHistoryUseCase.history(query)));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/pickup-sessions/{sessionId}/outcome")
	public ResponseEntity<OutcomeResponse> getOutcome(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId) {
		return ResponseEntity.ok(OutcomeResponse.from(
				getOutcomeUseCase.getOutcome(new SessionQuery(principal, sessionId))));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PutMapping("/v1/pickup-sessions/{sessionId}/outcome/correction")
	public ResponseEntity<OutcomeResponse> correct(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody CorrectOutcomeRequest request) {
		return ResponseEntity.ok(OutcomeResponse.from(
				correctOutcomeUseCase.correct(request.toCommand(principal, sessionId))));
	}

}
