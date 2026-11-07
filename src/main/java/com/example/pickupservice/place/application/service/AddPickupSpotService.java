package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AddPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.AddSpotCommand;
import com.example.pickupservice.place.application.port.in.SpotCreated;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.DistancePort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.DefaultSpotRule;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class AddPickupSpotService implements AddPickupSpotUseCase {

	private static final int MAX_DISTANCE_METERS = 500;

	private final LoadAcademyPort loadAcademyPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveSpotPort saveSpotPort;
	private final DistancePort distancePort;

	@Override
	@Transactional
	public SpotCreated addSpot(AddSpotCommand command) {
		AcademyInfo academy = loadAcademyPort.loadAcademy(command.academyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		requireNearAcademy(academy.location(), command.stopPoint());
		requireNearAcademy(academy.location(), command.meetingPoint());

		List<PickupSpot> existingSpots = loadSpotPort.loadSpots(command.familyId(), command.academyId());
		PickupSpot candidate = PickupSpot.create(command.familyId(), command.academyId(), command.name(),
				command.memo(), command.stopPoint(), command.meetingPoint(), command.isDefault(), command.createdBy());
		PickupSpot newSpot = DefaultSpotRule.applyFirstSpotDefault(candidate, existingSpots);

		PickupSpot saved = saveSpotPort.saveSpot(newSpot);
		DefaultSpotRule.demotePreviousDefaults(newSpot, existingSpots).forEach(saveSpotPort::saveSpot);
		return new SpotCreated(saved.getSpotId(), saved.isDefault());
	}

	private void requireNearAcademy(LatLng academyLocation, LatLng point) {
		if (!distancePort.isWithinMeters(academyLocation, point, MAX_DISTANCE_METERS)) {
			throw PlaceErrors.spotTooFar();
		}
	}
}
