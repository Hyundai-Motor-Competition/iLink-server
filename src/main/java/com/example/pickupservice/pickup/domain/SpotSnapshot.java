package com.example.pickupservice.pickup.domain;

import com.example.pickupservice.common.geo.LatLng;

public record SpotSnapshot(long spotId, String name, LatLng stopPoint, LatLng meetingPoint) {
}
