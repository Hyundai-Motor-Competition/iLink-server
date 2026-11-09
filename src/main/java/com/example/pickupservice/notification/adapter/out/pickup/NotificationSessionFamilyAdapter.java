package com.example.pickupservice.notification.adapter.out.pickup;

import com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort;
import com.example.pickupservice.pickup.application.port.in.FindSessionFamilyUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationSessionFamilyAdapter implements LoadSessionFamilyPort {

	private final FindSessionFamilyUseCase findSessionFamilyUseCase;

	@Override
	public Optional<Long> loadFamilyId(long sessionId) {
		return findSessionFamilyUseCase.findFamilyId(sessionId);
	}
}
