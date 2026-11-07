package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AddSpotCommand;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddSpotRequest(
		@NotNull Long academyId,
		@NotNull @Size(min = 1, max = 50) String name,
		@NotNull LatLng stopPoint,
		@NotNull LatLng meetingPoint,
		@Size(max = 200) String memo,
		@JsonProperty("isDefault") Boolean isDefault
) {

	public AddSpotCommand toCommand(long familyId, long createdBy) {
		return new AddSpotCommand(familyId, createdBy, academyId, name, stopPoint, meetingPoint, memo,
				Boolean.TRUE.equals(isDefault));
	}
}
