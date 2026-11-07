package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutVehicleServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T03:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-11-01T00:00:00Z");
	private static final long DEVICE_ID = 601L;
	private static final long VEHICLE_ID = 77L;
	private static final String REFRESH_HASH = "b".repeat(64);

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Mock
	private CheckActivePickupPort checkActivePickupPort;

	@Mock
	private Clock clock;

	@InjectMocks
	private LogoutVehicleService logoutVehicleService;

	@BeforeEach
	void setUpClock() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("차량 로그아웃은 진행 중 픽업이 있으면 ACTIVE_PICKUP_EXISTS로 거절한다")
	void logoutVehicleRejectsWhenVehicleHasActivePickup() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.of(liveVehicleDevice()));
		when(checkActivePickupPort.hasActivePickup(VEHICLE_ID)).thenReturn(true);

		assertThatThrownBy(() -> logoutVehicleService.logoutVehicle(DEVICE_ID))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("ACTIVE_PICKUP_EXISTS");
				});

		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("차량 로그아웃이 성공하면 그 기기의 revokedAt이 채워진다")
	void logoutVehicleFillsRevokedAtOfDevice() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.of(liveVehicleDevice()));
		when(checkActivePickupPort.hasActivePickup(VEHICLE_ID)).thenReturn(false);
		when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		logoutVehicleService.logoutVehicle(DEVICE_ID);

		ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort).saveDevice(saved.capture());
		Device device = saved.getValue();
		assertThat(device.getDeviceId()).isEqualTo(DEVICE_ID);
		assertThat(device.getRevokedAt()).isEqualTo(NOW);
		assertThat(device.isLive()).isFalse();
		verify(checkActivePickupPort).hasActivePickup(VEHICLE_ID);
	}

	@Test
	@DisplayName("차량 로그아웃은 기기가 없으면 NOT_FOUND로 거절한다")
	void logoutVehicleRejectsUnknownDeviceAsNotFound() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> logoutVehicleService.logoutVehicle(DEVICE_ID))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(e.code()).isEqualTo("NOT_FOUND");
				});

		verify(checkActivePickupPort, never()).hasActivePickup(anyLong());
		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("차량 로그아웃은 VEHICLE 기기가 아니면 NOT_FOUND로 거절한다")
	void logoutVehicleRejectsUserDeviceAsNotFound() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.of(Device.restore(DEVICE_ID, OwnerType.USER,
				31L, Platform.ANDROID, "fcm-token", EARLIER, REFRESH_HASH, null)));

		assertThatThrownBy(() -> logoutVehicleService.logoutVehicle(DEVICE_ID))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(e.code()).isEqualTo("NOT_FOUND");
				});

		verify(checkActivePickupPort, never()).hasActivePickup(anyLong());
		verify(saveDevicePort, never()).saveDevice(any());
	}

	private static Device liveVehicleDevice() {
		return Device.restore(DEVICE_ID, OwnerType.VEHICLE, VEHICLE_ID, Platform.VEHICLE, null, null, REFRESH_HASH,
				null);
	}

}
