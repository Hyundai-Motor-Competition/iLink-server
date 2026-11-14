package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
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
	private static final long OTHER_USER_ID = 32L;
	private static final long INVITE_ID = 801L;
	private static final long OTHER_INVITE_ID = 802L;
	private static final Instant INVITE_EXPIRES_AT = NOW.plus(Duration.ofHours(20));

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Mock
	private LoadInvitePort loadInvitePort;

	@Mock
	private SaveInvitePort saveInvitePort;

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

	@Test
	@DisplayName("revokeDevicesOf는 그 멤버의 아직 쓰지 않은 초대도 폐기해 그 토큰으로 확인 · 수락할 수 없게 한다")
	void revokeDevicesOfRevokesUnusedInviteOfMember() {
		Device android = liveUserDevice(701L, Platform.ANDROID, "fcm-android");
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, USER_ID)).thenReturn(List.of(android));
		lenient().when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
		MemberInvite unused = MemberInvite.restore(INVITE_ID, USER_ID, "inv-unused", INVITE_EXPIRES_AT, null);
		lenient().when(loadInvitePort.loadLatestInvite(USER_ID)).thenReturn(Optional.of(unused));
		lenient().when(saveInvitePort.saveInvite(any(MemberInvite.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		revokeMemberDevicesService.revokeDevicesOf(USER_ID);

		assertThat(android.getRevokedAt()).isEqualTo(NOW);
		assertThat(unused.isExpired(NOW)).as("쓰지 않은 초대 폐기").isTrue();
		assertThat(unused.getUsedAt()).isNull();
		ArgumentCaptor<MemberInvite> saved = ArgumentCaptor.forClass(MemberInvite.class);
		verify(saveInvitePort, atLeastOnce()).saveInvite(saved.capture());
		assertThat(saved.getAllValues())
				.filteredOn(invite -> invite.getInviteId() != null && invite.getInviteId() == INVITE_ID)
				.as("폐기한 초대 저장")
				.isNotEmpty()
				.allSatisfy(invite -> {
					assertThat(invite.getUserId()).isEqualTo(USER_ID);
					assertThat(invite.isExpired(NOW)).isTrue();
					assertThat(invite.getUsedAt()).isNull();
				});
	}

	@Test
	@DisplayName("revokeDevicesOf는 살아 있는 기기가 없어도 그 멤버의 쓰지 않은 초대를 폐기한다")
	void revokeDevicesOfRevokesUnusedInviteEvenWithoutLiveDevice() {
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, USER_ID)).thenReturn(List.of());
		MemberInvite unused = MemberInvite.restore(INVITE_ID, USER_ID, "inv-unused", INVITE_EXPIRES_AT, null);
		lenient().when(loadInvitePort.loadLatestInvite(USER_ID)).thenReturn(Optional.of(unused));
		lenient().when(saveInvitePort.saveInvite(any(MemberInvite.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		revokeMemberDevicesService.revokeDevicesOf(USER_ID);

		assertThat(unused.isExpired(NOW)).as("쓰지 않은 초대 폐기").isTrue();
		verify(saveInvitePort, atLeastOnce()).saveInvite(unused);
		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("revokeDevicesOf는 이미 쓴 초대와 다른 멤버의 초대는 그대로 둔다")
	void revokeDevicesOfLeavesUsedInviteAndOtherMembersInviteUntouched() {
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, USER_ID)).thenReturn(List.of());
		Instant usedAt = NOW.minus(Duration.ofHours(1));
		MemberInvite used = MemberInvite.restore(INVITE_ID, USER_ID, "inv-used", INVITE_EXPIRES_AT, usedAt);
		MemberInvite otherMembers = MemberInvite.restore(OTHER_INVITE_ID, OTHER_USER_ID, "inv-other",
				INVITE_EXPIRES_AT, null);
		lenient().when(loadInvitePort.loadLatestInvite(USER_ID)).thenReturn(Optional.of(used));
		lenient().when(loadInvitePort.loadLatestInvite(OTHER_USER_ID)).thenReturn(Optional.of(otherMembers));
		lenient().when(loadInvitePort.loadInviteByToken("inv-used")).thenReturn(Optional.of(used));
		lenient().when(loadInvitePort.loadInviteByToken("inv-other")).thenReturn(Optional.of(otherMembers));
		lenient().when(saveInvitePort.saveInvite(any(MemberInvite.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		revokeMemberDevicesService.revokeDevicesOf(USER_ID);

		assertThat(used.getExpiresAt()).as("쓴 초대 만료 시각 그대로").isEqualTo(INVITE_EXPIRES_AT);
		assertThat(used.getUsedAt()).isEqualTo(usedAt);
		assertThat(otherMembers.getExpiresAt()).as("다른 멤버 초대 만료 시각 그대로").isEqualTo(INVITE_EXPIRES_AT);
		assertThat(otherMembers.getUsedAt()).isNull();
		verify(saveInvitePort, never()).saveInvite(used);
		verify(saveInvitePort, never()).saveInvite(otherMembers);
		verify(loadInvitePort, atLeastOnce()).loadLatestInvite(USER_ID);
	}

	private static Device liveUserDevice(long deviceId, Platform platform, String pushToken) {
		return Device.restore(deviceId, OwnerType.USER, USER_ID, platform, pushToken, EARLIER, "c".repeat(64), null,
				NOW.plus(Duration.ofDays(14)));
	}

}
