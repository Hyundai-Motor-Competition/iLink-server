package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.adapter.out.token.JwtTokenIssuer;
import com.example.pickupservice.auth.application.port.in.AcceptInviteCommand;
import com.example.pickupservice.auth.application.port.in.AcceptInviteResult;
import com.example.pickupservice.auth.application.port.in.ConsentAgreement;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptInviteServiceTest {

	private static final byte[] SECRET_BYTES = "pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final long FAMILY_ID = 12L;
	private static final long CHILD_ID = 33L;
	private static final long TEACHER_ID = 34L;
	private static final long INVITE_ID = 701L;
	private static final long NEW_DEVICE_ID = 902L;
	private static final long OLD_DEVICE_ID_1 = 401L;
	private static final long OLD_DEVICE_ID_2 = 402L;
	private static final String OLD_REFRESH_HASH = "0".repeat(64);
	private static final String TOKEN = "inv-token-0123456789";
	private static final String PUSH_TOKEN = "fcm-child-token";

	@Mock
	private LoadInvitePort loadInvitePort;

	@Mock
	private SaveInvitePort saveInvitePort;

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Spy
	private JwtTokenIssuer issueTokenPort = new JwtTokenIssuer(SECRET, ACCESS_TTL, REFRESH_TTL,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Mock
	private Clock clock;

	@InjectMocks
	private AcceptInviteService acceptInviteService;

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(loadMemberUseCase.findMember(CHILD_ID))
				.thenReturn(Optional.of(new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));
		lenient().when(loadMemberUseCase.findMember(TEACHER_ID))
				.thenReturn(Optional.of(new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", null)));
		lenient().when(saveInvitePort.saveInvite(any(MemberInvite.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> {
			Device device = invocation.getArgument(0);
			if (device.getDeviceId() != null) {
				return device;
			}
			return Device.restore(NEW_DEVICE_ID, device.getOwnerType(), device.getOwnerId(), device.getPlatform(),
					device.getPushToken(), device.getPushUpdatedAt(), device.getRefreshTokenHash(),
					device.getRevokedAt(), device.getRefreshTokenExpiresAt());
		});
	}

	@Test
	@DisplayName("초대 수락은 requiredConsents 중 하나라도 agreed가 아니면 CONSENT_REQUIRED로 거절한다")
	void acceptInviteRejectsWhenRequiredConsentIsNotAgreed() {
		givenInvite(liveInvite(CHILD_ID));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(List.of(
						agreed(ConsentType.TERMS), agreed(ConsentType.PRIVACY),
						new ConsentAgreement(ConsentType.LOCATION, false), agreed(ConsentType.PUSH)), PUSH_TOKEN)),
				HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("초대 수락은 CHILD의 requiredConsents 중 PUSH가 빠지면 CONSENT_REQUIRED로 거절한다")
	void acceptInviteRejectsWhenChildPushConsentIsMissing() {
		givenInvite(liveInvite(CHILD_ID));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(List.of(
						agreed(ConsentType.TERMS), agreed(ConsentType.PRIVACY), agreed(ConsentType.LOCATION)),
						PUSH_TOKEN)),
				HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("초대 수락은 CHILD가 아닌 멤버면 TERMS · PRIVACY 동의만으로 수락한다")
	void acceptInviteAcceptsNonChildWithBasicConsents() {
		givenInvite(liveInvite(TEACHER_ID));

		AcceptInviteResult result = acceptInviteService.acceptInvite(TOKEN,
				command(List.of(agreed(ConsentType.TERMS), agreed(ConsentType.PRIVACY)), PUSH_TOKEN));

		assertThat(result.userId()).isEqualTo(TEACHER_ID);
		assertThat(result.role()).isEqualTo(Role.TEACHER);
	}

	@Test
	@DisplayName("초대 수락은 Device를 만들고 pushToken을 저장하고 usedAt을 채운다")
	void acceptInviteCreatesDeviceWithPushTokenAndMarksInviteUsed() {
		givenInvite(liveInvite(CHILD_ID));

		AcceptInviteResult result = acceptInviteService.acceptInvite(TOKEN,
				new AcceptInviteCommand(childConsents(), Platform.IOS, PUSH_TOKEN));

		Device device = lastSavedDeviceOf(CHILD_ID);
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
		assertThat(device.getPlatform()).isEqualTo(Platform.IOS);
		assertThat(device.getPushToken()).isEqualTo(PUSH_TOKEN);
		assertThat(device.getPushUpdatedAt()).isEqualTo(NOW);
		assertThat(device.getRevokedAt()).isNull();
		assertThat(device.getRefreshTokenHash()).isEqualTo(issueTokenPort.hashRefreshToken(result.refreshToken()));

		ArgumentCaptor<MemberInvite> savedInvite = ArgumentCaptor.forClass(MemberInvite.class);
		verify(saveInvitePort, atLeastOnce()).saveInvite(savedInvite.capture());
		assertThat(savedInvite.getValue().getInviteId()).isEqualTo(INVITE_ID);
		assertThat(savedInvite.getValue().getUsedAt()).isEqualTo(NOW);

		assertThat(result.userId()).isEqualTo(CHILD_ID);
		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
		assertThat(result.role()).isEqualTo(Role.CHILD);
		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();

		Jwt jwt = decode(result.accessToken());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(CHILD_ID));
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("USER");
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(FAMILY_ID);
		assertThat(jwt.getClaimAsString("role")).isEqualTo("CHILD");
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(NEW_DEVICE_ID);
	}

	@Test
	@DisplayName("초대 수락이 만든 Device의 refresh 토큰 만료 시각은 발급자가 준 refreshTokenExpiresAt(발급 시각 + 14일)이다")
	void acceptInviteStoresRefreshTokenExpiryFromIssuedTokens() {
		givenInvite(liveInvite(CHILD_ID));
		List<IssuedTokens> issued = new ArrayList<>();
		doAnswer(invocation -> {
			IssuedTokens tokens = (IssuedTokens) invocation.callRealMethod();
			issued.add(tokens);
			return tokens;
		}).when(issueTokenPort).issueTokens(any(TokenSubject.class));

		AcceptInviteResult result = acceptInviteService.acceptInvite(TOKEN,
				new AcceptInviteCommand(childConsents(), Platform.IOS, PUSH_TOKEN));

		assertThat(issued).hasSize(1);
		Device device = lastSavedDeviceOf(CHILD_ID);
		assertThat(device.getRefreshTokenHash()).isEqualTo(issueTokenPort.hashRefreshToken(result.refreshToken()));
		assertThat(device.getRefreshTokenExpiresAt())
				.isEqualTo(issued.getFirst().refreshTokenExpiresAt())
				.isEqualTo(NOW.plus(REFRESH_TTL));
	}

	@Test
	@DisplayName("사용된 초대를 다시 수락하면 INVITE_ALREADY_USED로 거절한다")
	void acceptInviteRejectsSecondAcceptanceOfSameInvite() {
		givenInvite(liveInvite(CHILD_ID));
		acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN)),
				HttpStatus.CONFLICT, "INVITE_ALREADY_USED");

		verify(issueTokenPort).issueTokens(any());
	}

	@Test
	@DisplayName("초대 수락은 usedAt이 채워진 초대면 INVITE_ALREADY_USED로 거절한다")
	void acceptInviteRejectsUsedInvite() {
		givenInvite(MemberInvite.restore(INVITE_ID, CHILD_ID, TOKEN, NOW.plus(Duration.ofHours(23)),
				NOW.minusSeconds(60)));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN)),
				HttpStatus.CONFLICT, "INVITE_ALREADY_USED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("만료된 초대를 수락하면 INVITE_EXPIRED로 거절한다")
	void acceptInviteRejectsExpiredInvite() {
		givenInvite(MemberInvite.restore(INVITE_ID, CHILD_ID, TOKEN, NOW.minusSeconds(1), null));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN)),
				HttpStatus.GONE, "INVITE_EXPIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("초대를 다시 발급해 폐기된 이전 초대 토큰으로 수락하면 INVITE_EXPIRED로 거절한다")
	void acceptInviteRejectsPreviousInviteRevokedByReissue() {
		MemberInvite previous = MemberInvite.issue(CHILD_ID, TOKEN, NOW.minus(Duration.ofHours(1)));
		previous.revoke(NOW);
		givenInvite(previous);

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN)),
				HttpStatus.GONE, "INVITE_EXPIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("초대 수락은 없는 토큰이면 NOT_FOUND로 거절한다")
	void acceptInviteRejectsUnknownToken() {
		when(loadInvitePort.loadInviteByToken(TOKEN)).thenReturn(Optional.empty());

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN)),
				HttpStatus.NOT_FOUND, "NOT_FOUND");

		assertNothingChanged();
	}

	@Test
	@DisplayName("초대 수락은 그 멤버의 살아 있는 이전 기기를 모두 폐기한다")
	void acceptInviteRevokesAllLivePreviousDevicesOfMember() {
		givenInvite(liveInvite(CHILD_ID));
		Device oldFirst = liveUserDevice(OLD_DEVICE_ID_1, CHILD_ID);
		Device oldSecond = liveUserDevice(OLD_DEVICE_ID_2, CHILD_ID);
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of(oldFirst, oldSecond));

		AcceptInviteResult result = acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN));

		assertThat(oldFirst.getRevokedAt()).isEqualTo(NOW);
		assertThat(oldSecond.getRevokedAt()).isEqualTo(NOW);

		List<Device> saved = allSavedDevices();
		assertThat(saved).filteredOn(d -> d.getDeviceId() != null && d.getDeviceId() == OLD_DEVICE_ID_1)
				.as("이전 기기 1 폐기 저장")
				.isNotEmpty()
				.allSatisfy(d -> assertThat(d.getRevokedAt()).isEqualTo(NOW));
		assertThat(saved).filteredOn(d -> d.getDeviceId() != null && d.getDeviceId() == OLD_DEVICE_ID_2)
				.as("이전 기기 2 폐기 저장")
				.isNotEmpty()
				.allSatisfy(d -> assertThat(d.getRevokedAt()).isEqualTo(NOW));

		int lastRevokeIndex = Math.max(lastIndexOfDevice(saved, OLD_DEVICE_ID_1), lastIndexOfDevice(saved, OLD_DEVICE_ID_2));
		int newDeviceIndex = firstIndexOfNewDevice(saved);
		assertThat(newDeviceIndex).as("새 Device 저장").isGreaterThanOrEqualTo(0);
		assertThat(lastRevokeIndex).as("이전 기기 폐기는 새 Device 생성보다 먼저").isLessThan(newDeviceIndex);

		Device newDevice = saved.get(newDeviceIndex);
		assertThat(newDevice.getOwnerId()).isEqualTo(CHILD_ID);
		assertThat(newDevice.getRevokedAt()).isNull();
		assertThat(((Number) decode(result.accessToken()).getClaim("deviceId")).longValue()).isEqualTo(NEW_DEVICE_ID);
	}

	@Test
	@DisplayName("초대 수락은 그 멤버에게 살아 있는 이전 기기가 없으면 아무 기기도 폐기하지 않고 새 Device만 만든다")
	void acceptInviteRevokesNothingWhenMemberHasNoLiveDevice() {
		givenInvite(liveInvite(CHILD_ID));
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of());

		acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN));

		verify(loadDevicePort).loadLiveDevices(OwnerType.USER, CHILD_ID);
		List<Device> saved = allSavedDevices();
		assertThat(saved).allSatisfy(d -> {
			assertThat(d.getRevokedAt()).isNull();
			assertThat(d.getOwnerId()).isEqualTo(CHILD_ID);
			assertThat(d.getDeviceId() == null || d.getDeviceId() == NEW_DEVICE_ID)
					.as("새 Device 외의 기기 저장 없음: %s", d.getDeviceId())
					.isTrue();
		});
	}

	@Test
	@DisplayName("초대 수락은 필수 동의가 빠져 거절되면 이전 기기를 폐기하지 않는다")
	void acceptInviteDoesNotRevokePreviousDevicesWhenConsentIsMissing() {
		givenInvite(liveInvite(CHILD_ID));
		Device oldDevice = liveUserDevice(OLD_DEVICE_ID_1, CHILD_ID);
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of(oldDevice));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(List.of(
						agreed(ConsentType.TERMS), agreed(ConsentType.PRIVACY)), PUSH_TOKEN)),
				HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED");

		assertThat(oldDevice.getRevokedAt()).isNull();
		assertNothingChanged();
	}

	@Test
	@DisplayName("초대 수락은 대상 멤버가 해제된 멤버(removed true)면 NOT_FOUND로 거절하고 기기 · 토큰을 만들지 않는다")
	void acceptInviteRejectsRemovedMemberWithoutCreatingDeviceOrToken() {
		when(loadMemberUseCase.findMember(CHILD_ID))
				.thenReturn(Optional.of(new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null, null, true)));
		MemberInvite invite = liveInvite(CHILD_ID);
		givenInvite(invite);
		Device oldDevice = liveUserDevice(OLD_DEVICE_ID_1, CHILD_ID);
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.USER, CHILD_ID)).thenReturn(List.of(oldDevice));

		assertError(() -> acceptInviteService.acceptInvite(TOKEN, command(childConsents(), PUSH_TOKEN)),
				HttpStatus.NOT_FOUND, "NOT_FOUND");

		assertNothingChanged();
		assertThat(invite.getUsedAt()).isNull();
		assertThat(oldDevice.getRevokedAt()).isNull();
	}

	private List<Device> allSavedDevices() {
		ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort, atLeastOnce()).saveDevice(captor.capture());
		return captor.getAllValues();
	}

	private static int lastIndexOfDevice(List<Device> devices, long deviceId) {
		for (int i = devices.size() - 1; i >= 0; i--) {
			Long id = devices.get(i).getDeviceId();
			if (id != null && id == deviceId) {
				return i;
			}
		}
		return -1;
	}

	private static int firstIndexOfNewDevice(List<Device> devices) {
		for (int i = 0; i < devices.size(); i++) {
			if (devices.get(i).getDeviceId() == null) {
				return i;
			}
		}
		return -1;
	}

	private static Device liveUserDevice(long deviceId, long userId) {
		return Device.restore(deviceId, OwnerType.USER, userId, Platform.ANDROID, "fcm-old-" + deviceId,
				NOW.minusSeconds(3600), OLD_REFRESH_HASH, null, NOW.plus(Duration.ofDays(14)));
	}

	private void givenInvite(MemberInvite invite) {
		when(loadInvitePort.loadInviteByToken(TOKEN)).thenReturn(Optional.of(invite));
	}

	private void assertNothingChanged() {
		verify(saveInvitePort, never()).saveInvite(any());
		verify(saveDevicePort, never()).saveDevice(any());
		verify(issueTokenPort, never()).issueTokens(any());
	}

	private Device lastSavedDeviceOf(long userId) {
		ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort, atLeastOnce()).saveDevice(captor.capture());
		List<Device> userDevices = captor.getAllValues().stream()
				.filter(d -> d.getOwnerType() == OwnerType.USER && d.getOwnerId() == userId)
				.toList();
		assertThat(userDevices).as("USER %d 기기 저장", userId).isNotEmpty();
		return userDevices.getLast();
	}

	private static MemberInvite liveInvite(long userId) {
		return MemberInvite.restore(INVITE_ID, userId, TOKEN, NOW.plus(Duration.ofHours(23)), null);
	}

	private static AcceptInviteCommand command(List<ConsentAgreement> consents, String pushToken) {
		return new AcceptInviteCommand(consents, Platform.ANDROID, pushToken);
	}

	private static List<ConsentAgreement> childConsents() {
		return List.of(agreed(ConsentType.TERMS), agreed(ConsentType.PRIVACY), agreed(ConsentType.LOCATION),
				agreed(ConsentType.PUSH));
	}

	private static ConsentAgreement agreed(ConsentType type) {
		return new ConsentAgreement(type, true);
	}

	private static void assertError(ThrowingCallable call, HttpStatus status, String code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.status()).isEqualTo(status);
			assertThat(e.code()).isEqualTo(code);
		});
	}

	private static Jwt decode(String token) {
		return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET_BYTES, "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build()
				.decode(token);
	}
}
