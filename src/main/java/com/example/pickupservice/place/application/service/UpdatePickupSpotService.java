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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePickupSpotService implements UpdatePickupSpotUseCase {

	private final LoadAcademyPort loadAcademyPort;
	private final LoadSpotPort loadSpotPort;
	private final SaveSpotPort saveSpotPort;
	private final DistancePort distancePort;

	@Override
	@Transactional
	public SpotView updateSpot(UpdateSpotCommand command) {
		PickupSpot current = OwnedSpotLookup.load(loadSpotPort, command.familyId(), command.spotId());
		if (current.isDefault() && Boolean.FALSE.equals(command.isDefault())) {
			throw PlaceErrors.defaultSpotRequired();
		}
		requireChangedPointsNearAcademy(current.getAcademyId(), command);

		PickupSpot updated = applyChanges(current, command);
		if (Boolean.TRUE.equals(command.isDefault())) {
			demoteOtherDefaults(updated);
		}
		return toView(saveSpotPort.saveSpot(updated));
	}

	/** 보낸(null 아닌) 좌표만 검사한다. 좌표를 안 보냈으면 학원도 읽지 않는다. */
	private void requireChangedPointsNearAcademy(long academyId, UpdateSpotCommand command) {
		if (command.stopPoint() == null && command.meetingPoint() == null) {
			return;
		}
		AcademyInfo academy = loadAcademyPort.loadAcademy(academyId)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		requireNearIfSent(academy.location(), command.stopPoint());
		requireNearIfSent(academy.location(), command.meetingPoint());
	}

	private void requireNearIfSent(LatLng academyLocation, LatLng point) {
		if (point != null) {
			AcademyDistanceCheck.requireNear(distancePort, academyLocation, point);
		}
	}

	private void demoteOtherDefaults(PickupSpot newDefault) {
		DefaultSpotRule.demotePreviousDefaults(newDefault,
				loadSpotPort.loadSpots(newDefault.getFamilyId(), newDefault.getAcademyId()))
				.forEach(saveSpotPort::saveSpot);
	}

	private static PickupSpot applyChanges(PickupSpot current, UpdateSpotCommand command) {
		return PickupSpot.restore(current.getSpotId(), current.getFamilyId(), current.getAcademyId(),
				valueOr(command.name(), current.getName()),
				valueOr(command.memo(), current.getMemo()),
				valueOr(command.stopPoint(), current.getStopPoint()),
				valueOr(command.meetingPoint(), current.getMeetingPoint()),
				valueOr(command.isDefault(), current.isDefault()),
				current.getCreatedBy());
	}

	private static SpotView toView(PickupSpot spot) {
		return new SpotView(spot.getSpotId(), spot.getName(), spot.getStopPoint(), spot.getMeetingPoint(),
				spot.getMemo(), spot.isDefault());
	}

	private static <T> T valueOr(T value, T fallback) {
		return value != null ? value : fallback;
	}
}
