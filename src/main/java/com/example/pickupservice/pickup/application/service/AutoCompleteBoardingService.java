package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.AutoCompleteBoardingUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCompleteBoardingService implements AutoCompleteBoardingUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final PickupCompletion pickupCompletion;
	private final TransactionOperations transactionOperations;

	@Override
	public int completeExpiredBoardings(Instant now) {
		int completed = 0;
		for (PickupSession expired : loadSessionPort.loadBoardingExpired(now)) {
			if (completeInOwnTransaction(expired.getSessionId(), now)) {
				completed++;
			}
		}
		return completed;
	}

	private boolean completeInOwnTransaction(long sessionId, Instant now) {
		try {
			return Boolean.TRUE.equals(transactionOperations.execute(status -> completeIfAllBoarded(sessionId, now)));
		} catch (RuntimeException exception) {
			log.warn("auto complete failed sessionId={}", sessionId, exception);
			return false;
		}
	}

	private boolean completeIfAllBoarded(long sessionId, Instant now) {
		Optional<PickupSession> found = loadSessionPort.loadForUpdate(sessionId);
		if (found.isEmpty() || !canAutoComplete(found.get())) {
			return false;
		}
		PickupSession locked = found.get();
		locked.complete(BoardingMethod.DOOR, List.of(), now);
		PickupSession session = saveSessionPort.save(locked);
		pickupCompletion.record(session, BoardingMethod.DOOR, now, loadFamilyPort.loadMembers(session.getFamilyId()));
		return true;
	}

	private static boolean canAutoComplete(PickupSession session) {
		return session.getSessionStatus() == SessionStatus.ACTIVE
				&& session.getChildren().stream().allMatch(child -> child.getBoardedAt() != null);
	}

}
