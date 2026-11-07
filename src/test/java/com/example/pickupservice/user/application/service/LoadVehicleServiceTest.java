package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.VehicleInfo;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoadVehicleServiceTest {

	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final String VIN = "KMHK381GFNU000001";

	@Mock
	private LoadVehiclePort loadVehiclePort;

	@InjectMocks
	private LoadVehicleService loadVehicleService;

	@Test
	@DisplayName("user의 LoadVehicleUseCase는 findVehicle · findLinkedVehicleByVin으로 VehicleInfo를 돌려준다")
	void findVehicleReturnsVehicleInfoFromLoadedVehicle() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(
				Vehicle.restore(VEHICLE_ID, FAMILY_ID, VIN, "아이오닉 5", "12가 3456", null)));

		Optional<VehicleInfo> result = loadVehicleService.findVehicle(VEHICLE_ID);

		assertThat(result).contains(
				new VehicleInfo(VEHICLE_ID, FAMILY_ID, VIN, "아이오닉 5", "12가 3456", true));
	}

	@Test
	@DisplayName("findVehicle은 연결이 해제된 차량이면 linked false인 VehicleInfo를 돌려준다")
	void findVehicleReturnsLinkedFalseForUnlinkedVehicle() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.of(
				Vehicle.restore(VEHICLE_ID, FAMILY_ID, VIN, "아이오닉 5", null,
						Instant.parse("2026-11-01T00:00:00Z"))));

		Optional<VehicleInfo> result = loadVehicleService.findVehicle(VEHICLE_ID);

		assertThat(result).contains(new VehicleInfo(VEHICLE_ID, FAMILY_ID, VIN, "아이오닉 5", null, false));
	}

	@Test
	@DisplayName("findVehicle은 차량이 없으면 빈 Optional을 돌려준다")
	void findVehicleReturnsEmptyWhenVehicleMissing() {
		given(loadVehiclePort.loadVehicle(VEHICLE_ID)).willReturn(Optional.empty());

		assertThat(loadVehicleService.findVehicle(VEHICLE_ID)).isEmpty();
	}

	@Test
	@DisplayName("findLinkedVehicleByVin은 그 VIN으로 연결 중인 차량을 VehicleInfo로 돌려준다")
	void findLinkedVehicleByVinReturnsVehicleInfo() {
		given(loadVehiclePort.loadLinkedVehicleByVin(VIN)).willReturn(Optional.of(
				Vehicle.restore(VEHICLE_ID, null, VIN, "아이오닉 5", null, null)));

		Optional<VehicleInfo> result = loadVehicleService.findLinkedVehicleByVin(VIN);

		assertThat(result).contains(new VehicleInfo(VEHICLE_ID, null, VIN, "아이오닉 5", null, true));
	}

	@Test
	@DisplayName("findLinkedVehicleByVin은 그 VIN의 차량이 없으면 빈 Optional을 돌려준다")
	void findLinkedVehicleByVinReturnsEmptyWhenMissing() {
		given(loadVehiclePort.loadLinkedVehicleByVin(VIN)).willReturn(Optional.empty());

		assertThat(loadVehicleService.findLinkedVehicleByVin(VIN)).isEmpty();
	}
}
