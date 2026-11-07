package com.example.pickupservice.user.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.RevokeVehicleDevicesUseCase;
import com.example.pickupservice.user.application.port.out.RevokeVehicleDevicesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthVehicleDeviceAdapterTest {

	@Mock
	private RevokeVehicleDevicesUseCase revokeVehicleDevicesUseCase;

	@InjectMocks
	private AuthVehicleDeviceAdapter adapter;

	@Test
	@DisplayName("user의 RevokeVehicleDevicesPort 구현은 auth RevokeVehicleDevicesUseCase를 그 vehicleId로 부른다")
	void revokeDevicesOfVehicleDelegatesToAuthUseCase() {
		assertThat(adapter).isInstanceOf(RevokeVehicleDevicesPort.class);

		adapter.revokeDevicesOfVehicle(77L);

		verify(revokeVehicleDevicesUseCase).revokeDevicesOfVehicle(77L);
	}
}
