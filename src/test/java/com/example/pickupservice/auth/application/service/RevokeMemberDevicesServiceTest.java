package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeMemberDevicesServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T03:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-11-01T00:00:00Z");
	private static final long USER_ID = 31L;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Mock
	private Clock clock;

	@InjectMocks
	private RevokeMemberDevicesService revokeMemberDevicesService;

	@BeforeEach
	void setUpClock() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("revokeDevicesOf는 그 멤버의 살아 있는 기기를 모두 폐기한다")
	void revokeDevicesOfRevokesAllLiveDevicesOfMember() {
		Device android = liveUserDevice(701L, Platform.ANDROID, "fcm-android");
		Device ios = liveUserDevice(702L, Platform.IOS, "fcm-ios");
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, USER_ID)).thenReturn(List.of(android, ios));
		when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		revokeMemberDevicesService.revokeDevicesOf(USER_ID);

		ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort, times(2)).saveDevice(saved.capture());
		assertThat(saved.getAllValues())
				.extracting(Device::getDeviceId)
				.containsExactlyInAnyOrder(701L, 702L);
		assertThat(saved.getAllValues())
				.allSatisfy(device -> {
					assertThat(device.getRevokedAt()).isEqualTo(NOW);
					assertThat(device.isLive()).isFalse();
					assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
					assertThat(device.getOwnerId()).isEqualTo(USER_ID);
				});
	}

	@Test
	@DisplayName("revokeDevicesOf는 그 멤버에게 살아 있는 기기가 없으면 아무것도 저장하지 않는다")
	void revokeDevicesOfSavesNothingWhenMemberHasNoLiveDevice() {
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, USER_ID)).thenReturn(List.of());

		revokeMemberDevicesService.revokeDevicesOf(USER_ID);

		verify(loadDevicePort).loadLiveDevices(OwnerType.USER, USER_ID);
		verify(saveDevicePort, never()).saveDevice(any());
	}

	private static Device liveUserDevice(long deviceId, Platform platform, String pushToken) {
		return Device.restore(deviceId, OwnerType.USER, USER_ID, platform, pushToken, EARLIER, "c".repeat(64), null,
				NOW.plus(Duration.ofDays(14)));
	}

}
