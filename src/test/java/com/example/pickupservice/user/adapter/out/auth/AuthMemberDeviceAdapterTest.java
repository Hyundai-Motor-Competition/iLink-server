package com.example.pickupservice.user.adapter.out.auth;

import com.example.pickupservice.auth.application.port.in.GetMemberDeviceStatusUseCase;
import com.example.pickupservice.auth.application.port.in.RevokeMemberDevicesUseCase;
import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.user.domain.DeviceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthMemberDeviceAdapterTest {

	@Mock
	private GetMemberDeviceStatusUseCase getMemberDeviceStatusUseCase;

	@Mock
	private RevokeMemberDevicesUseCase revokeMemberDevicesUseCase;

	@InjectMocks
	private AuthMemberDeviceAdapter adapter;

	@Test
	@DisplayName("user의 AuthMemberDeviceAdapter는 auth GetMemberDeviceStatusUseCase 결과를 user DeviceStatus로 바꿔 돌려준다")
	void convertsAuthMemberDeviceStatusToUserDeviceStatus() {
		Map<Long, MemberDeviceStatus> authStatuses = new LinkedHashMap<>();
		authStatuses.put(11L, MemberDeviceStatus.CONNECTED);
		authStatuses.put(12L, MemberDeviceStatus.INVITE_PENDING);
		authStatuses.put(13L, MemberDeviceStatus.INVITE_EXPIRED);
		authStatuses.put(14L, MemberDeviceStatus.NONE);
		when(getMemberDeviceStatusUseCase.getDeviceStatuses(anyList())).thenReturn(authStatuses);

		Map<Long, DeviceStatus> statuses = adapter.loadDeviceStatuses(List.of(11L, 12L, 13L, 14L));

		assertThat(statuses).containsExactlyInAnyOrderEntriesOf(Map.of(
				11L, DeviceStatus.CONNECTED,
				12L, DeviceStatus.INVITE_PENDING,
				13L, DeviceStatus.INVITE_EXPIRED,
				14L, DeviceStatus.NONE));
	}

	@Test
	@DisplayName("loadDeviceStatuses는 요청한 userId 목록을 그대로 auth GetMemberDeviceStatusUseCase에 넘긴다")
	void passesRequestedUserIdsToAuthUseCase() {
		List<Long> userIds = List.of(31L, 21L, 41L);
		when(getMemberDeviceStatusUseCase.getDeviceStatuses(userIds)).thenReturn(Map.of(
				31L, MemberDeviceStatus.CONNECTED,
				21L, MemberDeviceStatus.NONE,
				41L, MemberDeviceStatus.INVITE_PENDING));

		adapter.loadDeviceStatuses(userIds);

		verify(getMemberDeviceStatusUseCase).getDeviceStatuses(userIds);
	}

	@Test
	@DisplayName("user의 RevokeMemberDevicesPort 구현은 auth RevokeMemberDevicesUseCase를 부른다")
	void revokeDevicesOfDelegatesToAuthUseCase() {
		adapter.revokeDevicesOf(42L);

		verify(revokeMemberDevicesUseCase).revokeDevicesOf(42L);
	}
}
