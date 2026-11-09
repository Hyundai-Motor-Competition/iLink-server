package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.pickup.application.port.out.ProcessedVehicleEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProcessedVehicleEventPersistenceAdapter implements ProcessedVehicleEventPort {

	private final ProcessedVehicleEventJpaRepository processedVehicleEventJpaRepository;

	@Override
	public boolean markProcessed(long sessionId, String vehicleEventId) {
		return processedVehicleEventJpaRepository.insertIfAbsent(vehicleEventId, sessionId) == 1;
	}

}
