package com.example.pickupservice.place.application.service;

import com.example.pickupservice.place.application.port.in.SaveTemporarySpotCommand;
import com.example.pickupservice.place.application.port.in.SaveTemporarySpotUseCase;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaveTemporarySpotService implements SaveTemporarySpotUseCase {

	private final SaveSpotPort saveSpotPort;

	@Override
	public long saveTemporarySpot(SaveTemporarySpotCommand command) {
		PickupSpot spot = PickupSpot.createTemporary(command.familyId(), command.academyId(), command.name(),
				command.stopPoint(), command.meetingPoint(), command.createdBy());
		return saveSpotPort.saveSpot(spot).getSpotId();
	}
}
