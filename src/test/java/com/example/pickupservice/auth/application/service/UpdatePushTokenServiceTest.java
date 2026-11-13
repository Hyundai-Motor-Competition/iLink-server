package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.UpdatePushTokenCommand;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePushTokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T03:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-11-01T00:00:00Z");
	private static final long DEVICE_ID = 501L;
	private static final long USER_ID = 31L;
	private static final String OLD_PUSH_TOKEN = "fcm-old-token";
	private static final String NEW_PUSH_TOKEN = "fcm-new-token";

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Mock
	private Clock clock;

	@InjectMocks
	private UpdatePushTokenService updatePushTokenService;

	@BeforeEach
	void setUpClock() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("푸시 토큰 갱신은 지금 기기의 pushToken과 pushUpdatedAt을 바꾼다")
	void updatePushTokenChangesPushTokenAndPushUpdatedAtOfCurrentDevice() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.of(liveUserDevice()));
		when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		updatePushTokenService.updatePushToken(new UpdatePushTokenCommand(DEVICE_ID, Platform.ANDROID, NEW_PUSH_TOKEN));

		ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort).saveDevice(saved.capture());
		Device device = saved.getValue();
		assertThat(device.getDeviceId()).isEqualTo(DEVICE_ID);
		assertThat(device.getPushToken()).isEqualTo(NEW_PUSH_TOKEN);
		assertThat(device.getPushUpdatedAt()).isEqualTo(NOW);
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
		assertThat(device.getOwnerId()).isEqualTo(USER_ID);
		assertThat(device.isLive()).isTrue();
	}

	@Test
	@DisplayName("푸시 토큰 갱신은 기기가 없으면 UNAUTHORIZED로 거절한다")
	void updatePushTokenRejectsUnknownDeviceAsUnauthorized() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> updatePushTokenService.updatePushToken(
				new UpdatePushTokenCommand(DEVICE_ID, Platform.ANDROID, NEW_PUSH_TOKEN)))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(e.code()).isEqualTo("UNAUTHORIZED");
				});

		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("푸시 토큰 갱신은 폐기된 기기면 UNAUTHORIZED로 거절하고 저장하지 않는다")
	void updatePushTokenRejectsRevokedDeviceAsUnauthorized() {
		when(loadDevicePort.loadDevice(DEVICE_ID)).thenReturn(Optional.of(Device.restore(DEVICE_ID, OwnerType.USER,
				USER_ID, Platform.ANDROID, OLD_PUSH_TOKEN, EARLIER, "a".repeat(64), EARLIER,
				NOW.plus(Duration.ofDays(14)))));

		assertThatThrownBy(() -> updatePushTokenService.updatePushToken(
				new UpdatePushTokenCommand(DEVICE_ID, Platform.ANDROID, NEW_PUSH_TOKEN)))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(e.code()).isEqualTo("UNAUTHORIZED");
				});

		verify(saveDevicePort, never()).saveDevice(any());
	}

	private static Device liveUserDevice() {
		return Device.restore(DEVICE_ID, OwnerType.USER, USER_ID, Platform.ANDROID, OLD_PUSH_TOKEN, EARLIER,
				"a".repeat(64), null, NOW.plus(Duration.ofDays(14)));
	}

}
