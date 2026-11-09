package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.domain.SessionEvent;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface SessionEventPort {

	SessionEvent append(long sessionId, String type, int stateVersion, Instant at, Map<String, Object> payload);

	SessionEventPage loadAfter(long sessionId, long afterSeq, int limit);

	Optional<SessionEvent> loadLastHelpRequest(long sessionId, long childUserId);

	Optional<SessionEvent> loadFirst(long sessionId, String type);

}
