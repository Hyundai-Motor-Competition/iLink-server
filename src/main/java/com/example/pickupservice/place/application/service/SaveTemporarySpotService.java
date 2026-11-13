package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.place.application.port.in.SaveTemporarySpotCommand;
import com.example.pickupservice.place.application.port.in.SaveTemporarySpotUseCase;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveTemporarySpotService implements SaveTemporarySpotUseCase {

	private static final int MAX_NAME_LENGTH = 50;

	private final SaveSpotPort saveSpotPort;

	@Override
	@Transactional
	public long saveTemporarySpot(SaveTemporarySpotCommand c) {
		if (c.name() == null || c.name().isBlank() || c.name().length() > MAX_NAME_LENGTH) {
			throw BusinessException.invalidField("name", "1~50자여야 합니다");
		}
		PickupSpot spot = PickupSpot.createTemporary(c.familyId(), c.academyId(), c.name(), c.stopPoint(),
				c.meetingPoint());
		return saveSpotPort.saveSpot(spot).getSpotId();
	}
}
