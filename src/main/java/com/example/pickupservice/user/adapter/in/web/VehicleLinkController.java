package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.user.adapter.in.web.doc.VehicleLinkApiDoc;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleCommand;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VehicleLinkController implements VehicleLinkApiDoc {

	private final UnlinkVehicleUseCase unlinkVehicleUseCase;

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasRole('GUARDIAN')")
	@DeleteMapping("/v1/vehicle-links/{vehicleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unlinkVehicle(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long vehicleId) {
		unlinkVehicleUseCase.unlinkVehicle(new UnlinkVehicleCommand(principal.familyId(), vehicleId));
	}

}
