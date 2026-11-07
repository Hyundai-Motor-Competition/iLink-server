package com.example.pickupservice.user.domain;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Vehicle {

	private final Long vehicleId;
	private Long familyId;
	private final String vin;
	private String model;
	private String plateNumber;
	private Instant unlinkedAt;
	private final Long version;

	public static Vehicle restore(Long vehicleId, Long familyId, String vin, String model, String plateNumber,
			Instant unlinkedAt) {
		return restore(vehicleId, familyId, vin, model, plateNumber, unlinkedAt, null);
	}

	public static Vehicle restore(Long vehicleId, Long familyId, String vin, String model, String plateNumber,
			Instant unlinkedAt, Long version) {
		return new Vehicle(vehicleId, familyId, vin, model, plateNumber, unlinkedAt, version);
	}

	public static Vehicle register(String vin, String model) {
		return new Vehicle(null, null, vin, model, null, null, null);
	}

	public void linkTo(long familyId) {
		this.familyId = familyId;
	}

	public void unlink(Instant unlinkedAt) {
		this.unlinkedAt = unlinkedAt;
	}

	public boolean isLinked() {
		return unlinkedAt == null;
	}
}
