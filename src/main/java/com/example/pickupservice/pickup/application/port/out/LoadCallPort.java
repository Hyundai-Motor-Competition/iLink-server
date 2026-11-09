package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.domain.Call;

import java.util.Optional;

public interface LoadCallPort {

	Optional<Call> loadCall(long callId);

	Optional<Call> loadLiveCall(long sessionId);

	Optional<Call> loadLatestCall(long sessionId);

}
