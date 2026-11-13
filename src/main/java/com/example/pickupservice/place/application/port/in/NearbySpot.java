package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record NearbySpot(String name, LatLng location, int walkMeters, int walkSeconds) {
}
