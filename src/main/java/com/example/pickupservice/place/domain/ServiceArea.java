package com.example.pickupservice.place.domain;

import java.util.List;

import com.example.pickupservice.common.geo.LatLng;

public record ServiceArea(List<LatLng> outerRing) {

	public boolean contains(LatLng point) {
		boolean inside = false;
		int n = outerRing.size();
		for (int i = 0, j = n - 1; i < n; j = i++) {
			LatLng a = outerRing.get(i);
			LatLng b = outerRing.get(j);
			if ((a.lat() > point.lat()) != (b.lat() > point.lat())
					&& point.lng() < (b.lng() - a.lng()) * (point.lat() - a.lat()) / (b.lat() - a.lat()) + a.lng()) {
				inside = !inside;
			}
		}
		return inside;
	}
}
