package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;

final class LatLngParam {

	private LatLngParam() {
	}

	static LatLng parse(String field, String value) {
		String[] parts = value.split(",", -1);
		if (parts.length != 2) {
			throw invalid(field);
		}
		try {
			return new LatLng(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
		} catch (NumberFormatException e) {
			throw invalid(field);
		}
	}

	private static BusinessException invalid(String field) {
		return BusinessException.invalidField(field, "lat,lng 형식이어야 합니다");
	}

}
