package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;

import java.time.Instant;
import java.util.List;

public interface DriveTimePort {

	List<Integer> driveSecondsNow(LatLng origin, List<LatLng> destinations);

	List<Integer> driveSecondsAt(LatLng origin, List<LatLng> destinations, Instant departAt);
}
