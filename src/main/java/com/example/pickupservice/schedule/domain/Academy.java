package com.example.pickupservice.schedule.domain;

import com.example.pickupservice.common.geo.LatLng;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Academy {

	private final Long academyId;
	private final String externalPlaceId;
	private final String name;
	private final String address;
	private final LatLng location;
	private final LatLng entrance;
	private final String phone;

	public static Academy create(String externalPlaceId, String name, String address, LatLng location,
			LatLng entrance, String phone) {
		return new Academy(null, externalPlaceId, name, address, location, entrance, phone);
	}

	public static Academy restore(Long academyId, String externalPlaceId, String name, String address,
			LatLng location, LatLng entrance, String phone) {
		return new Academy(academyId, externalPlaceId, name, address, location, entrance, phone);
	}

	public Academy updateInfo(String name, String address, LatLng location, LatLng entrance, String phone) {
		return new Academy(academyId, externalPlaceId, name, address, location, entrance, phone);
	}

	public boolean hasEntrance() {
		return entrance != null;
	}
}
