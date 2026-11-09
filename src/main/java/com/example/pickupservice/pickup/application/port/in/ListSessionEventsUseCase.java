package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;

public interface ListSessionEventsUseCase {

	SessionEventPage listEvents(long sessionId, long afterSeq, int limit, AuthPrincipal principal);

}
