package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AcademySpots;
import com.example.pickupservice.place.application.port.in.ListPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.ListSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotGroups;
import com.example.pickupservice.place.application.port.in.SpotItem;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.domain.BlockDetourDistance;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListPickupSpotsService implements ListPickupSpotsUseCase {

	private static final double WALK_METERS_PER_SECOND = 1.1;

	private final LoadSpotPort loadSpotPort;
	private final LoadAcademyPort loadAcademyPort;
	private final CheckActivePickupPort checkActivePickupPort;

	@Override
	public SpotGroups listSpots(ListSpotsQuery query) {
		List<PickupSpot> spots = (query.near() != null && query.radiusM() != null)
				? loadSpotPort.loadSpotsWithin(query.familyId(), query.academyId(), query.near(), query.radiusM())
				: loadSpotPort.loadSpots(query.familyId(), query.academyId());

		Map<Long, List<PickupSpot>> byAcademy = spots.stream()
				.collect(Collectors.groupingBy(PickupSpot::getAcademyId, TreeMap::new, Collectors.toList()));

		List<AcademySpots> academies = byAcademy.entrySet().stream()
				.map(entry -> toAcademySpots(entry.getKey(), entry.getValue()))
				.toList();
		return new SpotGroups(academies);
	}

	private AcademySpots toAcademySpots(long academyId, List<PickupSpot> spots) {
		AcademyInfo academy = loadAcademyPort.loadAcademy(academyId).orElse(null);
		String name = academy == null ? null : academy.name();
		LatLng origin = academy == null ? null : (academy.entrance() != null ? academy.entrance() : academy.location());
		return new AcademySpots(academyId, name, toItems(spots, origin));
	}

	private static List<SpotItem> toItems(List<PickupSpot> spots, LatLng origin) {
		return spots.stream()
				.sorted(Comparator.comparing(PickupSpot::getSpotId))
				.map(spot -> new SpotItem(spot.getSpotId(), spot.getName(), spot.getStopPoint(),
						spot.getMeetingPoint(), spot.getMemo(), spot.isDefault(),
						walkMinutes(origin, spot.getMeetingPoint())))
				.toList();
	}

	private static Integer walkMinutes(LatLng origin, LatLng meetingPoint) {
		if (origin == null || meetingPoint == null) {
			return null;
		}
		double meters = BlockDetourDistance.meters(origin, meetingPoint);
		return (int) Math.ceil(meters / WALK_METERS_PER_SECOND / 60);
	}
}
