package com.example.pickupservice.pickup.adapter.out;

import com.example.pickupservice.auth.application.port.in.GetMemberDeviceStatusUseCase;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.out.LoadChildReadinessPort;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSchedulePort;
import com.example.pickupservice.pickup.application.port.out.LoadSpotPort;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import com.example.pickupservice.pickup.application.port.out.RecordNotificationPort;
import com.example.pickupservice.pickup.application.port.out.SaveTemporarySpotPort;
import com.example.pickupservice.place.application.port.in.LoadPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.place.application.port.in.SpotSummary;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.application.port.in.LoadScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.time.DayOfWeek;
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
class PickupModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String OUT_ADAPTER_PACKAGE = "com.example.pickupservice.pickup.adapter.out.";
	private static final Duration LONGER_THAN_INVITE_VALIDITY = Duration.ofHours(25);
	private static final long MISSING_ID = Long.MAX_VALUE - 7;
	private static final AtomicLong UNUSED_USER_ID =
			new AtomicLong(6_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L) * 1_000L);

	private final ApplicationContext applicationContext;
	private final UserSeed userSeed;
	private final ScheduleSeed scheduleSeed;
	private final PlaceSeed placeSeed;
	private final LoadScheduleUseCase loadScheduleUseCase;
	private final LoadPickupSpotUseCase loadPickupSpotUseCase;
	private final LoadFamilyUseCase loadFamilyUseCase;
	private final LoadMemberUseCase loadMemberUseCase;
	private final GetMemberDeviceStatusUseCase getMemberDeviceStatusUseCase;
	private final SaveDevicePort saveDevicePort;
	private final SaveInvitePort saveInvitePort;

	private UserSeed.Result seededUser;
	private ScheduleSeed.Result seededSchedule;
	private PlaceSeed.Result seededPlace;

	@AfterEach
	void cleanUp() {
		if (seededPlace != null) {
			placeSeed.clear(seededPlace);
		}
		if (seededSchedule != null) {
			scheduleSeed.clear(seededSchedule);
		}
		if (seededUser != null) {
			userSeed.clear(seededUser);
		}
	}

	@Test
	@DisplayName("pickup의 연결 out 포트 5개는 각각 빈이 정확히 1개이고 pickup.adapter.out.<모듈> 아래 클래스가 구현한다: schedule · place · user · auth · notification")
	void eachLinkPortHasExactlyOneBeanInPickupOutAdapterPackageOfThatModule() {
		Map<Class<?>, String> expectedPackages = Map.of(
				LoadSchedulePort.class, OUT_ADAPTER_PACKAGE + "schedule.",
				LoadSpotPort.class, OUT_ADAPTER_PACKAGE + "place.",
				LoadFamilyPort.class, OUT_ADAPTER_PACKAGE + "user.",
				LoadChildReadinessPort.class, OUT_ADAPTER_PACKAGE + "auth.",
				RecordNotificationPort.class, OUT_ADAPTER_PACKAGE + "notification.");

		expectedPackages.forEach((port, expectedPackage) -> {
			Object bean = onlyBeanOf(port);
			assertThat(AopUtils.getTargetClass(bean).getName())
					.as("%s 구현 클래스 위치", port.getSimpleName())
					.startsWith(expectedPackage);
		});
	}

	@Test
	@DisplayName("LoadSchedulePort는 schedule LoadScheduleUseCase.findSchedules에 위임해 시드 일정(민준 · 해솔영어학원 · 월수 · 18:00 · 5분 · READY)을 돌려준다")
	void schedulePortDelegatesToScheduleFindSchedules() {
		LoadSchedulePort schedulePort = onlyBeanOf(LoadSchedulePort.class);
		seededSchedule = scheduleSeed.seed();
		List<Long> scheduleIds = List.of(seededSchedule.scheduleId());

		List<ScheduleInfo> schedules = schedulePort.loadSchedules(scheduleIds);

		assertThat(schedules).isEqualTo(loadScheduleUseCase.findSchedules(scheduleIds));
		assertThat(schedules).singleElement().satisfies(schedule -> {
			assertThat(schedule.scheduleId()).isEqualTo(seededSchedule.scheduleId());
			assertThat(schedule.childUserId()).isEqualTo(seededSchedule.user().minjunId());
			assertThat(schedule.academy().academyId()).isEqualTo(seededSchedule.academyId());
			assertThat(schedule.academy().name()).isEqualTo(ScheduleSeed.ACADEMY_NAME);
			assertThat(schedule.weekdays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
			assertThat(schedule.classEndTime()).isEqualTo(ScheduleSeed.CLASS_END_TIME);
			assertThat(schedule.targetPickup()).isEqualTo(ScheduleSeed.TARGET_PICKUP);
			assertThat(schedule.status()).isEqualTo(ScheduleStatus.READY);
		});
	}

	@Test
	@DisplayName("LoadSchedulePort는 없는 일정 id만 받으면 빈 목록을 돌려준다")
	void schedulePortReturnsEmptyListForMissingSchedule() {
		LoadSchedulePort schedulePort = onlyBeanOf(LoadSchedulePort.class);

		assertThat(schedulePort.loadSchedules(List.of(MISSING_ID))).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("LoadSpotPort는 place LoadPickupSpotUseCase에 위임해 loadDefaultSpot은 가족 · 학원의 기본 장소(정문 앞), loadSpot은 그 id의 장소(후문 앞)를 돌려준다")
	void spotPortDelegatesToPlaceLoadPickupSpot() {
		LoadSpotPort spotPort = onlyBeanOf(LoadSpotPort.class);
		seededPlace = placeSeed.seed();
		long familyId = seededPlace.schedule().user().familyId();
		long academyId = seededPlace.schedule().academyId();

		SpotInfo defaultSpot = spotPort.loadDefaultSpot(familyId, academyId).orElseThrow();
		SpotInfo otherSpot = spotPort.loadSpot(seededPlace.otherSpotId()).orElseThrow();

		assertThat(defaultSpot).isEqualTo(loadPickupSpotUseCase.findDefaultSpot(familyId, academyId).orElseThrow());
		assertThat(defaultSpot.spotId()).isEqualTo(seededPlace.defaultSpotId());
		assertThat(defaultSpot.familyId()).isEqualTo(familyId);
		assertThat(defaultSpot.academyId()).isEqualTo(academyId);
		assertThat(defaultSpot.name()).isEqualTo(PlaceSeed.DEFAULT_SPOT_NAME);
		assertThat(otherSpot).isEqualTo(loadPickupSpotUseCase.findSpot(seededPlace.otherSpotId()).orElseThrow());
		assertThat(otherSpot.spotId()).isEqualTo(seededPlace.otherSpotId());
		assertThat(otherSpot.name()).isEqualTo(PlaceSeed.OTHER_SPOT_NAME);
	}

	@Test
	@DisplayName("SaveTemporarySpotPort는 빈이 정확히 1개이고 pickup.adapter.out.place 아래 클래스가 구현한다")
	void temporarySpotPortHasExactlyOneBeanInPickupOutPlacePackage() {
		Object bean = onlyBeanOf(SaveTemporarySpotPort.class);

		assertThat(AopUtils.getTargetClass(bean).getName()).startsWith(OUT_ADAPTER_PACKAGE + "place.");
	}

	@Test
	@DisplayName("SaveTemporarySpotPort.saveTemporarySpot은 place SaveTemporarySpotUseCase에 위임해 같은 가족 · 학원 · 이름 · 정차 지점 · 만남 지점의 임시 장소를 만들고 그 spotId를 돌려준다(가족 장소 목록에는 안 보인다)")
	void temporarySpotPortDelegatesToPlaceSaveTemporarySpot() {
		SaveTemporarySpotPort temporarySpotPort = onlyBeanOf(SaveTemporarySpotPort.class);
		seededPlace = placeSeed.seed();
		long familyId = seededPlace.schedule().user().familyId();
		long academyId = seededPlace.schedule().academyId();
		LatLng location = new LatLng(37.500900, 127.040500);

		long spotId = temporarySpotPort.saveTemporarySpot(familyId, academyId, "학원 옆 편의점 앞", location, location);

		assertThat(spotId).isPositive().isNotIn(seededPlace.defaultSpotId(), seededPlace.otherSpotId());
		assertThat(loadPickupSpotUseCase.findSpot(spotId)).contains(
				new SpotInfo(spotId, familyId, academyId, "학원 옆 편의점 앞", location, location));
		assertThat(loadPickupSpotUseCase.findSpots(familyId, List.of(academyId)))
				.extracting(SpotSummary::spotId)
				.doesNotContain(spotId);
	}

	@Test
	@DisplayName("LoadSpotPort는 없는 장소 id, 기본 장소가 없는 가족 · 학원이면 빈 값을 돌려준다")
	void spotPortReturnsEmptyForMissingSpot() {
		LoadSpotPort spotPort = onlyBeanOf(LoadSpotPort.class);

		assertThat(spotPort.loadSpot(MISSING_ID)).isNotNull().isEmpty();
		assertThat(spotPort.loadDefaultSpot(MISSING_ID, MISSING_ID)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("LoadFamilyPort는 user LoadFamilyUseCase · LoadMemberUseCase에 위임해 loadFamily는 가족 정보(AUTO), loadMembers는 보호자 · 민준 · 서윤을 돌려준다")
	void familyPortDelegatesToUserLoadFamilyAndLoadMember() {
		LoadFamilyPort familyPort = onlyBeanOf(LoadFamilyPort.class);
		seededUser = userSeed.seed();
		long familyId = seededUser.familyId();

		FamilyInfo family = familyPort.loadFamily(familyId);
		List<MemberInfo> members = familyPort.loadMembers(familyId);

		assertThat(family).isEqualTo(loadFamilyUseCase.findFamily(familyId));
		assertThat(family.familyId()).isEqualTo(familyId);
		assertThat(family.joinCallMode()).isEqualTo(JoinCallMode.AUTO);
		assertThat(members).containsExactlyInAnyOrderElementsOf(loadMemberUseCase.findMembers(familyId));
		assertThat(members).extracting(MemberInfo::userId)
				.containsExactlyInAnyOrder(seededUser.guardianId(), seededUser.minjunId(), seededUser.seoyunId());
		assertThat(members).extracting(MemberInfo::name)
				.containsExactlyInAnyOrder(UserSeed.GUARDIAN_NAME, UserSeed.MINJUN_NAME, UserSeed.SEOYUN_NAME);
	}

	@Test
	@DisplayName("LoadFamilyPort.loadMember는 user LoadMemberUseCase.findMember에 위임해 그 사용자(민준 · CHILD · 시드 가족)의 MemberInfo를 돌려주고 없는 사용자 id면 빈 값을 돌려준다")
	void familyPortLoadMemberDelegatesToUserFindMember() {
		LoadFamilyPort familyPort = onlyBeanOf(LoadFamilyPort.class);
		seededUser = userSeed.seed();
		long minjunId = seededUser.minjunId();

		MemberInfo member = familyPort.loadMember(minjunId).orElseThrow();

		assertThat(member).isEqualTo(loadMemberUseCase.findMember(minjunId).orElseThrow());
		assertThat(member.userId()).isEqualTo(minjunId);
		assertThat(member.familyId()).isEqualTo(seededUser.familyId());
		assertThat(member.role()).isEqualTo(Role.CHILD);
		assertThat(member.name()).isEqualTo(UserSeed.MINJUN_NAME);
		assertThat(familyPort.loadMember(MISSING_ID)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("LoadChildReadinessPort.loadDeviceStatuses는 auth GetMemberDeviceStatusUseCase 판정을 그대로 돌려준다: CONNECTED · INVITE_PENDING · INVITE_EXPIRED · NONE")
	void childReadinessPortReturnsAuthMemberDeviceStatuses() {
		LoadChildReadinessPort readinessPort = onlyBeanOf(LoadChildReadinessPort.class);
		long connectedUserId = UNUSED_USER_ID.incrementAndGet();
		long pendingUserId = UNUSED_USER_ID.incrementAndGet();
		long expiredUserId = UNUSED_USER_ID.incrementAndGet();
		long bareUserId = UNUSED_USER_ID.incrementAndGet();
		Instant now = Instant.now();
		saveDevicePort.saveDevice(liveUserDevice(connectedUserId, now));
		saveInvitePort.saveInvite(MemberInvite.issue(pendingUserId, uniqueInviteToken(), now));
		saveInvitePort.saveInvite(MemberInvite.issue(expiredUserId, uniqueInviteToken(),
				now.minus(LONGER_THAN_INVITE_VALIDITY)));
		List<Long> userIds = List.of(connectedUserId, pendingUserId, expiredUserId, bareUserId);

		Map<Long, MemberDeviceStatus> statuses = readinessPort.loadDeviceStatuses(userIds);

		assertThat(statuses).containsExactlyInAnyOrderEntriesOf(Map.of(
				connectedUserId, MemberDeviceStatus.CONNECTED,
				pendingUserId, MemberDeviceStatus.INVITE_PENDING,
				expiredUserId, MemberDeviceStatus.INVITE_EXPIRED,
				bareUserId, MemberDeviceStatus.NONE));
		assertThat(statuses).isEqualTo(getMemberDeviceStatusUseCase.getDeviceStatuses(userIds));
	}

	@Test
	@DisplayName("LoadChildReadinessPort.loadDeviceStatuses에 빈 목록을 주면 빈 Map을 돌려준다")
	void childReadinessPortReturnsEmptyMapForEmptyList() {
		LoadChildReadinessPort readinessPort = onlyBeanOf(LoadChildReadinessPort.class);

		assertThat(readinessPort.loadDeviceStatuses(List.of())).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("auth 위치 동의 저장 전 임시 LoadChildReadinessPort.hasLocationConsent는 어떤 자녀에게도 true를 돌려준다(CHILD 초대 수락에 LOCATION 동의가 필수)")
	void temporaryLocationConsentIsAlwaysTrue() {
		LoadChildReadinessPort readinessPort = onlyBeanOf(LoadChildReadinessPort.class);

		assertThat(readinessPort.hasLocationConsent(1L)).isTrue();
		assertThat(readinessPort.hasLocationConsent(UNUSED_USER_ID.incrementAndGet())).isTrue();
		assertThat(readinessPort.hasLocationConsent(MISSING_ID)).isTrue();
	}

	@Test
	@DisplayName("RecordNotificationPort.recordNotifications는 받는 사람이 없으면 알림을 만들지 않고 빈 목록을 돌려준다(실제 notification 연결 뒤에도 같다)")
	void notificationPortReturnsEmptyListWhenNoReceivers() {
		RecordNotificationPort notificationPort = onlyBeanOf(RecordNotificationPort.class);

		assertThat(notificationPort.recordNotifications(
				new NotificationRequest(3L, null, "HELP", List.of()))).isNotNull().isEmpty();
	}

	private static Device liveUserDevice(long userId, Instant at) {
		return Device.register(OwnerType.USER, userId, Platform.ANDROID, "push-" + UUID.randomUUID(), at,
				randomSha256Hex(), at.plus(Duration.ofDays(14)));
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
