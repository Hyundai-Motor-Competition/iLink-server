package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.in.FindSessionFamilyUseCase;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindSessionFamilyService implements FindSessionFamilyUseCase {

	private final LoadSessionPort loadSessionPort;

	@Override
	public Optional<Long> findFamilyId(long sessionId) {
		return loadSessionPort.load(sessionId).map(PickupSession::getFamilyId);
	}

}
