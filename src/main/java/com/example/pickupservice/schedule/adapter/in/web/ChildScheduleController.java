package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.doc.ChildScheduleApiDoc;
import com.example.pickupservice.schedule.application.port.in.GetNextPickupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChildScheduleController implements ChildScheduleApiDoc {

	private final GetNextPickupUseCase getNextPickupUseCase;

	@Override
	@PreAuthorize("hasRole('CHILD')")
	@GetMapping("/v1/children/me/next-pickup")
	public NextPickupResponse getNextPickup(@AuthenticationPrincipal AuthPrincipal principal) {
		return getNextPickupUseCase.getNextPickup(principal.familyId(), principal.id())
				.map(NextPickupResponse::from)
				.orElse(null);
	}

}
