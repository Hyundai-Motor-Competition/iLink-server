package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterVehicleDeviceRequest(
		@ValidVin String vin,
		@NotBlank @Size(max = 50) String model
) {

	public RegisterVehicleDeviceCommand toCommand() {
		return new RegisterVehicleDeviceCommand(vin, model);
	}

}
