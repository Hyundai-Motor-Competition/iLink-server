package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.auth.fixture.AuthSeed;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
class AuthPersistenceAdapterTest extends IntegrationTestSupport {

	private static final Instant AT = Instant.parse("2026-11-07T01:02:03.123456Z");
	private static final Instant LATER = Instant.parse("2026-11-07T03:04:05.654321Z");
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

	private final SaveDevicePort saveDevicePort;
	private final LoadDevicePort loadDevicePort;
	private final SaveLinkCodePort saveLinkCodePort;
	private final LoadLinkCodePort loadLinkCodePort;
	private final SaveInvitePort saveInvitePort;
	private final LoadInvitePort loadInvitePort;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;

	private final List<Long> deviceIds = new ArrayList<>();
	private final List<Long> codeIds = new ArrayList<>();
	private final List<String> inviteTokens = new ArrayList<>();

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> {
			deleteRows("delete from DeviceEntity d where d.deviceId in :values", deviceIds);
			deleteRows("delete from VehicleLinkCodeEntity c where c.codeId in :values", codeIds);
			deleteRows("delete from MemberInviteEntity i where i.token in :values", inviteTokens);
		});
	}

	@Test
	@DisplayName("Device를 저장한 뒤 다시 읽으면 도메인 객체의 값이 같다")
	void savedDeviceIsLoadedWithSameValues() {
		long userId = randomId();
		String hash = AuthSeed.sha256(randomToken());
		Device saved = saveDevice(Device.restore(null, OwnerType.USER, userId, Platform.ANDROID, "fcm-token-1", AT,
				hash, LATER));
		assertThat(saved.getDeviceId()).as("저장된 deviceId").isNotNull();
		clearPersistenceContext();

		assertThat(loadDevicePort.loadDevice(saved.getDeviceId())).get()
				.usingRecursiveComparison()
				.isEqualTo(Device.restore(saved.getDeviceId(), OwnerType.USER, userId, Platform.ANDROID, "fcm-token-1",
						AT, hash, LATER));
		assertThat(loadDevicePort.loadDevice(Long.MAX_VALUE)).as("없는 deviceId").isEmpty();
	}

	@Test
	@DisplayName("LinkCode를 저장한 뒤 다시 읽으면 도메인 객체의 값이 같다")
	void savedLinkCodeIsLoadedWithSameValues() {
		long vehicleId = randomId();
		long issuedDeviceId = randomId();
		String code = randomCode();
		LinkCode saved = saveLinkCode(LinkCode.restore(null, vehicleId, issuedDeviceId, true, code, LinkCodeStatus.USED,
				LATER, AT, LinkMode.RELINK, LATER));
		assertThat(saved.getCodeId()).as("저장된 codeId").isNotNull();
		clearPersistenceContext();

		assertThat(loadLinkCodePort.loadLinkCode(saved.getCodeId())).get()
				.usingRecursiveComparison()
				.isEqualTo(LinkCode.restore(saved.getCodeId(), vehicleId, issuedDeviceId, true, code, LinkCodeStatus.USED,
						LATER, AT, LinkMode.RELINK, LATER));
		assertThat(loadLinkCodePort.loadLinkCode(Long.MAX_VALUE)).as("없는 codeId").isEmpty();
	}

	@Test
	@DisplayName("MemberInvite를 저장한 뒤 다시 읽으면 도메인 객체의 값이 같다")
	void savedInviteIsLoadedWithSameValues() {
		long userId = randomId();
		String token = randomToken();
		MemberInvite saved = saveInvite(MemberInvite.restore(null, userId, token, LATER, AT));
		assertThat(saved.getInviteId()).as("저장된 inviteId").isNotNull();
		clearPersistenceContext();

		assertThat(loadInvitePort.loadInviteByToken(token)).get()
				.usingRecursiveComparison()
				.isEqualTo(MemberInvite.restore(saved.getInviteId(), userId, token, LATER, AT));
	}

	@Test
	@DisplayName("저장된 Device · LinkCode · MemberInvite를 바꿔 다시 저장하면 새 행 없이 같은 id의 값이 바뀐다")
	void resavingRestoredObjectsUpdatesSameRows() {
		long userId = randomId();
		long vehicleId = randomId();
		long issuedDeviceId = randomId();
		Device device = saveDevice(Device.register(OwnerType.USER, userId, Platform.IOS, null, AT,
				AuthSeed.sha256(randomToken())));
		LinkCode linkCode = saveLinkCode(LinkCode.issue(vehicleId, issuedDeviceId, true, randomCode(), AT));
		String token = randomToken();
		MemberInvite invite = saveInvite(MemberInvite.issue(userId, token, AT));
		clearPersistenceContext();

		String newHash = AuthSeed.sha256(randomToken());
		Device loadedDevice = loadDevicePort.loadDevice(device.getDeviceId()).orElseThrow();
		loadedDevice.updatePushToken("fcm-new", LATER);
		loadedDevice.rotateRefreshToken(newHash);
		loadedDevice.revoke(LATER);
		saveDevicePort.saveDevice(loadedDevice);

		LinkCode loadedCode = loadLinkCodePort.loadLinkCode(linkCode.getCodeId()).orElseThrow();
		loadedCode.approve(LinkMode.NEW, LATER);
		saveLinkCodePort.saveLinkCode(loadedCode);

		MemberInvite loadedInvite = loadInvitePort.loadInviteByToken(token).orElseThrow();
		loadedInvite.markUsed(LATER);
		saveInvitePort.saveInvite(loadedInvite);
		clearPersistenceContext();

		assertThat(loadDevicePort.loadDevice(device.getDeviceId())).get()
				.usingRecursiveComparison()
				.isEqualTo(Device.restore(device.getDeviceId(), OwnerType.USER, userId, Platform.IOS, "fcm-new",
						LATER, newHash, LATER));
		assertThat(loadLinkCodePort.loadLinkCode(linkCode.getCodeId())).get()
				.usingRecursiveComparison()
				.isEqualTo(LinkCode.restore(linkCode.getCodeId(), vehicleId, issuedDeviceId, true, linkCode.getCode(),
						LinkCodeStatus.USED, linkCode.getExpiresAt(), LATER, LinkMode.NEW, null));
		assertThat(loadInvitePort.loadLatestInvite(userId)).get()
				.usingRecursiveComparison()
				.isEqualTo(MemberInvite.restore(invite.getInviteId(), userId, token, invite.getExpiresAt(), LATER));
		assertThat(countRows("select count(d) from DeviceEntity d where d.ownerId = :value", userId))
				.as("기기 행 수").isEqualTo(1L);
		assertThat(countRows("select count(c) from VehicleLinkCodeEntity c where c.vehicleId = :value", vehicleId))
				.as("연결 코드 행 수").isEqualTo(1L);
		assertThat(countRows("select count(i) from MemberInviteEntity i where i.userId = :value", userId))
				.as("초대 행 수").isEqualTo(1L);
	}

	@Test
	@DisplayName("Device를 refreshTokenHash로 찾는다")
	void loadByRefreshTokenHashFindsDevice() {
		long vehicleId = randomId();
		long userId = randomId();
		String targetHash = AuthSeed.sha256(randomToken());
		String otherHash = AuthSeed.sha256(randomToken());
		Device target = saveDevice(Device.register(OwnerType.VEHICLE, vehicleId, Platform.VEHICLE, null, AT,
				targetHash));
		saveDevice(Device.register(OwnerType.USER, userId, Platform.ANDROID, "fcm-token", AT, otherHash));
		clearPersistenceContext();

		assertThat(loadDevicePort.loadByRefreshTokenHash(targetHash)).get()
				.usingRecursiveComparison()
				.isEqualTo(Device.restore(target.getDeviceId(), OwnerType.VEHICLE, vehicleId, Platform.VEHICLE, null,
						null, targetHash, null));
		assertThat(loadDevicePort.loadByRefreshTokenHash(AuthSeed.sha256(randomToken())))
				.as("저장되지 않은 해시").isEmpty();
	}

	@Test
	@DisplayName("loadLiveDevices는 revokedAt이 빈 기기만 돌려준다")
	void loadLiveDevicesReturnsOnlyDevicesWithoutRevokedAt() {
		long ownerId = randomId();
		long otherUserId = randomId();
		Device liveAndroid = saveDevice(Device.register(OwnerType.USER, ownerId, Platform.ANDROID, "fcm-a", AT,
				AuthSeed.sha256(randomToken())));
		Device liveIos = saveDevice(Device.register(OwnerType.USER, ownerId, Platform.IOS, null, AT,
				AuthSeed.sha256(randomToken())));
		saveDevice(Device.restore(null, OwnerType.USER, ownerId, Platform.ANDROID, "fcm-old", AT,
				AuthSeed.sha256(randomToken()), LATER));
		saveDevice(Device.register(OwnerType.VEHICLE, ownerId, Platform.VEHICLE, null, AT,
				AuthSeed.sha256(randomToken())));
		saveDevice(Device.register(OwnerType.USER, otherUserId, Platform.ANDROID, "fcm-other", AT,
				AuthSeed.sha256(randomToken())));
		clearPersistenceContext();

		List<Device> liveDevices = loadDevicePort.loadLiveDevices(OwnerType.USER, ownerId);

		assertThat(liveDevices)
				.extracting(Device::getDeviceId)
				.as("같은 USER의 살아 있는 기기만, 폐기 · VEHICLE 같은 번호 · 다른 사용자 기기는 빠짐")
				.containsExactlyInAnyOrder(liveAndroid.getDeviceId(), liveIos.getDeviceId());
		assertThat(liveDevices).allSatisfy(device -> {
			assertThat(device.getRevokedAt()).isNull();
			assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
			assertThat(device.getOwnerId()).isEqualTo(ownerId);
		});
		assertThat(loadDevicePort.loadLiveDevices(OwnerType.USER, randomId())).as("기기가 없는 사용자").isEmpty();
	}

	@Test
	@DisplayName("loadLinkCodeByCode는 8자 code로 연결 코드를 찾는다")
	void loadLinkCodeByCodeFindsLinkCodeByEightCharacterCode() {
		long vehicleId = randomId();
		long issuedDeviceId = randomId();
		String code = randomCode();
		assertThat(code).hasSize(8);
		LinkCode target = saveLinkCode(LinkCode.issue(vehicleId, issuedDeviceId, false, code, AT));
		saveLinkCode(LinkCode.issue(randomId(), randomId(), false, randomCode(), AT));
		clearPersistenceContext();

		assertThat(loadLinkCodePort.loadLinkCodeByCode(code)).get()
				.usingRecursiveComparison()
				.isEqualTo(LinkCode.restore(target.getCodeId(), vehicleId, issuedDeviceId, false, code, LinkCodeStatus.ISSUED,
						AT.plusSeconds(300), null, null, null));
		assertThat(loadLinkCodePort.loadLinkCodeByCode(randomCode())).as("없는 code").isEmpty();
	}

	@Test
	@DisplayName("loadIssuedCodes는 그 차량의 ISSUED 코드만 돌려준다")
	void loadIssuedCodesReturnsOnlyIssuedCodesOfThatVehicle() {
		long vehicleId = randomId();
		long otherVehicleId = randomId();
		LinkCode issuedFirst = saveLinkCode(LinkCode.issue(vehicleId, randomId(), false, randomCode(), AT));
		LinkCode issuedSecond = saveLinkCode(LinkCode.issue(vehicleId, randomId(), false, randomCode(), LATER));
		saveLinkCode(LinkCode.restore(null, vehicleId, randomId(), false, randomCode(), LinkCodeStatus.EXPIRED,
				AT.plusSeconds(300), null, null, null));
		saveLinkCode(LinkCode.restore(null, vehicleId, randomId(), false, randomCode(), LinkCodeStatus.USED,
				AT.plusSeconds(300), AT, LinkMode.NEW, null));
		saveLinkCode(LinkCode.issue(otherVehicleId, randomId(), false, randomCode(), AT));
		clearPersistenceContext();

		List<LinkCode> issuedCodes = loadLinkCodePort.loadIssuedCodes(vehicleId);

		assertThat(issuedCodes)
				.extracting(LinkCode::getCodeId)
				.as("그 차량의 ISSUED 코드만, EXPIRED · USED · 다른 차량 코드는 빠짐")
				.containsExactlyInAnyOrder(issuedFirst.getCodeId(), issuedSecond.getCodeId());
		assertThat(issuedCodes).allSatisfy(code -> {
			assertThat(code.getStatus()).isEqualTo(LinkCodeStatus.ISSUED);
			assertThat(code.getVehicleId()).isEqualTo(vehicleId);
		});
		assertThat(loadLinkCodePort.loadIssuedCodes(randomId())).as("코드가 없는 차량").isEmpty();
	}

	@Test
	@DisplayName("MemberInvite를 token으로 찾고 같은 token 두 개는 유니크 위반이 난다")
	void loadInviteByTokenFindsInviteAndDuplicateTokenViolatesUniqueConstraint() {
		long userId = randomId();
		String token = randomToken();
		MemberInvite saved = saveInvite(MemberInvite.issue(userId, token, AT));
		saveInvite(MemberInvite.issue(randomId(), randomToken(), AT));
		clearPersistenceContext();

		assertThat(loadInvitePort.loadInviteByToken(token)).get()
				.usingRecursiveComparison()
				.isEqualTo(MemberInvite.restore(saved.getInviteId(), userId, token, AT.plusSeconds(24 * 60 * 60),
						null));
		assertThat(loadInvitePort.loadInviteByToken(randomToken())).as("없는 token").isEmpty();

		assertThatThrownBy(() -> saveInvitePort.saveInvite(MemberInvite.issue(randomId(), token, LATER)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(countRows("select count(i) from MemberInviteEntity i where i.token = :value", token))
				.as("같은 token 행 수").isEqualTo(1L);
	}

	@Test
	@DisplayName("loadLatestInvite는 그 멤버의 가장 최근 초대를 돌려준다")
	void loadLatestInviteReturnsMostRecentInviteOfThatMember() {
		long userId = randomId();
		long otherUserId = randomId();
		saveInvite(MemberInvite.restore(null, userId, randomToken(), LATER.plusSeconds(3600), null));
		saveInvite(MemberInvite.restore(null, userId, randomToken(), LATER.plusSeconds(7200), AT));
		String latestToken = randomToken();
		MemberInvite latest = saveInvite(MemberInvite.restore(null, userId, latestToken, LATER, null));
		saveInvite(MemberInvite.restore(null, otherUserId, randomToken(), LATER, null));
		clearPersistenceContext();

		assertThat(loadInvitePort.loadLatestInvite(userId)).get()
				.as("inviteId가 가장 큰(가장 나중에 저장된) 초대, expiresAt이 더 이른 값이어도")
				.usingRecursiveComparison()
				.isEqualTo(MemberInvite.restore(latest.getInviteId(), userId, latestToken, LATER, null));
		assertThat(loadInvitePort.loadLatestInvite(randomId())).as("초대가 없는 멤버").isEmpty();
	}

	private Device saveDevice(Device device) {
		Device saved = saveDevicePort.saveDevice(device);
		deviceIds.add(saved.getDeviceId());
		return saved;
	}

	private LinkCode saveLinkCode(LinkCode linkCode) {
		LinkCode saved = saveLinkCodePort.saveLinkCode(linkCode);
		codeIds.add(saved.getCodeId());
		return saved;
	}

	private MemberInvite saveInvite(MemberInvite invite) {
		inviteTokens.add(invite.getToken());
		return saveInvitePort.saveInvite(invite);
	}

	private void deleteRows(String jpql, List<?> values) {
		if (values.isEmpty()) {
			return;
		}
		entityManager.createQuery(jpql).setParameter("values", values).executeUpdate();
	}

	private long countRows(String jpql, Object value) {
		return transactionTemplate.execute(status -> entityManager.createQuery(jpql, Long.class)
				.setParameter("value", value)
				.getSingleResult());
	}

	private void clearPersistenceContext() {
		transactionTemplate.executeWithoutResult(status -> entityManager.clear());
	}

	private static long randomId() {
		return 9_000_000_000L + ThreadLocalRandom.current().nextLong(1_000_000_000L);
	}

	private static String randomCode() {
		StringBuilder builder = new StringBuilder(8);
		for (int i = 0; i < 8; i++) {
			builder.append(CODE_ALPHABET.charAt(ThreadLocalRandom.current().nextInt(CODE_ALPHABET.length())));
		}
		return builder.toString();
	}

	private static String randomToken() {
		return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
	}

}
