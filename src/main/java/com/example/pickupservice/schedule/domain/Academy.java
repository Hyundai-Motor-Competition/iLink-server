package com.example.pickupservice.schedule.domain;

import com.example.pickupservice.common.geo.LatLng;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Academy {

	private final Long academyId;
	private final long familyId;
	private final String externalPlaceId;
	private final String name;
	private final String address;
	private final LatLng location;
	private final LatLng entrance;
	private final String phone;

	public static Academy create(long familyId, String externalPlaceId, String name, String address,
			LatLng location, LatLng entrance, String phone) {
		return new Academy(null, familyId, externalPlaceId, name, address, location, entrance, phone);
	}

	public static Academy restore(Long academyId, long familyId, String externalPlaceId, String name,
			String address, LatLng location, LatLng entrance, String phone) {
		return new Academy(academyId, familyId, externalPlaceId, name, address, location, entrance, phone);
	}

	public Academy updateInfo(String name, String address, LatLng location, LatLng entrance, String phone) {
		return new Academy(academyId, familyId, externalPlaceId, name, address, location,
				entrance != null ? entrance : this.entrance, phone != null ? phone : this.phone);
	}

	public boolean hasEntrance() {
		return entrance != null;
	}
}
