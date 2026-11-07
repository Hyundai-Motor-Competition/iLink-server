package com.example.pickupservice.schedule.adapter.out;

import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class ScheduleModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String OUT_ADAPTER_PACKAGE = "com.example.pickupservice.schedule.adapter.out.";
	private static final Duration LONGER_THAN_INVITE_VALIDITY = Duration.ofHours(25);
	private static final AtomicLong UNUSED_USER_ID =
			new AtomicLong(7_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L) * 1_000L);

	private final ApplicationContext applicationContext;
	private final SaveDevicePort saveDevicePort;
	private final SaveInvitePort saveInvitePort;

	@Test
	@DisplayName("place 구현 전 임시 LoadPickupSpotPort는 빈 목록을 돌려준다")
	void temporaryPickupSpotAdapterReturnsEmptyList() {
		LoadPickupSpotPort pickupSpotPort = onlyBeanOf(LoadPickupSpotPort.class);

		assertThat(pickupSpotPort.loadSpots(1L, List.of(10L, 20L))).isNotNull().isEmpty();
		assertThat(pickupSpotPort.loadSpots(9_999_999L, List.of())).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("schedule의 LoadPickupSpotPort · LoadMemberDeviceStatusPort는 각각 빈이 정확히 1개이고 schedule.adapter.out 아래 클래스가 구현한다")
	void eachPortHasExactlyOneBeanInScheduleOutAdapterPackage() {
		for (Class<?> port : List.of(LoadPickupSpotPort.class, LoadMemberDeviceStatusPort.class)) {
			Object bean = onlyBeanOf(port);
			assertThat(AopUtils.getTargetClass(bean).getName())
					.as("%s 구현 클래스 위치", port.getSimpleName())
					.startsWith(OUT_ADAPTER_PACKAGE);
		}
	}

	@Test
	@DisplayName("LoadMemberDeviceStatusPort는 auth의 멤버 기기 상태를 schedule DeviceStatus로 바꿔 돌려준다: CONNECTED · INVITE_PENDING · INVITE_EXPIRED · NONE")
	void deviceStatusPortConvertsAuthMemberDeviceStatusToScheduleDeviceStatus() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);
		long connectedUserId = UNUSED_USER_ID.incrementAndGet();
		long pendingUserId = UNUSED_USER_ID.incrementAndGet();
		long expiredUserId = UNUSED_USER_ID.incrementAndGet();
		long bareUserId = UNUSED_USER_ID.incrementAndGet();
		Instant now = Instant.now();
		saveDevicePort.saveDevice(liveUserDevice(connectedUserId, now));
		saveInvitePort.saveInvite(MemberInvite.issue(pendingUserId, uniqueInviteToken(), now));
		saveInvitePort.saveInvite(MemberInvite.issue(expiredUserId, uniqueInviteToken(),
				now.minus(LONGER_THAN_INVITE_VALIDITY)));

		Map<Long, DeviceStatus> statuses = deviceStatusPort.loadDeviceStatuses(
				List.of(connectedUserId, pendingUserId, expiredUserId, bareUserId));

		assertThat(statuses).containsExactlyInAnyOrderEntriesOf(Map.of(
				connectedUserId, DeviceStatus.CONNECTED,
				pendingUserId, DeviceStatus.INVITE_PENDING,
				expiredUserId, DeviceStatus.INVITE_EXPIRED,
				bareUserId, DeviceStatus.NONE));
	}

	@Test
	@DisplayName("LoadMemberDeviceStatusPort에 빈 목록을 주면 빈 Map을 돌려준다")
	void deviceStatusPortReturnsEmptyMapForEmptyList() {
		LoadMemberDeviceStatusPort deviceStatusPort = onlyBeanOf(LoadMemberDeviceStatusPort.class);

		assertThat(deviceStatusPort.loadDeviceStatuses(List.of())).isNotNull().isEmpty();
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
		assertThat(beans).as("%s 빈 개수", type.getName()).hasSize(1);
		return beans.values().iterator().next();
	}
}
