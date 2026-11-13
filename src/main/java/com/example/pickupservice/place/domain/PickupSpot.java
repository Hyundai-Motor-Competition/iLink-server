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
	private final boolean temporary;

	public static PickupSpot create(long familyId, long academyId, String name, String memo, LatLng stopPoint,
			LatLng meetingPoint, boolean isDefault, long createdBy) {
		return new PickupSpot(null, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy, false);
	}

	/** 차량 세션용 임시 장소. memo 없이 기본 장소가 아닌 상태로 만든다. */
	public static PickupSpot createTemporary(long familyId, long academyId, String name, LatLng stopPoint,
			LatLng meetingPoint, long createdBy) {
		return new PickupSpot(null, familyId, academyId, name, null, stopPoint, meetingPoint, false, createdBy, true);
	}

	public static PickupSpot restore(Long spotId, long familyId, long academyId, String name, String memo,
			LatLng stopPoint, LatLng meetingPoint, boolean isDefault, long createdBy) {
		return restore(spotId, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy, false);
	}

	public static PickupSpot restore(Long spotId, long familyId, long academyId, String name, String memo,
			LatLng stopPoint, LatLng meetingPoint, boolean isDefault, long createdBy, boolean temporary) {
		return new PickupSpot(spotId, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy,
				temporary);
	}

	public PickupSpot withDefault(boolean isDefault) {
		return new PickupSpot(spotId, familyId, academyId, name, memo, stopPoint, meetingPoint, isDefault, createdBy,
				temporary);
	}

	/** 차량 세션용 임시 장소면 true. 가족 장소 목록 · 기본 장소 판정에서 빠진다. */
	public boolean isTemporary() {
		return temporary;
	}

	public boolean isSiblingOf(PickupSpot other) {
		if (spotId != null && spotId.equals(other.spotId)) {
			return false;
		}
		return familyId == other.familyId && academyId == other.academyId;
	}
}
