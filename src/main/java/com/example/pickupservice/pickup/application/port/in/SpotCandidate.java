package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

public record SpotCandidate(String name, LatLng location) {
}
