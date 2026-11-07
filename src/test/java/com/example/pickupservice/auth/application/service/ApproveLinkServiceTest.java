package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.adapter.out.token.JwtTokenIssuer;
import com.example.pickupservice.auth.application.port.in.ApproveLinkCommand;
import com.example.pickupservice.auth.application.port.in.ConsentAgreement;
import com.example.pickupservice.auth.application.port.in.VehicleLinkResult;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.PublishAuthEventPort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.CreateGuardianCommand;
import com.example.pickupservice.user.application.port.in.CreateGuardianUseCase;
import com.example.pickupservice.user.application.port.in.GuardianAccount;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
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
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveLinkServiceTest {

	private static final byte[] SECRET_BYTES = "pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final long CODE_ID = 5012L;
	private static final String CODE = "K7P2QX9M";
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long NEW_GUARDIAN_ID = 21L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long CHILD_ID = 33L;
	private static final long NEW_DEVICE_ID = 901L;
	private static final long OLD_DEVICE_ID_1 = 401L;
	private static final long OLD_DEVICE_ID_2 = 402L;
	private static final String GUARDIAN_NAME = "김보호";
	private static final String PUSH_TOKEN = "fcm-token-abc";
	private static final String OLD_REFRESH_HASH = "0".repeat(64);

	@Mock
	private LoadLinkCodePort loadLinkCodePort;

	@Mock
	private SaveLinkCodePort saveLinkCodePort;

	@Mock
	private LoadVehicleUseCase loadVehicleUseCase;

	@Mock
	private CreateGuardianUseCase createGuardianUseCase;

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Spy
	private JwtTokenIssuer issueTokenPort = new JwtTokenIssuer(SECRET, ACCESS_TTL, Clock.fixed(NOW, ZoneOffset.UTC));

	@Mock
	private PublishAuthEventPort publishAuthEventPort;

	@Spy
	private LinkCodeIssueCounter linkCodeIssueCounter = new LinkCodeIssueCounter();

	@Mock
	private Clock clock;

	@InjectMocks
	private ApproveLinkService approveLinkService;

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(saveLinkCodePort.saveLinkCode(any(LinkCode.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> {
			Device device = invocation.getArgument(0);
			if (device.getDeviceId() != null) {
				return device;
			}
			return Device.restore(NEW_DEVICE_ID, device.getOwnerType(), device.getOwnerId(), device.getPlatform(),
					device.getPushToken(), device.getPushUpdatedAt(), device.getRefreshTokenHash(),
					device.getRevokedAt());
		});
		lenient().when(createGuardianUseCase.createGuardian(any(CreateGuardianCommand.class)))
				.thenReturn(new GuardianAccount(FAMILY_ID, NEW_GUARDIAN_ID));
	}

	@Test
	@DisplayName("연결 승인은 mode가 코드의 실제 상태와 다르면 LINK_MODE_MISMATCH로 거절한다")
	void approveLinkRejectsNewModeForCodeOfLinkedVehicle() {
		givenCode(issuedCode());
		givenVehicle(linkedVehicle());

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.CONFLICT, "LINK_MODE_MISMATCH");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 가족이 없는 차량의 코드에 RELINK로 보내면 LINK_MODE_MISMATCH로 거절한다")
	void approveLinkRejectsRelinkModeForCodeOfVehicleWithoutFamily() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		assertError(() -> approveLinkService.approveLink(command(LinkMode.RELINK, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.CONFLICT, "LINK_MODE_MISMATCH");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 familyId가 남아 있어도 연결 해제된 차량의 코드에 RELINK로 보내면 LINK_MODE_MISMATCH로 거절한다")
	void approveLinkRejectsRelinkModeForCodeOfUnlinkedVehicleEvenWithFamilyId() {
		givenCode(issuedCode());
		givenVehicle(new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", false));

		assertError(() -> approveLinkService.approveLink(command(LinkMode.RELINK, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.CONFLICT, "LINK_MODE_MISMATCH");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 TERMS나 PRIVACY 동의가 없으면 CONSENT_REQUIRED로 거절한다")
	void approveLinkRejectsWhenTermsConsentIsMissing() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW,
						List.of(new ConsentAgreement(ConsentType.PRIVACY, true)), PUSH_TOKEN)),
				HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 PRIVACY 동의가 agreed false면 CONSENT_REQUIRED로 거절한다")
	void approveLinkRejectsWhenPrivacyConsentIsNotAgreed() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW,
						List.of(new ConsentAgreement(ConsentType.TERMS, true),
								new ConsentAgreement(ConsentType.PRIVACY, false),
								new ConsentAgreement(ConsentType.MARKETING, true)), PUSH_TOKEN)),
				HttpStatus.UNPROCESSABLE_CONTENT, "CONSENT_REQUIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 MARKETING 동의가 없어도 TERMS · PRIVACY에 동의했으면 승인한다")
	void approveLinkAcceptsWithoutMarketingConsent() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		VehicleLinkResult result = approveLinkService.approveLink(command(LinkMode.NEW,
				List.of(new ConsentAgreement(ConsentType.TERMS, true),
						new ConsentAgreement(ConsentType.PRIVACY, true),
						new ConsentAgreement(ConsentType.MARKETING, false)), PUSH_TOKEN));

		assertThat(result.userId()).isEqualTo(NEW_GUARDIAN_ID);
	}

	@Test
	@DisplayName("연결 승인 NEW는 CreateGuardianUseCase를 불러 가족과 GUARDIAN을 만든다")
	void approveLinkNewCreatesFamilyAndGuardianThroughCreateGuardianUseCase() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		VehicleLinkResult result = approveLinkService.approveLink(
				command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN));

		verify(createGuardianUseCase).createGuardian(new CreateGuardianCommand(VEHICLE_ID, null, GUARDIAN_NAME));
		assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
		assertThat(result.userId()).isEqualTo(NEW_GUARDIAN_ID);
		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();

		Jwt jwt = decode(result.accessToken());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(NEW_GUARDIAN_ID));
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("USER");
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(FAMILY_ID);
		assertThat(jwt.getClaimAsString("role")).isEqualTo("GUARDIAN");
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(NEW_DEVICE_ID);

		verify(loadMemberUseCase, never()).findMembers(anyLong());
		verify(publishAuthEventPort, never()).publishDeviceReplaced(anyList());
	}

	@Test
	@DisplayName("연결 승인 NEW는 요청의 name으로 GUARDIAN을 만든다")
	void approveLinkNewCreatesGuardianWithRequestedName() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		approveLinkService.approveLink(new ApproveLinkCommand(CODE, LinkMode.NEW, requiredConsents(),
				Platform.ANDROID, PUSH_TOKEN, "이지은"));

		ArgumentCaptor<CreateGuardianCommand> created = ArgumentCaptor.forClass(CreateGuardianCommand.class);
		verify(createGuardianUseCase).createGuardian(created.capture());
		assertThat(created.getValue().name()).isEqualTo("이지은");
		assertThat(created.getValue().vehicleId()).isEqualTo(VEHICLE_ID);
	}

	@Test
	@DisplayName("연결 승인 RELINK는 기존 GUARDIAN에게 토큰을 주고 그 GUARDIAN의 이전 기기를 폐기한다")
	void approveLinkRelinkIssuesTokensToExistingGuardianAndRevokesHisPreviousDevices() {
		givenRelinkFamily();
		Device old1 = liveUserDevice(OLD_DEVICE_ID_1, GUARDIAN_ID);
		Device old2 = liveUserDevice(OLD_DEVICE_ID_2, GUARDIAN_ID);
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, GUARDIAN_ID)).thenReturn(List.of(old1, old2));

		VehicleLinkResult result = approveLinkService.approveLink(
				command(LinkMode.RELINK, requiredConsents(), PUSH_TOKEN));

		verify(createGuardianUseCase, never()).createGuardian(any());
		assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
		assertThat(result.userId()).isEqualTo(GUARDIAN_ID);

		Jwt jwt = decode(result.accessToken());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(GUARDIAN_ID));
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("USER");
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(FAMILY_ID);
		assertThat(jwt.getClaimAsString("role")).isEqualTo("GUARDIAN");
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(NEW_DEVICE_ID);

		List<Device> saved = savedDevices();
		assertThat(saved).filteredOn(d -> d.getDeviceId() != null && d.getDeviceId() == OLD_DEVICE_ID_1)
				.isNotEmpty().allSatisfy(d -> assertThat(d.getRevokedAt()).isEqualTo(NOW));
		assertThat(saved).filteredOn(d -> d.getDeviceId() != null && d.getDeviceId() == OLD_DEVICE_ID_2)
				.isNotEmpty().allSatisfy(d -> assertThat(d.getRevokedAt()).isEqualTo(NOW));

		verify(loadDevicePort, never()).loadLiveDevices(OwnerType.USER, CO_GUARDIAN_ID);
		verify(loadDevicePort, never()).loadLiveDevices(OwnerType.USER, CHILD_ID);
	}

	@Test
	@DisplayName("연결 승인 RELINK는 이전 기기 알림 이벤트를 발행한다")
	void approveLinkRelinkPublishesDeviceReplacedEventWithPreviousDeviceIds() {
		givenRelinkFamily();
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, GUARDIAN_ID)).thenReturn(List.of(
				liveUserDevice(OLD_DEVICE_ID_1, GUARDIAN_ID), liveUserDevice(OLD_DEVICE_ID_2, GUARDIAN_ID)));

		approveLinkService.approveLink(command(LinkMode.RELINK, requiredConsents(), PUSH_TOKEN));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Long>> published = ArgumentCaptor.forClass(List.class);
		verify(publishAuthEventPort).publishDeviceReplaced(published.capture());
		assertThat(published.getValue()).containsExactlyInAnyOrder(OLD_DEVICE_ID_1, OLD_DEVICE_ID_2);
	}

	@Test
	@DisplayName("연결 승인 RELINK는 GUARDIAN에게 살아 있는 이전 기기가 없으면 이전 기기 알림 이벤트를 발행하지 않는다")
	void approveLinkRelinkDoesNotPublishEventWhenGuardianHasNoPreviousDevice() {
		givenRelinkFamily();
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, GUARDIAN_ID)).thenReturn(List.of());

		VehicleLinkResult result = approveLinkService.approveLink(
				command(LinkMode.RELINK, requiredConsents(), PUSH_TOKEN));

		assertThat(result.userId()).isEqualTo(GUARDIAN_ID);
		verify(publishAuthEventPort, never()).publishDeviceReplaced(anyList());
	}

	@Test
	@DisplayName("연결 승인은 요청의 pushToken으로 Device를 만든다")
	void approveLinkCreatesDeviceWithRequestedPushToken() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		VehicleLinkResult result = approveLinkService.approveLink(new ApproveLinkCommand(CODE, LinkMode.NEW,
				requiredConsents(), Platform.IOS, PUSH_TOKEN, GUARDIAN_NAME));

		Device device = lastSavedDeviceOf(NEW_GUARDIAN_ID);
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.USER);
		assertThat(device.getPlatform()).isEqualTo(Platform.IOS);
		assertThat(device.getPushToken()).isEqualTo(PUSH_TOKEN);
		assertThat(device.getPushUpdatedAt()).isEqualTo(NOW);
		assertThat(device.getRevokedAt()).isNull();
		assertThat(device.getRefreshTokenHash()).isEqualTo(issueTokenPort.hashRefreshToken(result.refreshToken()));
	}

	@Test
	@DisplayName("연결 승인은 pushToken 없이도 Device를 만들고 pushToken · pushUpdatedAt을 비워 둔다")
	void approveLinkCreatesDeviceWithoutPushTokenWhenNotGiven() {
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		VehicleLinkResult result = approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), null));

		Device device = lastSavedDeviceOf(NEW_GUARDIAN_ID);
		assertThat(device.getPlatform()).isEqualTo(Platform.ANDROID);
		assertThat(device.getPushToken()).isNull();
		assertThat(device.getPushUpdatedAt()).isNull();
		assertThat(device.getRefreshTokenHash()).isEqualTo(issueTokenPort.hashRefreshToken(result.refreshToken()));
	}

	@Test
	@DisplayName("연결 승인이 성공하면 코드가 USED가 되고 같은 코드로 다시 승인하면 LINK_CODE_EXPIRED로 거절한다")
	void approveLinkMarksCodeUsedAndRejectsSecondApprovalWithSameCode() {
		LinkCode code = issuedCode();
		givenCode(code);
		givenVehicle(pendingVehicle());

		approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN));

		ArgumentCaptor<LinkCode> savedCode = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort).saveLinkCode(savedCode.capture());
		assertThat(savedCode.getValue().getCodeId()).isEqualTo(CODE_ID);
		assertThat(savedCode.getValue().getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(savedCode.getValue().getUsedAt()).isEqualTo(NOW);
		assertThat(savedCode.getValue().getMode()).isEqualTo(LinkMode.NEW);

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.GONE, "LINK_CODE_EXPIRED");
		verify(createGuardianUseCase).createGuardian(any());
	}

	@Test
	@DisplayName("연결 승인 RELINK가 성공하면 코드의 mode가 RELINK로 채워진다")
	void approveLinkRelinkSavesCodeWithRelinkMode() {
		givenRelinkFamily();
		when(loadDevicePort.loadLiveDevices(OwnerType.USER, GUARDIAN_ID)).thenReturn(List.of());

		approveLinkService.approveLink(command(LinkMode.RELINK, requiredConsents(), PUSH_TOKEN));

		ArgumentCaptor<LinkCode> savedCode = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort).saveLinkCode(savedCode.capture());
		assertThat(savedCode.getValue().getStatus()).isEqualTo(LinkCodeStatus.USED);
		assertThat(savedCode.getValue().getMode()).isEqualTo(LinkMode.RELINK);
	}

	@Test
	@DisplayName("연결 승인이 성공하면 그 차량의 연결 코드 발급 횟수를 처음으로 돌린다")
	void approveLinkResetsIssueCounterOfVehicle() {
		for (int i = 0; i < 5; i++) {
			linkCodeIssueCounter.increment(VEHICLE_ID);
		}
		givenCode(issuedCode());
		givenVehicle(pendingVehicle());

		approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN));

		assertThat(linkCodeIssueCounter.isLimitReached(VEHICLE_ID)).isFalse();
		assertThat(linkCodeIssueCounter.remaining(VEHICLE_ID)).isEqualTo(5);
	}

	@Test
	@DisplayName("연결 승인은 없는 코드면 LINK_CODE_INVALID로 거절한다")
	void approveLinkRejectsUnknownCode() {
		when(loadLinkCodePort.loadLinkCodeByCode(CODE)).thenReturn(Optional.empty());

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.NOT_FOUND, "LINK_CODE_INVALID");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 expiresAt이 지난 ISSUED 코드면 LINK_CODE_EXPIRED로 거절한다")
	void approveLinkRejectsIssuedCodePastExpiresAt() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.ISSUED, NOW.minusSeconds(1), null,
				null, null));

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.GONE, "LINK_CODE_EXPIRED");

		assertNothingChanged();
	}

	@Test
	@DisplayName("연결 승인은 status가 EXPIRED인 코드면 LINK_CODE_EXPIRED로 거절한다")
	void approveLinkRejectsExpiredStatusCode() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.EXPIRED, NOW.plusSeconds(180), null,
				null, null));

		assertError(() -> approveLinkService.approveLink(command(LinkMode.NEW, requiredConsents(), PUSH_TOKEN)),
				HttpStatus.GONE, "LINK_CODE_EXPIRED");

		assertNothingChanged();
	}

	private void givenCode(LinkCode code) {
		when(loadLinkCodePort.loadLinkCodeByCode(CODE)).thenReturn(Optional.of(code));
	}

	private void givenVehicle(VehicleInfo vehicle) {
		when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
	}

	private void givenRelinkFamily() {
		givenCode(issuedCode());
		givenVehicle(linkedVehicle());
		when(loadMemberUseCase.findMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "박공동", null),
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, GUARDIAN_NAME, null),
				new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));
	}

	private void assertNothingChanged() {
		verify(createGuardianUseCase, never()).createGuardian(any());
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(saveDevicePort, never()).saveDevice(any());
		verify(issueTokenPort, never()).issueTokens(any());
		verify(publishAuthEventPort, never()).publishDeviceReplaced(anyList());
	}

	private List<Device> savedDevices() {
		ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort, atLeastOnce()).saveDevice(captor.capture());
		return captor.getAllValues();
	}

	private Device lastSavedDeviceOf(long userId) {
		List<Device> userDevices = savedDevices().stream()
				.filter(d -> d.getOwnerType() == OwnerType.USER && d.getOwnerId() == userId)
				.toList();
		assertThat(userDevices).as("USER %d 기기 저장", userId).isNotEmpty();
		return userDevices.getLast();
	}

	private static ApproveLinkCommand command(LinkMode mode, List<ConsentAgreement> consents, String pushToken) {
		return new ApproveLinkCommand(CODE, mode, consents, Platform.ANDROID, pushToken, GUARDIAN_NAME);
	}

	private static List<ConsentAgreement> requiredConsents() {
		return List.of(new ConsentAgreement(ConsentType.TERMS, true), new ConsentAgreement(ConsentType.PRIVACY, true));
	}

	private static LinkCode issuedCode() {
		return LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.ISSUED, NOW.plusSeconds(180), null, null,
				null);
	}

	private static VehicleInfo linkedVehicle() {
		return new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", true);
	}

	private static VehicleInfo pendingVehicle() {
		return new VehicleInfo(VEHICLE_ID, null, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", true);
	}

	private static Device liveUserDevice(long deviceId, long userId) {
		return Device.restore(deviceId, OwnerType.USER, userId, Platform.ANDROID, "old-fcm-" + deviceId,
				NOW.minusSeconds(3600), OLD_REFRESH_HASH, null);
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
