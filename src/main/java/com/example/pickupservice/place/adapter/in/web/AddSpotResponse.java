package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.place.application.port.in.SpotCreated;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AddSpotResponse(
		long spotId,
		@JsonProperty("isDefault") boolean isDefault
) {

	public static AddSpotResponse from(SpotCreated created) {
		return new AddSpotResponse(created.spotId(), created.isDefault());
	}
}
