package com.example.pickupservice.place.application.service;

import com.example.pickupservice.place.application.port.in.AcademySpots;
import com.example.pickupservice.place.application.port.in.ListPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.ListSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotGroups;
import com.example.pickupservice.place.application.port.in.SpotItem;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListPickupSpotsService implements ListPickupSpotsUseCase {

	private final LoadSpotPort loadSpotPort;
	private final LoadAcademyPort loadAcademyPort;
	private final CheckActivePickupPort checkActivePickupPort;

	@Override
	public SpotGroups listSpots(ListSpotsQuery query) {
		List<PickupSpot> spots = (query.near() != null && query.radiusM() != null)
				? loadSpotPort.loadSpotsWithin(query.familyId(), query.academyId(), query.near(), query.radiusM())
				: loadSpotPort.loadSpots(query.familyId(), query.academyId());

		// 진행 중 세션의 자녀 위치. 걷는 시간 계산 방법이 정해지기 전까지 walkMinutes는 늘 null.
		checkActivePickupPort.loadActiveChildLocation(query.familyId());

		Map<Long, List<PickupSpot>> byAcademy = spots.stream()
				.collect(Collectors.groupingBy(PickupSpot::getAcademyId, TreeMap::new, Collectors.toList()));

		List<AcademySpots> academies = byAcademy.entrySet().stream()
				.map(entry -> new AcademySpots(entry.getKey(), academyName(entry.getKey()), toItems(entry.getValue())))
				.toList();
		return new SpotGroups(academies);
	}

	private String academyName(long academyId) {
		return loadAcademyPort.loadAcademy(academyId).map(AcademyInfo::name).orElse(null);
	}

	private static List<SpotItem> toItems(List<PickupSpot> spots) {
		return spots.stream()
				.sorted(Comparator.comparing(PickupSpot::getSpotId))
				.map(spot -> new SpotItem(spot.getSpotId(), spot.getName(), spot.getStopPoint(),
						spot.getMeetingPoint(), spot.getMemo(), spot.isDefault(), null))
				.toList();
	}
}
