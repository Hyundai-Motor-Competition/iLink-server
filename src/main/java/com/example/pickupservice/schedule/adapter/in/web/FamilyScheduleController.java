package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.doc.FamilyScheduleApiDoc;
import com.example.pickupservice.schedule.application.port.in.GetTodayPickupsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FamilyScheduleController implements FamilyScheduleApiDoc {

	private final GetTodayPickupsUseCase getTodayPickupsUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/families/me/today-pickups")
	public TodayPickupsResponse getTodayPickups(@AuthenticationPrincipal AuthPrincipal principal) {
		return TodayPickupsResponse.from(getTodayPickupsUseCase.getTodayPickups(principal.familyId()));
	}

}
