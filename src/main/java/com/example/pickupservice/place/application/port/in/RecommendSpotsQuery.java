package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;

import java.time.Instant;

public record RecommendSpotsQuery(long academyId, LatLng origin, Instant arriveAt) {
}
