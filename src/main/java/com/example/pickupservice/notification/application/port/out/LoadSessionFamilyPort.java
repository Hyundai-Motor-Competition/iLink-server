package com.example.pickupservice.notification.application.port.out;

import java.util.Optional;

public interface LoadSessionFamilyPort {

	Optional<Long> loadFamilyId(long sessionId);
}
