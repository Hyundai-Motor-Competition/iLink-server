package com.example.pickupservice.pickup.adapter.in.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class ValidDoorCycleValidator implements ConstraintValidator<ValidDoorCycle, VehicleEventRequest> {

	private static final String DOOR_CYCLE = "DOOR_CYCLE";
	private static final Set<String> PASSENGER_DOORS = Set.of("FRONT_RIGHT", "REAR_LEFT", "REAR_RIGHT");

	@Override
	public boolean isValid(VehicleEventRequest request, ConstraintValidatorContext context) {
		if (request == null || !DOOR_CYCLE.equals(request.type()) || isPassengerDoor(request.door())) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode("door")
				.addConstraintViolation();
		return false;
	}

	private static boolean isPassengerDoor(String door) {
		return door != null && PASSENGER_DOORS.contains(door);
	}

}
