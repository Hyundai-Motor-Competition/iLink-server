package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PickupSpot {

	private final Long spotId;
	private final long familyId;
	private final long academyId;
	private final String name;
	private final String memo;
	private final LatLng stopPoint;
	private final LatLng meetingPoint;
	private final boolean isDefault;
	private final long createdBy;

	public static PickupSpot create(long familyId, long academyId, String name, String memo, LatLng stopPoint,
			LatLng meetingPoint, boolean isDefault, long createdBy) {
		return new PickupSpot(null, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy);
	}

	public static PickupSpot restore(Long spotId, long familyId, long academyId, String name, String memo,
			LatLng stopPoint, LatLng meetingPoint, boolean isDefault, long createdBy) {
		return new PickupSpot(spotId, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy);
	}

	public PickupSpot withDefault(boolean isDefault) {
		return new PickupSpot(spotId, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy);
	}

	public boolean isSiblingOf(PickupSpot other) {
		if (spotId != null && spotId.equals(other.spotId)) {
			return false;
		}
		return familyId == other.familyId && academyId == other.academyId;
	}
}
