package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.application.port.out.LoadOutcomePort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.domain.CorrectionReason;
import com.example.pickupservice.pickup.domain.CorrectionStatus;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class PickupOutcomePersistenceAdapter implements LoadOutcomePort, SaveOutcomePort {

	private final PickupOutcomeJpaRepository pickupOutcomeJpaRepository;

	@Override
	public Optional<PickupOutcome> loadOutcome(long sessionId) {
		return pickupOutcomeJpaRepository.findById(sessionId).map(PickupOutcomePersistenceAdapter::toDomain);
	}

	@Override
	public PickupOutcome saveOutcome(PickupOutcome outcome) {
		return toDomain(pickupOutcomeJpaRepository.save(toEntity(outcome)));
	}

	private static PickupOutcome toDomain(PickupOutcomeEntity entity) {
		return PickupOutcome.restore(entity.getSessionId(), entity.getCompletedAt(), entity.getBoardingMethod(),
				entity.getPredictedStopAt(), entity.getActualStopAt(), entity.getCorrectedStopAt(),
				entity.getCorrectionReason() == null ? null : CorrectionReason.valueOf(entity.getCorrectionReason()),
				entity.getCorrectionStatus() == null ? null : CorrectionStatus.valueOf(entity.getCorrectionStatus()),
				entity.getCorrectedBy(), entity.getCorrectionNote());
	}

	private static PickupOutcomeEntity toEntity(PickupOutcome outcome) {
		return new PickupOutcomeEntity(outcome.getSessionId(), outcome.getCompletedAt(), outcome.getBoardingMethod(),
				outcome.getPredictedStopAt(), outcome.getActualStopAt(), outcome.getCorrectedStopAt(),
				outcome.getCorrectionReason() == null ? null : outcome.getCorrectionReason().name(),
				outcome.getCorrectionStatus() == null ? null : outcome.getCorrectionStatus().name(),
				outcome.getCorrectedBy(), outcome.getCorrectionNote());
	}

}
