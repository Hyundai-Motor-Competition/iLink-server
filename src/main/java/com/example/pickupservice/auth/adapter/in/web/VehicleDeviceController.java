package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.adapter.in.web.doc.VehicleDeviceApiDoc;
import com.example.pickupservice.auth.application.port.in.LogoutVehicleUseCase;
import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceUseCase;
import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VehicleDeviceController implements VehicleDeviceApiDoc {

	private final RegisterVehicleDeviceUseCase registerVehicleDeviceUseCase;
	private final LogoutVehicleUseCase logoutVehicleUseCase;

	@Override
	@IdempotencyKeyRequired
	@PostMapping("/v1/vehicle-devices")
	@ResponseStatus(HttpStatus.CREATED)
	public VehicleDeviceResponse registerVehicleDevice(@Valid @RequestBody RegisterVehicleDeviceRequest request) {
		return VehicleDeviceResponse.from(registerVehicleDeviceUseCase.registerVehicleDevice(request.toCommand()));
	}

	@Override
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@DeleteMapping("/v1/vehicle-devices/me/session")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logoutVehicle() {
		logoutVehicleUseCase.logoutVehicle(CurrentDevice.id());
	}

}
