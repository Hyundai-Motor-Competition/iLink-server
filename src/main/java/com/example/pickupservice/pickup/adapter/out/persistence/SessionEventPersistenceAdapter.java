package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.application.port.out.SessionEventPort;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SessionEventPersistenceAdapter implements SessionEventPort {

	private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
	};
	private static final String CHILD_USER_ID = "childUserId";

	private final SessionEventJpaRepository sessionEventJpaRepository;
	private final PickupSessionJpaRepository pickupSessionJpaRepository;
	private final JsonMapper jsonMapper;

	/** 세션 행을 잠근 뒤 그 세션의 마지막 seq + 1(없으면 1)로 저장한다. 동시 append가 같은 seq를 매기지 않게 한다. */
	@Override
	@Transactional
	public SessionEvent append(long sessionId, String type, int stateVersion, Instant at, Map<String, Object> payload) {
		pickupSessionJpaRepository.findForUpdateBySessionId(sessionId);
		long seq = sessionEventJpaRepository.findFirstBySessionIdOrderBySeqDesc(sessionId)
				.map(SessionEventEntity::getSeq)
				.orElse(0L) + 1;
		SessionEventEntity saved = sessionEventJpaRepository.save(
				new SessionEventEntity(null, sessionId, seq, type, stateVersion, at, toJson(payload)));
		return toDomain(saved);
	}

	@Override
	public SessionEventPage loadAfter(long sessionId, long afterSeq, int limit) {
		List<SessionEventEntity> rows = sessionEventJpaRepository.findBySessionIdAndSeqGreaterThanOrderBySeqAsc(
				sessionId, afterSeq, Limit.of(limit + 1));
		List<SessionEvent> items = rows.stream().limit(limit).map(this::toDomain).toList();
		return new SessionEventPage(items, rows.size() > limit);
	}

	/** 그 세션의 HELP_REQUESTED를 seq 큰 것부터 보며 payload childUserId가 같은 첫 건을 돌려준다. */
	@Override
	public Optional<SessionEvent> loadLastHelpRequest(long sessionId, long childUserId) {
		return sessionEventJpaRepository
				.findBySessionIdAndTypeOrderBySeqDesc(sessionId, SessionEventType.HELP_REQUESTED.name())
				.stream()
				.map(this::toDomain)
				.filter(event -> isChild(event.getPayload(), childUserId))
				.findFirst();
	}

	@Override
	public Optional<SessionEvent> loadFirst(long sessionId, String type) {
		return sessionEventJpaRepository.findFirstBySessionIdAndTypeOrderBySeqAsc(sessionId, type)
				.map(this::toDomain);
	}

	private static boolean isChild(Map<String, Object> payload, long childUserId) {
		return payload != null && payload.get(CHILD_USER_ID) instanceof Number id && id.longValue() == childUserId;
	}

	private SessionEvent toDomain(SessionEventEntity entity) {
		return SessionEvent.restore(entity.getEventId(), entity.getSessionId(), entity.getSeq(),
				SessionEventType.valueOf(entity.getType()), entity.getStateVersion(), entity.getOccurredAt(),
				toPayload(entity.getPayload()));
	}

	private String toJson(Map<String, Object> payload) {
		return payload == null ? null : jsonMapper.writeValueAsString(payload);
	}

	private Map<String, Object> toPayload(String json) {
		return json == null ? null : jsonMapper.readValue(json, PAYLOAD_TYPE);
	}

}
