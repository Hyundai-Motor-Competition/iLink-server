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
import java.util.Optional;

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

	@Test
	@DisplayName("findPushTargetByDeviceId는 폐기된 USER 기기도 pushToken이 있으면 PushTarget으로 돌려준다")
	void findPushTargetByDeviceIdReturnsRevokedUserDeviceWithPushToken() {
		when(loadDevicePort.loadDevice(804L)).thenReturn(Optional.of(
				userDevice(804L, CHILD_ID, Platform.IOS, "fcm-revoked", EARLIER)));

		Optional<PushTarget> target = loadPushTargetService.findPushTargetByDeviceId(804L);

		assertThat(target).contains(new PushTarget(CHILD_ID, 804L, Platform.IOS, "fcm-revoked"));
	}

	@Test
	@DisplayName("findPushTargetByDeviceId는 폐기되지 않은 USER 기기도 PushTarget으로 돌려준다")
	void findPushTargetByDeviceIdReturnsLiveUserDevice() {
		when(loadDevicePort.loadDevice(801L)).thenReturn(Optional.of(
				userDevice(801L, CHILD_ID, Platform.ANDROID, "fcm-child", null)));

		assertThat(loadPushTargetService.findPushTargetByDeviceId(801L))
				.contains(new PushTarget(CHILD_ID, 801L, Platform.ANDROID, "fcm-child"));
	}

	@Test
	@DisplayName("findPushTargetByDeviceId는 pushToken이 null이거나 빈 기기면 빈 값을 돌려준다")
	void findPushTargetByDeviceIdReturnsEmptyWhenPushTokenMissing() {
		when(loadDevicePort.loadDevice(802L)).thenReturn(Optional.of(
				userDevice(802L, CHILD_ID, Platform.ANDROID, null, null)));
		when(loadDevicePort.loadDevice(803L)).thenReturn(Optional.of(
				userDevice(803L, CHILD_ID, Platform.IOS, "", null)));

		assertThat(loadPushTargetService.findPushTargetByDeviceId(802L)).isNotNull().isEmpty();
		assertThat(loadPushTargetService.findPushTargetByDeviceId(803L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("findPushTargetByDeviceId는 VEHICLE 기기면 pushToken이 있어도 빈 값을 돌려준다")
	void findPushTargetByDeviceIdReturnsEmptyForVehicleDevice() {
		when(loadDevicePort.loadDevice(901L)).thenReturn(Optional.of(
				Device.restore(901L, OwnerType.VEHICLE, 55L, Platform.ANDROID, "fcm-vehicle", EARLIER, "d".repeat(64),
						null)));

		assertThat(loadPushTargetService.findPushTargetByDeviceId(901L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("findPushTargetByDeviceId는 없는 기기 id면 빈 값을 돌려준다")
	void findPushTargetByDeviceIdReturnsEmptyForMissingDevice() {
		when(loadDevicePort.loadDevice(999L)).thenReturn(Optional.empty());

		assertThat(loadPushTargetService.findPushTargetByDeviceId(999L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("findLivePushTargetByDeviceId는 폐기된 USER 기기면 pushToken이 있어도 빈 값을 돌려준다")
	void findLivePushTargetByDeviceIdReturnsEmptyForRevokedDevice() {
		when(loadDevicePort.loadDevice(804L)).thenReturn(Optional.of(
				userDevice(804L, CHILD_ID, Platform.IOS, "fcm-revoked", EARLIER)));

		assertThat(loadPushTargetService.findLivePushTargetByDeviceId(804L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("findLivePushTargetByDeviceId는 폐기되지 않고 pushToken이 있는 USER 기기를 PushTarget으로 돌려준다")
	void findLivePushTargetByDeviceIdReturnsLiveUserDevice() {
		when(loadDevicePort.loadDevice(801L)).thenReturn(Optional.of(
				userDevice(801L, CHILD_ID, Platform.ANDROID, "fcm-child", null)));

		assertThat(loadPushTargetService.findLivePushTargetByDeviceId(801L))
				.contains(new PushTarget(CHILD_ID, 801L, Platform.ANDROID, "fcm-child"));
	}

	@Test
	@DisplayName("findLivePushTargetByDeviceId는 pushToken이 null이거나 빈 기기면 빈 값을 돌려준다")
	void findLivePushTargetByDeviceIdReturnsEmptyWhenPushTokenMissing() {
		when(loadDevicePort.loadDevice(802L)).thenReturn(Optional.of(
				userDevice(802L, CHILD_ID, Platform.ANDROID, null, null)));
		when(loadDevicePort.loadDevice(803L)).thenReturn(Optional.of(
				userDevice(803L, CHILD_ID, Platform.IOS, "", null)));

		assertThat(loadPushTargetService.findLivePushTargetByDeviceId(802L)).isNotNull().isEmpty();
		assertThat(loadPushTargetService.findLivePushTargetByDeviceId(803L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("findLivePushTargetByDeviceId는 VEHICLE 기기면 살아 있고 pushToken이 있어도 빈 값을 돌려준다")
	void findLivePushTargetByDeviceIdReturnsEmptyForVehicleDevice() {
		when(loadDevicePort.loadDevice(901L)).thenReturn(Optional.of(
				Device.restore(901L, OwnerType.VEHICLE, 55L, Platform.ANDROID, "fcm-vehicle", EARLIER, "d".repeat(64),
						null)));

		assertThat(loadPushTargetService.findLivePushTargetByDeviceId(901L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("findLivePushTargetByDeviceId는 없는 기기 id면 빈 값을 돌려준다")
	void findLivePushTargetByDeviceIdReturnsEmptyForMissingDevice() {
		when(loadDevicePort.loadDevice(999L)).thenReturn(Optional.empty());

		assertThat(loadPushTargetService.findLivePushTargetByDeviceId(999L)).isNotNull().isEmpty();
	}

	private static Device userDevice(long deviceId, long userId, Platform platform, String pushToken,
			Instant revokedAt) {
		Instant pushUpdatedAt = pushToken == null ? null : EARLIER;
		return Device.restore(deviceId, OwnerType.USER, userId, platform, pushToken, pushUpdatedAt, "d".repeat(64),
				revokedAt);
	}

}
