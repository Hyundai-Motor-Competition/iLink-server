package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.PickupProgress;
import com.example.pickupservice.pickup.application.port.in.PickupProgressQueryUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PickupProgressQueryService implements PickupProgressQueryUseCase {

	private final LoadSessionPort loadSessionPort;

	@Override
	public Map<Long, PickupProgress> findProgressBySchedule(long familyId, LocalDate date) {
		Map<Long, PickupProgress> progress = new HashMap<>();
		for (PickupSession session : loadSessionPort.loadByFamilyAndDate(familyId, date)) {
			PickupProgress sessionProgress = progressOf(session.getSessionStatus());
			if (sessionProgress == null) {
				continue;
			}
			for (SessionChild child : session.getChildren()) {
				progress.merge(child.getScheduleId(), sessionProgress, PickupProgressQueryService::higher);
			}
		}
		return progress;
	}

	private static PickupProgress progressOf(SessionStatus status) {
		return switch (status) {
			case ACTIVE, ON_HOLD -> PickupProgress.IN_PROGRESS;
			case COMPLETED -> PickupProgress.COMPLETED;
			case CANCELLED -> null;
		};
	}

	private static PickupProgress higher(PickupProgress a, PickupProgress b) {
		return a == PickupProgress.COMPLETED || b == PickupProgress.COMPLETED
				? PickupProgress.COMPLETED
				: PickupProgress.IN_PROGRESS;
	}

}
