package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.PickupSchedule;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class SetupChecks {

	private SetupChecks() {
	}

	static List<Long> academyIdsOf(List<PickupSchedule> schedules) {
		return schedules.stream()
				.map(schedule -> schedule.getAcademy().getAcademyId())
				.distinct()
				.toList();
	}

	static boolean lacksDefaultSpot(List<SpotSummary> spots, List<Long> academyIds) {
		Set<Long> academiesWithDefaultSpot = spots.stream()
				.filter(SpotSummary::isDefault)
				.map(SpotSummary::academyId)
				.collect(Collectors.toSet());
		return !academiesWithDefaultSpot.containsAll(academyIds);
	}
}
