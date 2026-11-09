package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcademyRequest(
		@NotBlank @Size(max = 100) String externalPlaceId,
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 255) String address,
		@NotNull LatLng location,
		LatLng entrance,
		@Size(max = 20) String phone
) {

	AcademyInput toInput() {
		return new AcademyInput(externalPlaceId, name, address, location, entrance, phone);
	}
}
