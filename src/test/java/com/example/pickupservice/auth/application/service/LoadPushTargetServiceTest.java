package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadPushTargetServiceTest {

	private static final Instant EARLIER = Instant.parse("2026-11-01T00:00:00Z");
	private static final long CHILD_ID = 31L;
	private static final long TEACHER_ID = 32L;

	@Mock
	private LoadDevicePort loadDevicePort;

	@InjectMocks
	private LoadPushTargetService loadPushTargetService;

	@Test
	@DisplayName("findPushTargets는 폐기됐거나 pushToken이 빈 기기를 빼고 돌려준다")
	void findPushTargetsExcludesRevokedDevicesAndDevicesWithoutPushToken() {
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of(
				userDevice(801L, CHILD_ID, Platform.ANDROID, "fcm-child", null),
				userDevice(802L, CHILD_ID, Platform.ANDROID, null, null),
				userDevice(803L, CHILD_ID, Platform.IOS, "", null),
				userDevice(804L, CHILD_ID, Platform.IOS, "fcm-revoked", EARLIER)));
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, TEACHER_ID)).thenReturn(List.of(
				userDevice(805L, TEACHER_ID, Platform.IOS, "fcm-teacher", null)));

		List<PushTarget> targets = loadPushTargetService.findPushTargets(List.of(CHILD_ID, TEACHER_ID));

		assertThat(targets).containsExactlyInAnyOrder(
				new PushTarget(CHILD_ID, 801L, Platform.ANDROID, "fcm-child"),
				new PushTarget(TEACHER_ID, 805L, Platform.IOS, "fcm-teacher"));
	}

	@Test
	@DisplayName("findPushTargets는 userIds가 비면 빈 목록을 돌려준다")
	void findPushTargetsReturnsEmptyListForEmptyUserIds() {
		List<PushTarget> targets = loadPushTargetService.findPushTargets(List.of());

		assertThat(targets).isEmpty();
	}

	private static Device userDevice(long deviceId, long userId, Platform platform, String pushToken,
			Instant revokedAt) {
		Instant pushUpdatedAt = pushToken == null ? null : EARLIER;
		return Device.restore(deviceId, OwnerType.USER, userId, platform, pushToken, pushUpdatedAt, "d".repeat(64),
				revokedAt);
	}

}
