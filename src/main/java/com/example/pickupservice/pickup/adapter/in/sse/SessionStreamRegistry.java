package com.example.pickupservice.pickup.adapter.in.sse;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.pickup.application.port.out.SessionEventMessage;
import com.example.pickupservice.pickup.domain.SessionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
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

	public void send(SessionEventMessage message) {
		sendToAll(message.sessionId(), toFrame(message));
	}

	public void sendTo(long sessionId, SseEmitter emitter, SessionEvent event) {
		sendFrame(sessionId, emitter, toFrame(event));
	}

	private void sendToAll(long sessionId, Frame frame) {
		List<SseEmitter> emitters = emittersBySession.get(sessionId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			sendFrame(sessionId, emitter, frame);
		}
	}

	private void sendFrame(long sessionId, SseEmitter emitter, Frame frame) {
		try {
			SseEmitter.SseEventBuilder builder = SseEmitter.event().name(frame.name());
			if (frame.seq() != null) {
				builder.id(String.valueOf(frame.seq()));
			}
			emitter.send(builder.data(frame.data()));
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

	@Scheduled(fixedDelayString = "${pickup.sse.ping-interval}",
			initialDelayString = "${pickup.sse.ping-interval}")
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

	private Frame toFrame(SessionEvent event) {
		return toFrame(event.getType().name(), event.getEventId(), event.getSeq(), event.getStateVersion(),
				event.getOccurredAt(), event.getPayload());
	}

	private Frame toFrame(SessionEventMessage message) {
		return toFrame(message.type(), message.eventId(), message.seq(), message.stateVersion(),
				message.occurredAt(), message.payload());
	}

	private Frame toFrame(String name, Long eventId, Long seq, int stateVersion, Instant occurredAt,
			Map<String, Object> payload) {
		String data = jsonMapper.writeValueAsString(new EventData(eventId, seq, stateVersion,
				TimeUtils.toKst(occurredAt), payload));
		return new Frame(name, seq, data);
	}

	private record Frame(String name, Long seq, String data) {
	}

	private record EventData(Long eventId, Long seq, int stateVersion, String occurredAt,
			Map<String, Object> payload) {
	}

}
