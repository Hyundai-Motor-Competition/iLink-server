package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.NearbySpot;
import com.example.pickupservice.place.application.port.in.NearbySpots;

import java.util.List;

public record NearbySpotsResponse(boolean supported, List<Item> items) {

	public static NearbySpotsResponse from(NearbySpots spots) {
		return new NearbySpotsResponse(spots.supported(), spots.items().stream().map(Item::from).toList());
	}

	public record Item(String name, LatLng location, int walkMeters, int walkSeconds) {

		static Item from(NearbySpot spot) {
			return new Item(spot.name(), spot.location(), spot.walkMeters(), spot.walkSeconds());
		}
	}
}
