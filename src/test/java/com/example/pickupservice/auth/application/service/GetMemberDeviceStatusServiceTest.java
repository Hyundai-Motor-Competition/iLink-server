package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMemberDeviceStatusServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-07T03:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-11-01T00:00:00Z");
	private static final long CONNECTED_ID = 31L;
	private static final long PENDING_ID = 32L;
	private static final long EXPIRED_ID = 33L;
	private static final long NONE_ID = 34L;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private LoadInvitePort loadInvitePort;

	@Mock
	private Clock clock;

	@InjectMocks
	private GetMemberDeviceStatusService getMemberDeviceStatusService;

	@BeforeEach
	void setUpClock() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("getDeviceStatuses는 멤버마다 기기와 최근 초대로 CONNECTED · INVITE_PENDING · INVITE_EXPIRED · NONE을 판정해 돌려준다")
	void getDeviceStatusesJudgesEachMemberFromDevicesAndLatestInvite() {
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, CONNECTED_ID)).thenReturn(List.of(
				Device.restore(901L, OwnerType.USER, CONNECTED_ID, Platform.ANDROID, "fcm", EARLIER, "e".repeat(64),
						null, NOW.plus(Duration.ofDays(14)))));
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, PENDING_ID)).thenReturn(List.of());
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, EXPIRED_ID)).thenReturn(List.of());
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, NONE_ID)).thenReturn(List.of());
		lenient().when(loadInvitePort.loadLatestInvite(CONNECTED_ID)).thenReturn(Optional.empty());
		when(loadInvitePort.loadLatestInvite(PENDING_ID)).thenReturn(Optional.of(
				MemberInvite.restore(1001L, PENDING_ID, "pending-token", NOW.plus(Duration.ofHours(1)), null)));
		when(loadInvitePort.loadLatestInvite(EXPIRED_ID)).thenReturn(Optional.of(
				MemberInvite.restore(1002L, EXPIRED_ID, "expired-token", NOW.minus(Duration.ofMinutes(1)), null)));
		when(loadInvitePort.loadLatestInvite(NONE_ID)).thenReturn(Optional.empty());

		Map<Long, MemberDeviceStatus> statuses = getMemberDeviceStatusService.getDeviceStatuses(
				List.of(CONNECTED_ID, PENDING_ID, EXPIRED_ID, NONE_ID));

		assertThat(statuses).containsExactlyInAnyOrderEntriesOf(Map.of(
				CONNECTED_ID, MemberDeviceStatus.CONNECTED,
				PENDING_ID, MemberDeviceStatus.INVITE_PENDING,
				EXPIRED_ID, MemberDeviceStatus.INVITE_EXPIRED,
				NONE_ID, MemberDeviceStatus.NONE));
	}

	@Test
	@DisplayName("getDeviceStatuses는 userIds가 비면 빈 Map을 돌려준다")
	void getDeviceStatusesReturnsEmptyMapForEmptyUserIds() {
		Map<Long, MemberDeviceStatus> statuses = getMemberDeviceStatusService.getDeviceStatuses(List.of());

		assertThat(statuses).isEmpty();
	}

}
