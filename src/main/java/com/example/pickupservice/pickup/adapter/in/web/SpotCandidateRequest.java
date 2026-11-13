package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.in.SpotCandidate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpotCandidateRequest(
		@NotBlank @Size(max = 50) String name,
		@NotNull @Valid LocationRequest location
) {

	public SpotCandidate toCandidate() {
		return new SpotCandidate(name, new LatLng(location.lat(), location.lng()));
	}

	public record LocationRequest(
			@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
			@NotNull @DecimalMin("-180") @DecimalMax("180") Double lng
	) {
	}

}
