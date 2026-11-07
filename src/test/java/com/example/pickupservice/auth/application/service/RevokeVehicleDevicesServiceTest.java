package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.adapter.out.token.JwtTokenIssuer;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevokeVehicleDevicesServiceTest {

	private static final String SECRET = Base64.getEncoder()
			.encodeToString("pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8));
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final Instant EARLIER = NOW.minus(Duration.ofDays(3));
	private static final long VEHICLE_ID = 77L;
	private static final long OTHER_VEHICLE_ID = 88L;
	private static final long USER_ID = 31L;
	private static final long FAMILY_ID = 12L;

	private final InMemoryDevices devices = new InMemoryDevices();
	private final JwtTokenIssuer tokenIssuer = new JwtTokenIssuer(SECRET, Duration.ofMinutes(30),
			Clock.fixed(NOW, ZoneOffset.UTC));
	private final LoadVehicleUseCase loadVehicleUseCase = mock(LoadVehicleUseCase.class);

	private RevokeVehicleDevicesService revokeVehicleDevicesService;
	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		revokeVehicleDevicesService = new RevokeVehicleDevicesService(devices, devices,
				Clock.fixed(NOW, ZoneOffset.UTC));
		refreshTokenService = new RefreshTokenService(devices, devices, tokenIssuer, mock(LoadMemberUseCase.class),
				loadVehicleUseCase);
	}

	@Test
	@DisplayName("차량 기기 폐기는 그 차량의 살아 있는 VEHICLE 기기 revokedAt을 모두 채우고 다른 차량 · USER 기기는 그대로 둔다")
	void revokesAllLiveDevicesOfVehicleOnly() {
		Device first = devices.add(vehicleDevice(601L, VEHICLE_ID, "rt_vehicle-601"));
		Device second = devices.add(vehicleDevice(602L, VEHICLE_ID, "rt_vehicle-602"));
		Device otherVehicle = devices.add(vehicleDevice(603L, OTHER_VEHICLE_ID, "rt_vehicle-603"));
		Device sameIdUser = devices.add(Device.restore(701L, OwnerType.USER, VEHICLE_ID, Platform.ANDROID, "fcm-701",
				EARLIER, tokenIssuer.hashRefreshToken("rt_user-701"), null));

		revokeVehicleDevicesService.revokeDevicesOfVehicle(VEHICLE_ID);

		assertThat(devices.find(first.getDeviceId()).getRevokedAt()).isEqualTo(NOW);
		assertThat(devices.find(second.getDeviceId()).getRevokedAt()).isEqualTo(NOW);
		assertThat(devices.find(otherVehicle.getDeviceId()).getRevokedAt()).isNull();
		assertThat(devices.find(sameIdUser.getDeviceId()).getRevokedAt()).isNull();
		assertThat(devices.loadLiveDevices(OwnerType.VEHICLE, VEHICLE_ID)).isEmpty();
	}

	@Test
	@DisplayName("연결이 해제돼 기기가 폐기된 차량은 이전 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다")
	void refreshOfRevokedVehicleDeviceIsRejected() {
		devices.add(vehicleDevice(601L, VEHICLE_ID, "rt_vehicle-601"));
		when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(
				new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHKM81AFPU123456", "아이오닉 5", "12가3456", false)));

		revokeVehicleDevicesService.revokeDevicesOfVehicle(VEHICLE_ID);

		assertThatThrownBy(() -> refreshTokenService.refresh("rt_vehicle-601"))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.code()).isEqualTo("REFRESH_TOKEN_INVALID");
					assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
				});
	}

	@Test
	@DisplayName("차량 기기 폐기는 그 차량에 살아 있는 기기가 없으면 아무것도 바꾸지 않는다")
	void doesNothingWhenVehicleHasNoLiveDevice() {
		Device revoked = devices.add(Device.restore(601L, OwnerType.VEHICLE, VEHICLE_ID, Platform.VEHICLE, null, null,
				tokenIssuer.hashRefreshToken("rt_vehicle-601"), EARLIER));

		revokeVehicleDevicesService.revokeDevicesOfVehicle(VEHICLE_ID);

		assertThat(devices.find(revoked.getDeviceId()).getRevokedAt()).isEqualTo(EARLIER);
		assertThat(devices.saveCount).isZero();
	}

	private Device vehicleDevice(long deviceId, long vehicleId, String refreshToken) {
		return Device.restore(deviceId, OwnerType.VEHICLE, vehicleId, Platform.VEHICLE, null, null,
				tokenIssuer.hashRefreshToken(refreshToken), null);
	}

	private static final class InMemoryDevices implements LoadDevicePort, SaveDevicePort {

		private final List<Device> stored = new ArrayList<>();
		private int saveCount;

		Device add(Device device) {
			stored.add(device);
			return device;
		}

		Device find(long deviceId) {
			return loadDevice(deviceId).orElseThrow();
		}

		@Override
		public Optional<Device> loadDevice(long deviceId) {
			return stored.stream().filter(device -> device.getDeviceId() == deviceId).findFirst();
		}

		@Override
		public List<Device> loadLiveDevices(OwnerType type, long ownerId) {
			return stored.stream()
					.filter(device -> device.getOwnerType() == type && device.getOwnerId() == ownerId)
					.filter(Device::isLive)
					.toList();
		}

		@Override
		public Optional<Device> loadByRefreshTokenHash(String hash) {
			return stored.stream().filter(device -> hash.equals(device.getRefreshTokenHash())).findFirst();
		}

		@Override
		public Device saveDevice(Device device) {
			saveCount++;
			stored.removeIf(existing -> existing.getDeviceId().equals(device.getDeviceId()));
			stored.add(device);
			return device;
		}
	}
}
