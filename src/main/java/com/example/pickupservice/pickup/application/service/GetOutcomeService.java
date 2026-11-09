package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.CorrectionView;
import com.example.pickupservice.pickup.application.port.in.GetOutcomeUseCase;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.out.LoadOutcomePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetOutcomeService implements GetOutcomeUseCase {

	private final LoadSessionPort loadSessionPort;
	private final LoadOutcomePort loadOutcomePort;

	// 볼 수 없는 세션이거나 결과가 아직 없으면(완료 전) NOT_FOUND
	@Override
	public OutcomeView getOutcome(SessionQuery query) {
		PickupSession session = loadSessionPort.load(query.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, query.principal());
		PickupOutcome outcome = loadOutcomePort.loadOutcome(session.getSessionId())
				.orElseThrow(PickupErrors::notFound);
		return outcomeView(session, outcome);
	}

	// OUT-02 · OUT-03이 같은 모양으로 돌려준다
	static OutcomeView outcomeView(PickupSession session, PickupOutcome outcome) {
		return new OutcomeView(session.getSessionId(), outcome.getCompletedAt(), session.getSpotName(),
				outcome.getBoardingMethod() == null ? null : outcome.getBoardingMethod().name(),
				outcome.getPredictedStopAt(), outcome.getActualStopAt(), outcome.diffSeconds().orElse(null),
				correctionOf(outcome));
	}

	// 정정이 없으면 status · correctedStopAt 모두 null
	private static CorrectionView correctionOf(PickupOutcome outcome) {
		String status = outcome.getCorrectionStatus() == null ? null : outcome.getCorrectionStatus().name();
		return new CorrectionView(status, outcome.getCorrectedStopAt());
	}

}
