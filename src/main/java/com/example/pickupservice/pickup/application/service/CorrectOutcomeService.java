package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeCommand;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeUseCase;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;
import com.example.pickupservice.pickup.application.port.out.LoadOutcomePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.domain.CorrectionReason;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Transactional
@RequiredArgsConstructor
public class CorrectOutcomeService implements CorrectOutcomeUseCase {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final LoadSessionPort loadSessionPort;
	private final LoadOutcomePort loadOutcomePort;
	private final SaveOutcomePort saveOutcomePort;

	@Override
	public OutcomeView correct(CorrectOutcomeCommand command) {
		PickupSession session = loadSessionPort.load(command.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, command.principal());
		PickupOutcome outcome = loadOutcomePort.loadOutcome(session.getSessionId())
				.orElseThrow(PickupErrors::notFound);
		if (!LocalDate.ofInstant(command.correctedStopAt(), SEOUL).equals(session.getDate())) {
			throw BusinessException.invalidField("correctedStopAt", "세션 날짜(한국 시간) 안이어야 합니다");
		}
		outcome.correct(command.correctedStopAt(), CorrectionReason.valueOf(command.reasonCode()),
				command.principal().id(), command.note());
		PickupOutcome saved = saveOutcomePort.saveOutcome(outcome);
		return GetOutcomeService.outcomeView(session, saved);
	}

}
