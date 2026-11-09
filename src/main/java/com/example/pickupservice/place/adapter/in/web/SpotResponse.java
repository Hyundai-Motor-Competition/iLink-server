package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.SpotView;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpotResponse(
		long spotId,
		String name,
		LatLng stopPoint,
		LatLng meetingPoint,
		String memo,
		@JsonProperty("isDefault") boolean isDefault
) {

	public static SpotResponse from(SpotView view) {
		return new SpotResponse(view.spotId(), view.name(), view.stopPoint(), view.meetingPoint(), view.memo(),
				view.isDefault());
	}
}
