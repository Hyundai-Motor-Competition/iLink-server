package com.example.pickupservice.user.domain;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleTest {

	private static final String VIN = "KMHKN81BFNU123456";

	@Test
	@DisplayName("Vehicle.unlink를 부르면 unlinkedAt이 채워지고 vehicleId · vin은 그대로다")
	void unlinkFillsUnlinkedAtAndKeepsVehicleIdAndVin() {
		Vehicle vehicle = Vehicle.restore(77L, 12L, VIN, "아이오닉 5", "12가 3456", null);
		Instant unlinkedAt = Instant.parse("2026-11-07T09:30:00Z");

		vehicle.unlink(unlinkedAt);

		assertThat(vehicle.getUnlinkedAt()).isEqualTo(unlinkedAt);
		assertThat(vehicle.getVehicleId()).isEqualTo(77L);
		assertThat(vehicle.getVin()).isEqualTo(VIN);
	}

	@Test
	@DisplayName("Vehicle.unlink를 불러도 familyId · model · plateNumber는 그대로다")
	void unlinkKeepsFamilyIdModelAndPlateNumber() {
		Vehicle vehicle = Vehicle.restore(77L, 12L, VIN, "아이오닉 5", "12가 3456", null);

		vehicle.unlink(Instant.parse("2026-11-07T09:30:00Z"));

		assertThat(vehicle.getFamilyId()).isEqualTo(12L);
		assertThat(vehicle.getModel()).isEqualTo("아이오닉 5");
		assertThat(vehicle.getPlateNumber()).isEqualTo("12가 3456");
	}

	@Test
	@DisplayName("unlinkedAt이 채워진 Vehicle은 연결 중이 아니다")
	void vehicleWithUnlinkedAtIsNotLinked() {
		Vehicle vehicle = Vehicle.restore(77L, 12L, VIN, "아이오닉 5", "12가 3456",
				Instant.parse("2026-11-07T09:30:00Z"));

		assertThat(vehicle.isLinked()).isFalse();
	}

	@Test
	@DisplayName("Vehicle.unlink를 부른 뒤에는 연결 중이 아니다")
	void vehicleIsNotLinkedAfterUnlink() {
		Vehicle vehicle = Vehicle.restore(77L, 12L, VIN, "아이오닉 5", "12가 3456", null);

		vehicle.unlink(Instant.parse("2026-11-07T09:30:00Z"));

		assertThat(vehicle.isLinked()).isFalse();
	}

	@Test
	@DisplayName("unlinkedAt이 빈 Vehicle은 연결 중이다")
	void vehicleWithoutUnlinkedAtIsLinked() {
		Vehicle vehicle = Vehicle.restore(77L, 12L, VIN, "아이오닉 5", "12가 3456", null);

		assertThat(vehicle.isLinked()).isTrue();
	}

	@Test
	@DisplayName("Vehicle.register는 familyId · unlinkedAt이 빈 Vehicle을 만들고 Vehicle.linkTo는 familyId를 채운다")
	void registerCreatesVehicleWithoutFamilyIdAndUnlinkedAtAndLinkToFillsFamilyId() {
		Vehicle vehicle = Vehicle.register(VIN, "아이오닉 5");

		assertThat(vehicle.getFamilyId()).isNull();
		assertThat(vehicle.getUnlinkedAt()).isNull();

		vehicle.linkTo(12L);

		assertThat(vehicle.getFamilyId()).isEqualTo(12L);
		assertThat(vehicle.getUnlinkedAt()).isNull();
	}

	@Test
	@DisplayName("Vehicle.register는 vehicleId · plateNumber가 빈 Vehicle을 만들고 vin · model은 받은 값 그대로다")
	void registerKeepsVinAndModelAndLeavesVehicleIdAndPlateNumberEmpty() {
		Vehicle vehicle = Vehicle.register(VIN, "아이오닉 5");

		assertThat(vehicle.getVehicleId()).isNull();
		assertThat(vehicle.getPlateNumber()).isNull();
		assertThat(vehicle.getVin()).isEqualTo(VIN);
		assertThat(vehicle.getModel()).isEqualTo("아이오닉 5");
	}

	@Test
	@DisplayName("Vehicle.linkTo를 불러도 vehicleId · vin · model · plateNumber · unlinkedAt은 그대로다")
	void linkToKeepsOtherFields() {
		Vehicle vehicle = Vehicle.restore(77L, null, VIN, "아이오닉 5", "12가 3456", null);

		vehicle.linkTo(12L);

		assertThat(vehicle.getFamilyId()).isEqualTo(12L);
		assertThat(vehicle.getVehicleId()).isEqualTo(77L);
		assertThat(vehicle.getVin()).isEqualTo(VIN);
		assertThat(vehicle.getModel()).isEqualTo("아이오닉 5");
		assertThat(vehicle.getPlateNumber()).isEqualTo("12가 3456");
		assertThat(vehicle.getUnlinkedAt()).isNull();
	}
}
