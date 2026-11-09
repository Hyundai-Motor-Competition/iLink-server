package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DistancePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostgisDistanceAdapter implements DistancePort {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public boolean isWithinMeters(LatLng from, LatLng to, int meters) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
				SELECT ST_DWithin(ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
				                  ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)
				""", Boolean.class, from.lng(), from.lat(), to.lng(), to.lat(), meters));
	}
}
