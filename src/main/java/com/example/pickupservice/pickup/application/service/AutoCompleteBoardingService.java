package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.AutoCompleteBoardingUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveSessionPort;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// 10초 자동 완료 작업: 취소 창이 지나고 세션 자녀가 모두 탄 ACTIVE 세션을 boardingMethod DOOR로 완료
// 세션마다 따로 트랜잭션을 열어 한 세션의 실패가 다른 세션 완료를 막지 않는다
@Slf4j
@Service
public class AutoCompleteBoardingService implements AutoCompleteBoardingUseCase {

	private final LoadSessionPort loadSessionPort;
	private final SaveSessionPort saveSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final PickupCompletion pickupCompletion;
	private final TransactionOperations transactionOperations;

	@Autowired
	public AutoCompleteBoardingService(LoadSessionPort loadSessionPort, SaveSessionPort saveSessionPort,
			LoadFamilyPort loadFamilyPort, PickupCompletion pickupCompletion,
			TransactionOperations transactionOperations) {
		this.loadSessionPort = loadSessionPort;
		this.saveSessionPort = saveSessionPort;
		this.loadFamilyPort = loadFamilyPort;
		this.pickupCompletion = pickupCompletion;
		this.transactionOperations = transactionOperations;
	}

	AutoCompleteBoardingService(LoadSessionPort loadSessionPort, SaveSessionPort saveSessionPort,
			LoadFamilyPort loadFamilyPort, PickupCompletion pickupCompletion) {
		this(loadSessionPort, saveSessionPort, loadFamilyPort, pickupCompletion,
				TransactionOperations.withoutTransaction());
	}

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

	// 목록을 읽은 뒤 SES-11 완료 · SES-12 탑승 취소가 있었을 수 있어 잠가 다시 읽고 확인한다
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
