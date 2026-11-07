package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.UpdateSpotCommand;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/** 모두 선택. 보내지 않은 필드는 null로 넘긴다. */
public record UpdateSpotRequest(
		@Size(min = 1, max = 50) String name,
		LatLng stopPoint,
		LatLng meetingPoint,
		@Size(max = 200) String memo,
		@JsonProperty("isDefault") Boolean isDefault
) {

	public UpdateSpotCommand toCommand(long familyId, long spotId) {
		return new UpdateSpotCommand(familyId, spotId, name, stopPoint, meetingPoint, memo, isDefault);
	}
}
