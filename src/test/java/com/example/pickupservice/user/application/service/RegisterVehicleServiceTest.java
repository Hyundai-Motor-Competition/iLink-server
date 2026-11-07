package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.out.SaveVehiclePort;
import com.example.pickupservice.user.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterVehicleServiceTest {

	private static final long VEHICLE_ID = 77L;
	private static final String VIN = "KMHKM81AFPU123456";
	private static final String MODEL = "아이오닉 5";

	@Mock
	private SaveVehiclePort saveVehiclePort;

	private RegisterVehicleService registerVehicleService;

	@BeforeEach
	void setUp() {
		registerVehicleService = new RegisterVehicleService(saveVehiclePort);
	}

	@Test
	@DisplayName("registerVehicle은 familyId가 빈 Vehicle 행을 만들고 vehicleId를 돌려준다")
	void registersUnownedVehicleAndReturnsVehicleId() {
		given(saveVehiclePort.saveVehicle(any())).willAnswer(invocation -> {
			Vehicle vehicle = invocation.getArgument(0);
			return Vehicle.restore(VEHICLE_ID, vehicle.getFamilyId(), vehicle.getVin(), vehicle.getModel(),
					vehicle.getPlateNumber(), vehicle.getUnlinkedAt());
		});

		long vehicleId = registerVehicleService.registerVehicle(VIN, MODEL);

		assertThat(vehicleId).isEqualTo(VEHICLE_ID);
		ArgumentCaptor<Vehicle> saved = ArgumentCaptor.forClass(Vehicle.class);
		verify(saveVehiclePort, times(1)).saveVehicle(saved.capture());
		Vehicle vehicle = saved.getValue();
		assertThat(vehicle.getVehicleId()).isNull();
		assertThat(vehicle.getFamilyId()).isNull();
		assertThat(vehicle.getUnlinkedAt()).isNull();
		assertThat(vehicle.getVin()).isEqualTo(VIN);
		assertThat(vehicle.getModel()).isEqualTo(MODEL);
	}
}
