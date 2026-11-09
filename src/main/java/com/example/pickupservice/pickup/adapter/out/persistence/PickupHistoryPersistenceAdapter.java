package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.application.port.out.LoadHistoryPort;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class PickupHistoryPersistenceAdapter implements LoadHistoryPort {

	private static final List<SessionStatus> FINISHED = List.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED);

	private final PickupSessionJpaRepository pickupSessionJpaRepository;
	private final PickupOutcomeJpaRepository pickupOutcomeJpaRepository;

	@Override
	public List<HistoryItem> loadHistory(long childUserId, LocalDate from, LocalDate to) {
		List<PickupSessionEntity> sessions = pickupSessionJpaRepository
				.findByChildUserIdAndSessionStatusInAndDateBetween(childUserId, FINISHED, from, to);
		Map<Long, Instant> completedAts = pickupOutcomeJpaRepository.findAllById(sessions.stream()
						.filter(session -> session.getSessionStatus() == SessionStatus.COMPLETED)
						.map(PickupSessionEntity::getSessionId)
						.toList())
				.stream()
				.collect(Collectors.toMap(PickupOutcomeEntity::getSessionId, PickupOutcomeEntity::getCompletedAt));
		return sessions.stream()
				.map(session -> new HistoryItem(session.getDate(), session.getSessionId(), session.getSessionStatus(),
						completedAts.get(session.getSessionId())))
				.toList();
	}

}
