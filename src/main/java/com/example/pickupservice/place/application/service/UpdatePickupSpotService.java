package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.SpotView;
import com.example.pickupservice.place.application.port.in.UpdatePickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.UpdateSpotCommand;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.DistancePort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.DefaultSpotRule;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class UpdatePickupSpotService implements UpdatePickupSpotUseCase {

	private static final int MAX_DISTANCE_METERS = 500;

	private final LoadAcademyPort loadAcademyPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveSpotPort saveSpotPort;
	private final DistancePort distancePort;

	@Override
	@Transactional
	public SpotView updateSpot(UpdateSpotCommand command) {
		PickupSpot current = loadSpotPort.loadSpot(command.spotId())
				.filter(spot -> spot.getFamilyId() == command.familyId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));

		if (current.isDefault() && Boolean.FALSE.equals(command.isDefault())) {
			throw PlaceErrors.defaultSpotRequired();
		}

		if (command.stopPoint() != null || command.meetingPoint() != null) {
			AcademyInfo academy = loadAcademyPort.loadAcademy(current.getAcademyId())
					.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
			requireNearAcademy(academy.location(), command.stopPoint());
			requireNearAcademy(academy.location(), command.meetingPoint());
		}

		PickupSpot updated = PickupSpot.restore(current.getSpotId(), current.getFamilyId(), current.getAcademyId(),
				valueOr(command.name(), current.getName()),
				valueOr(command.memo(), current.getMemo()),
				valueOr(command.stopPoint(), current.getStopPoint()),
				valueOr(command.meetingPoint(), current.getMeetingPoint()),
				valueOr(command.isDefault(), current.isDefault()),
				current.getCreatedBy());

		if (Boolean.TRUE.equals(command.isDefault())) {
			DefaultSpotRule.demotePreviousDefaults(updated,
					loadSpotPort.loadSpots(updated.getFamilyId(), updated.getAcademyId()))
					.forEach(saveSpotPort::saveSpot);
		}
		PickupSpot saved = saveSpotPort.saveSpot(updated);
		return new SpotView(saved.getSpotId(), saved.getName(), saved.getStopPoint(), saved.getMeetingPoint(),
				saved.getMemo(), saved.isDefault());
	}

	private void requireNearAcademy(LatLng academyLocation, LatLng point) {
		if (point != null && !distancePort.isWithinMeters(academyLocation, point, MAX_DISTANCE_METERS)) {
			throw PlaceErrors.spotTooFar();
		}
	}

	private static <T> T valueOr(T value, T fallback) {
		return value != null ? value : fallback;
	}
}
