package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.application.port.out.LoadCallPort;
import com.example.pickupservice.pickup.application.port.out.SaveCallPort;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.CallStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CallPersistenceAdapter implements LoadCallPort, SaveCallPort {

	private final CallJpaRepository callJpaRepository;
	private final EntityManager entityManager;

	@Override
	public Optional<Call> loadCall(long callId) {
		return callJpaRepository.findById(callId).map(this::refreshed).map(CallPersistenceAdapter::toDomain);
	}

	private CallEntity refreshed(CallEntity entity) {
		if (TransactionSynchronizationManager.isActualTransactionActive() && entityManager.contains(entity)) {
			entityManager.refresh(entity);
		}
		return entity;
	}

	@Override
	public Optional<Call> loadLiveCall(long sessionId) {
		return callJpaRepository.findFirstBySessionIdAndStatusNotOrderByCallIdDesc(sessionId, CallStatus.REVOKED)
				.map(CallPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<Call> loadLatestCall(long sessionId) {
		return callJpaRepository.findFirstBySessionIdOrderByCallIdDesc(sessionId).map(CallPersistenceAdapter::toDomain);
	}

	@Override
	@Transactional
	public Call saveCall(Call call) {
		if (call.getCallId() == null) {
			return toDomain(callJpaRepository.save(toEntity(call)));
		}
		Instant acceptedAt = callJpaRepository.findById(call.getCallId()).orElseThrow().getCreatedAt();
		return toDomain(callJpaRepository.saveAndFlush(toEntity(call)), acceptedAt);
	}

	private static Call toDomain(CallEntity entity) {
		return toDomain(entity, entity.getCreatedAt());
	}

	private static Call toDomain(CallEntity entity, Instant acceptedAt) {
		return Call.restore(entity.getCallId(), entity.getSessionId(), entity.getPrevCallId(),
				entity.getTriggerType(), entity.getStatus(), entity.getSpotVersion(), acceptedAt,
				entity.getSentAt(), entity.getDeliveredAt(), entity.getAcknowledgedAt(), entity.getRevokedAt(),
				entity.getRevokeReason());
	}

	private static CallEntity toEntity(Call call) {
		return new CallEntity(call.getCallId(), call.getSessionId(), call.getPrevCallId(), call.getTriggerType(),
				call.getStatus(), call.getSpotVersion(), call.getSentAt(), call.getDeliveredAt(),
				call.getAcknowledgedAt(), call.getRevokedAt(), call.getRevokeReason());
	}

}
