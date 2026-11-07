package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.doc.ScheduleApiDoc;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ListSchedulesUseCase;
import com.example.pickupservice.schedule.application.port.in.SearchAcademyUseCase;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScheduleController implements ScheduleApiDoc {

	private final SearchAcademyUseCase searchAcademyUseCase;
	private final CreateScheduleUseCase createScheduleUseCase;
	private final UpdateScheduleUseCase updateScheduleUseCase;
	private final ListSchedulesUseCase listSchedulesUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/places/academies")
	public AcademySearchResponse searchAcademies(@RequestParam("query") @Size(min = 2, max = 50) String query) {
		return AcademySearchResponse.from(searchAcademyUseCase.searchAcademies(query));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PostMapping("/v1/children/{childId}/schedules")
	@ResponseStatus(HttpStatus.CREATED)
	public ScheduleResponse createSchedule(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long childId,
			@Valid @RequestBody ScheduleRequest request) {
		return ScheduleResponse.from(createScheduleUseCase.createSchedule(
				request.toCreateCommand(principal.familyId(), childId)));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PutMapping("/v1/schedules/{scheduleId}")
	public ScheduleResponse updateSchedule(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long scheduleId,
			@Valid @RequestBody ScheduleRequest request) {
		return ScheduleResponse.from(updateScheduleUseCase.updateSchedule(
				request.toUpdateCommand(principal.familyId(), scheduleId)));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/children/{childId}/schedules")
	public ScheduleListResponse listSchedules(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long childId) {
		return ScheduleListResponse.from(listSchedulesUseCase.listSchedules(
				new ChildQuery(principal.familyId(), childId)));
	}

}
