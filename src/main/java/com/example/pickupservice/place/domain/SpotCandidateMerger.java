package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpotCandidateMerger {

	private static final int MAX_CURB_STOPS = 4;
	private static final int MAX_CANDIDATES = 8;

	private SpotCandidateMerger() {
	}

	public static List<SpotCandidate> merge(LatLng academy, List<PublicParking> parkings, List<CurbStopCandidate> curbStops) {
		List<SpotCandidate> all = new ArrayList<>();
		for (PublicParking parking : parkings) {
			WalkTime walk = walkTime(academy, parking.location());
			all.add(new SpotCandidate(SpotKind.PUBLIC_PARKING, parking.name(), parking.location(),
					walk.walkMeters(), walk.walkSeconds(), null, false));
		}
		for (CurbStopCandidate curb : nearestPerRoad(academy, curbStops)) {
			WalkTime walk = walkTime(academy, curb.location());
			all.add(new SpotCandidate(SpotKind.CURB_STOP, curb.roadName(), curb.location(),
					walk.walkMeters(), walk.walkSeconds(), curb.lineKind(), curb.nearEnforcementCctv()));
		}
		return all.stream()
				.sorted(Comparator.comparingInt(SpotCandidate::walkMeters))
				.limit(MAX_CANDIDATES)
				.toList();
	}

	private static List<CurbStopCandidate> nearestPerRoad(LatLng academy, List<CurbStopCandidate> curbStops) {
		Map<String, CurbStopCandidate> nearestByRoad = new LinkedHashMap<>();
		for (CurbStopCandidate curb : curbStops) {
			String key = curb.roadName() == null ? "\0null" : curb.roadName();
			CurbStopCandidate current = nearestByRoad.get(key);
			if (current == null || distance(academy, curb) < distance(academy, current)) {
				nearestByRoad.put(key, curb);
			}
		}
		return nearestByRoad.values().stream()
				.sorted(Comparator.comparingDouble(curb -> distance(academy, curb)))
				.limit(MAX_CURB_STOPS)
				.toList();
	}

	private static double distance(LatLng academy, CurbStopCandidate curb) {
		return GeoPoints.distanceMeters(academy, curb.location());
	}

	private static WalkTime walkTime(LatLng academy, LatLng location) {
		return WalkTimeRule.fromStraightMeters(GeoPoints.distanceMeters(academy, location));
	}
}
