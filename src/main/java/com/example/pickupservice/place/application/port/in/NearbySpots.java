package com.example.pickupservice.place.application.port.in;

import java.util.List;

public record NearbySpots(boolean supported, List<NearbySpot> items) {
}
