package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.doc.SetupApiDoc;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.CompleteSetupUseCase;
import com.example.pickupservice.schedule.application.port.in.GetSetupSummaryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SetupController implements SetupApiDoc {

	private final GetSetupSummaryUseCase getSetupSummaryUseCase;
	private final CompleteSetupUseCase completeSetupUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/children/{childId}/setup-summary")
	public SetupSummaryResponse getSetupSummary(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long childId) {
		return SetupSummaryResponse.from(getSetupSummaryUseCase.getSetupSummary(
				new ChildQuery(principal.familyId(), childId)));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PostMapping("/v1/children/{childId}/setup-completion")
	public SetupCompletionResponse completeSetup(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long childId,
			@Valid @RequestBody SetupCompletionRequest request) {
		return SetupCompletionResponse.from(completeSetupUseCase.completeSetup(
				request.toCommand(principal.familyId(), childId)));
	}

}
