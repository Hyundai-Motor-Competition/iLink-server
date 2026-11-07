package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;

public interface DistancePort {

	boolean isWithinMeters(LatLng from, LatLng to, int meters);
}
