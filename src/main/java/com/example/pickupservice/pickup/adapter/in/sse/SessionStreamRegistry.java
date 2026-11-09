package com.example.pickupservice.pickup.adapter.in.sse;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.domain.SessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionStreamRegistry {

	private static final long NO_TIMEOUT = 0L;
	private static final String PING_COMMENT = " ping";

	private final JsonMapper jsonMapper;
	private final Map<Long, List<SseEmitter>> emittersBySession = new ConcurrentHashMap<>();

	public SseEmitter register(long sessionId) {
		SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
		emittersBySession.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>()).add(emitter);
		emitter.onCompletion(() -> unregister(sessionId, emitter));
		emitter.onTimeout(() -> unregister(sessionId, emitter));
		emitter.onError(error -> unregister(sessionId, emitter));
		return emitter;
	}

	public void unregister(long sessionId, SseEmitter emitter) {
		emittersBySession.computeIfPresent(sessionId, (id, emitters) -> {
			emitters.remove(emitter);
			return emitters.isEmpty() ? null : emitters;
		});
	}

	public void send(long sessionId, SessionEvent event) {
		List<SseEmitter> emitters = emittersBySession.get(sessionId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			sendTo(sessionId, emitter, event);
		}
	}

	/** 이어 받기와 새 이벤트가 같이 쓰는, 이벤트 한 건을 보내는 곳. 보내기에 실패한 emitter는 지운다. */
	public void sendTo(long sessionId, SseEmitter emitter, SessionEvent event) {
		try {
			emitter.send(SseEmitter.event()
					.name(event.getType().name())
					.id(String.valueOf(event.getSeq()))
					.data(toJson(event)));
		} catch (IOException | RuntimeException e) {
			log.debug("SSE 이벤트를 보내지 못해 연결을 지운다. sessionId={}", sessionId, e);
			unregister(sessionId, emitter);
		}
	}

	public void complete(long sessionId) {
		List<SseEmitter> emitters = emittersBySession.remove(sessionId);
		if (emitters == null) {
			return;
		}
		emitters.forEach(SseEmitter::complete);
	}

	@Scheduled(fixedDelayString = "${pickup.sse.ping-interval}")
	void ping() {
		emittersBySession.forEach((sessionId, emitters) -> {
			for (SseEmitter emitter : emitters) {
				try {
					emitter.send(SseEmitter.event().comment(PING_COMMENT));
				} catch (IOException | RuntimeException e) {
					log.debug("SSE ping을 보내지 못해 연결을 지운다. sessionId={}", sessionId, e);
					unregister(sessionId, emitter);
				}
			}
		});
	}

	private String toJson(SessionEvent event) {
		return jsonMapper.writeValueAsString(new EventData(
				event.getEventId(),
				event.getSeq(),
				event.getStateVersion(),
				TimeUtils.toKst(event.getOccurredAt()),
				event.getPayload()));
	}

	private record EventData(long eventId, long seq, int stateVersion, String occurredAt,
			Map<String, Object> payload) {
	}

}
