package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.schedule.adapter.in.web.doc.VehicleScheduleApiDoc;
import com.example.pickupservice.schedule.application.port.in.GetVehicleSettingsSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.GetWeeklyPickupsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class VehicleScheduleController implements VehicleScheduleApiDoc {

	private final GetWeeklyPickupsUseCase getWeeklyPickupsUseCase;
	private final GetVehicleSettingsSummaryUseCase getVehicleSettingsSummaryUseCase;

	@Override
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping("/v1/vehicles/me/weekly-pickups")
	public WeeklyPickupsResponse getWeeklyPickups(
			@AuthenticationPrincipal AuthPrincipal principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return WeeklyPickupsResponse.from(getWeeklyPickupsUseCase.getWeeklyPickups(principal.familyId(), weekStart));
	}

	@Override
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping("/v1/vehicles/me/settings-summary")
	public VehicleSettingsSummaryResponse getSettingsSummary(@AuthenticationPrincipal AuthPrincipal principal) {
		return VehicleSettingsSummaryResponse.from(
				getVehicleSettingsSummaryUseCase.getVehicleSettingsSummary(principal.familyId()));
	}

}
