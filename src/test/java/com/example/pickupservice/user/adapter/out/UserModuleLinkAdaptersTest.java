package com.example.pickupservice.user.adapter.out;

import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.RevokeMemberDevicesPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
class UserModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String OUT_ADAPTER_PACKAGE = "com.example.pickupservice.user.adapter.out.";
	private static final AtomicLong UNUSED_USER_ID =
			new AtomicLong(8_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L) * 1_000L);

	private final ApplicationContext applicationContext;
	private final SaveDevicePort saveDevicePort;
	private final LoadDevicePort loadDevicePort;
	private final SaveInvitePort saveInvitePort;

	@MockitoBean
	private ActivePickupQueryUseCase activePickupQueryUseCase;

	@Test
	@DisplayName("세 포트는 각각 빈이 정확히 1개이고 user.adapter.out 아래 클래스가 구현한다")
	void eachPortHasExactlyOneBeanInUserOutAdapterPackage() {
		for (Class<?> port : List.of(LoadMemberDeviceStatusPort.class, RevokeMemberDevicesPort.class, CheckActivePickupPort.class)) {
			Object bean = onlyBeanOf(port);
			assertThat(AopUtils.getTargetClass(bean).getName())
					.as("%s 구현 클래스 위치", port.getSimpleName())
					.startsWith(OUT_ADAPTER_PACKAGE);
		}
	}

	@Test
	@DisplayName("LoadMemberDeviceStatusPort는 auth 판정 결과를 돌려준다: 살아 있는 USER 기기가 있으면 CONNECTED, 사용 전 · 만료 전 초대만 있으면 INVITE_PENDING, 기기도 초대도 없으면 NONE")
	void deviceStatusPortReturnsAuthJudgement() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);
		long connectedUserId = UNUSED_USER_ID.incrementAndGet();
		long invitedUserId = UNUSED_USER_ID.incrementAndGet();
		long bareUserId = UNUSED_USER_ID.incrementAndGet();
		Instant now = Instant.now();
		saveDevicePort.saveDevice(liveUserDevice(connectedUserId, now));
		saveInvitePort.saveInvite(MemberInvite.issue(invitedUserId, uniqueInviteToken(), now));

		Map<Long, DeviceStatus> statuses = deviceStatusPort.loadDeviceStatuses(
				List.of(connectedUserId, invitedUserId, bareUserId));

		assertThat(statuses).containsExactlyInAnyOrderEntriesOf(Map.of(
				connectedUserId, DeviceStatus.CONNECTED,
				invitedUserId, DeviceStatus.INVITE_PENDING,
				bareUserId, DeviceStatus.NONE));
	}

	@Test
	@DisplayName("LoadMemberDeviceStatusPort에 빈 목록을 주면 빈 Map을 돌려준다")
	void deviceStatusPortReturnsEmptyMapForEmptyList() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);

		assertThat(deviceStatusPort.loadDeviceStatuses(List.of())).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("RevokeMemberDevicesPort는 auth를 거쳐 그 멤버의 살아 있는 USER 기기를 모두 폐기하고 다른 멤버의 기기는 그대로 둔다")
	void revokePortRevokesLiveDevicesOfThatMemberThroughAuth() {
		RevokeMemberDevicesPort revokePort = onlyBeanOf(RevokeMemberDevicesPort.class);
		long targetUserId = UNUSED_USER_ID.incrementAndGet();
		long otherUserId = UNUSED_USER_ID.incrementAndGet();
		Instant now = Instant.now();
		saveDevicePort.saveDevice(liveUserDevice(targetUserId, now));
		saveDevicePort.saveDevice(liveUserDevice(targetUserId, now));
		saveDevicePort.saveDevice(liveUserDevice(otherUserId, now));

		revokePort.revokeDevicesOf(targetUserId);

		assertThat(loadDevicePort.loadLiveDevices(OwnerType.USER, targetUserId)).isEmpty();
		assertThat(loadDevicePort.loadLiveDevices(OwnerType.USER, otherUserId)).hasSize(1);
	}

	@Test
	@DisplayName("CheckActivePickupPort는 isInActivePickup · hasActivePickup을 pickup ActivePickupQueryUseCase 결과 그대로 돌려준다")
	void activePickupPortDelegatesToPickupUseCase() {
		CheckActivePickupPort activePickupPort = onlyBeanOf(CheckActivePickupPort.class);
		given(activePickupQueryUseCase.isInActivePickup(101L)).willReturn(true);
		given(activePickupQueryUseCase.isInActivePickup(102L)).willReturn(false);
		given(activePickupQueryUseCase.hasActivePickup(77L)).willReturn(true);
		given(activePickupQueryUseCase.hasActivePickup(78L)).willReturn(false);

		assertThat(activePickupPort.isInActivePickup(101L)).isTrue();
		assertThat(activePickupPort.isInActivePickup(102L)).isFalse();
		assertThat(activePickupPort.hasActivePickup(77L)).isTrue();
		assertThat(activePickupPort.hasActivePickup(78L)).isFalse();
		verify(activePickupQueryUseCase).isInActivePickup(101L);
		verify(activePickupQueryUseCase).hasActivePickup(77L);
	}

	private static Device liveUserDevice(long userId, Instant at) {
		return Device.register(OwnerType.USER, userId, Platform.ANDROID, "push-" + UUID.randomUUID(), at,
				randomSha256Hex());
	}

	private static String uniqueInviteToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private static String randomSha256Hex() {
		byte[] bytes = new byte[32];
		ThreadLocalRandom.current().nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private <T> T onlyBeanOf(Class<T> type) {
		Map<String, T> beans = applicationContext.getBeansOfType(type);
		assertThat(beans).as("%s 빈 개수", type.getSimpleName()).hasSize(1);
		return beans.values().iterator().next();
	}
}
