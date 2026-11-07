package com.example.pickupservice.schedule.adapter.out;

import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.GetSetupSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.SetupSpot;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import com.example.pickupservice.schedule.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.schedule.application.port.out.LoadPickupSpotPort;
import com.example.pickupservice.schedule.application.port.out.SpotSummary;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

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
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class})
class ScheduleModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String OUT_ADAPTER_PACKAGE = "com.example.pickupservice.schedule.adapter.out.";
	private static final Duration LONGER_THAN_INVITE_VALIDITY = Duration.ofHours(25);
	private static final AtomicLong UNUSED_USER_ID =
			new AtomicLong(7_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L) * 1_000L);

	private final ApplicationContext applicationContext;
	private final SaveDevicePort saveDevicePort;
	private final SaveInvitePort saveInvitePort;
	private final PlaceSeed placeSeed;
	private final GetSetupSummaryUseCase getSetupSummaryUseCase;

	private PlaceSeed.Result seeded;

	@AfterEach
	void cleanUp() {
		if (seeded != null) {
			placeSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("LoadPickupSpotPort는 place findSpots에 위임해 그 가족 · 학원 목록의 실제 장소를 spotId · academyId · name · isDefault로 돌려준다")
	void pickupSpotPortReturnsRealSpotsOfFamilyAndAcademiesFromPlace() {
		LoadPickupSpotPort pickupSpotPort = onlyBeanOf(LoadPickupSpotPort.class);
		seeded = placeSeed.seed();
		long familyId = seeded.schedule().user().familyId();
		long academyId = seeded.schedule().academyId();

		List<SpotSummary> spots = pickupSpotPort.loadSpots(familyId, List.of(academyId));

		assertThat(spots).containsExactlyInAnyOrder(
				new SpotSummary(seeded.defaultSpotId(), academyId, PlaceSeed.DEFAULT_SPOT_NAME, true),
				new SpotSummary(seeded.otherSpotId(), academyId, PlaceSeed.OTHER_SPOT_NAME, false));
	}

	@Test
	@DisplayName("LoadPickupSpotPort는 다른 가족의 장소와 학원 목록에 없는 학원의 장소를 돌려주지 않는다")
	void pickupSpotPortExcludesOtherFamilyAndAcademiesNotInList() {
		LoadPickupSpotPort pickupSpotPort = onlyBeanOf(LoadPickupSpotPort.class);
		seeded = placeSeed.seed();
		long familyId = seeded.schedule().user().familyId();
		long academyId = seeded.schedule().academyId();
		long otherFamilyId = UNUSED_USER_ID.incrementAndGet();
		long academyNotInSeed = academyId + 1_000_000L;

		assertThat(pickupSpotPort.loadSpots(otherFamilyId, List.of(academyId))).isNotNull().isEmpty();
		assertThat(pickupSpotPort.loadSpots(familyId, List.of(academyNotInSeed))).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("설정 요약은 시드 가족 · 민준의 해솔영어학원에 기본 장소가 있으면 missing에 PICKUP_SPOT을 넣지 않고 그 장소를 보여준다")
	void setupSummaryDoesNotReportPickupSpotMissingWhenSeededDefaultSpotExists() {
		seeded = placeSeed.seed();
		long familyId = seeded.schedule().user().familyId();
		long minjunId = seeded.schedule().user().minjunId();

		SetupSummary summary = getSetupSummaryUseCase.getSetupSummary(new ChildQuery(familyId, minjunId));

		assertThat(summary.missing()).doesNotContain(MissingSetupItem.PICKUP_SPOT);
		assertThat(summary.pickupSpots()).containsExactlyInAnyOrder(
				new SetupSpot(seeded.defaultSpotId(), PlaceSeed.DEFAULT_SPOT_NAME, true),
				new SetupSpot(seeded.otherSpotId(), PlaceSeed.OTHER_SPOT_NAME, false));
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
