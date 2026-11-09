package com.example.pickupservice.place.application.service;

import com.example.pickupservice.place.application.port.in.DeletePickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.DeleteSpotCommand;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import com.example.pickupservice.place.domain.SpotDeletionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePickupSpotService implements DeletePickupSpotUseCase {

	private final LoadSpotPort loadSpotPort;
	private final SaveSpotPort saveSpotPort;
	private final CheckActivePickupPort checkActivePickupPort;

	@Override
	@Transactional
	public void deleteSpot(DeleteSpotCommand command) {
		PickupSpot target = OwnedSpotLookup.load(loadSpotPort, command.familyId(), command.spotId());
		if (checkActivePickupPort.isUsedByActiveSession(target.getSpotId())) {
			throw PlaceErrors.spotInUse();
		}
		if (!SpotDeletionRule.canDelete(target,
				loadSpotPort.loadSpots(target.getFamilyId(), target.getAcademyId()))) {
			throw PlaceErrors.defaultSpotRequired();
		}
		saveSpotPort.deleteSpot(target.getSpotId());
	}
}
