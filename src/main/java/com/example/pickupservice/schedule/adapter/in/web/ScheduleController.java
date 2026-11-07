package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.adapter.in.web.doc.ScheduleApiDoc;
import com.example.pickupservice.schedule.application.port.in.SearchAcademyUseCase;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApiDoc {

	private final SearchAcademyUseCase searchAcademyUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/places/academies")
	public AcademySearchResponse searchAcademies(@RequestParam("query") @Size(min = 2, max = 50) String query) {
		return AcademySearchResponse.from(searchAcademyUseCase.searchAcademies(query));
	}

}
