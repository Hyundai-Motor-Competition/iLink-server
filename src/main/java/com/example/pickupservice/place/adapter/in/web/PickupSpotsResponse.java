package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AcademySpots;
import com.example.pickupservice.place.application.port.in.SpotGroups;
import com.example.pickupservice.place.application.port.in.SpotItem;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PickupSpotsResponse(
		List<AcademySpotsResponse> academies
) {

	public static PickupSpotsResponse from(SpotGroups groups) {
		return new PickupSpotsResponse(groups.academies().stream().map(AcademySpotsResponse::from).toList());
	}

	public record AcademySpotsResponse(long academyId, String academyName, List<SpotItemResponse> spots) {

		static AcademySpotsResponse from(AcademySpots academy) {
			return new AcademySpotsResponse(academy.academyId(), academy.academyName(),
					academy.spots().stream().map(SpotItemResponse::from).toList());
		}
	}

	public record SpotItemResponse(
			long spotId,
			String name,
			LatLng stopPoint,
			LatLng meetingPoint,
			String memo,
			@JsonProperty("isDefault") boolean isDefault,
			Integer walkMinutes
	) {

		static SpotItemResponse from(SpotItem item) {
			return new SpotItemResponse(item.spotId(), item.name(), item.stopPoint(), item.meetingPoint(), item.memo(),
					item.isDefault(), item.walkMinutes());
		}
	}
}
