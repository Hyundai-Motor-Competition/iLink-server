package com.example.pickupservice.schedule.adapter.out.pickup;

import com.example.pickupservice.pickup.application.port.in.PickupProgress;
import com.example.pickupservice.pickup.application.port.in.PickupProgressQueryUseCase;
import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;
import com.example.pickupservice.schedule.application.port.out.LoadPickupProgressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class SchedulePickupProgressAdapter implements LoadPickupProgressPort {

	private final PickupProgressQueryUseCase pickupProgressQueryUseCase;

	@Override
	public Map<Long, TodayPickupStatus> loadProgress(long familyId, LocalDate date) {
		return pickupProgressQueryUseCase.findProgressBySchedule(familyId, date).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> toStatus(entry.getValue())));
	}

	private static TodayPickupStatus toStatus(PickupProgress progress) {
		return switch (progress) {
			case IN_PROGRESS -> TodayPickupStatus.IN_PROGRESS;
			case COMPLETED -> TodayPickupStatus.COMPLETED;
		};
	}
}
