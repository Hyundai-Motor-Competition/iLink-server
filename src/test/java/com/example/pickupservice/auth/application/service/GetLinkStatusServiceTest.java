package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.adapter.out.token.JwtTokenIssuer;
import com.example.pickupservice.auth.application.port.in.LinkStatus;
import com.example.pickupservice.auth.application.port.in.LinkStatusResult;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLinkStatusServiceTest {

	private static final byte[] SECRET_BYTES = "pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final long CODE_ID = 5012L;
	private static final long VEHICLE_ID = 77L;
	private static final long OTHER_VEHICLE_ID = 78L;
	private static final long FAMILY_ID = 12L;
	private static final long VEHICLE_DEVICE_ID = 601L;
	private static final long OTHER_VEHICLE_DEVICE_ID = 602L;
	private static final long OTHER_VEHICLE_OWN_DEVICE_ID = 701L;
	private static final String OLD_VEHICLE_REFRESH_HASH = "0".repeat(64);

	@Mock
	private LoadLinkCodePort loadLinkCodePort;

	@Mock
	private SaveLinkCodePort saveLinkCodePort;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Spy
	private JwtTokenIssuer issueTokenPort = new JwtTokenIssuer(SECRET, ACCESS_TTL, REFRESH_TTL,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Mock
	private LoadVehicleUseCase loadVehicleUseCase;

	@Mock
	private Clock clock;

	@InjectMocks
	private GetLinkStatusService getLinkStatusService;

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(saveLinkCodePort.saveLinkCode(any(LinkCode.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(saveDevicePort.saveDevice(any(Device.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("연결 상태 조회는 다른 차량이 발급한 코드면 NOT_CODE_OWNER로 거절한다")
	void getLinkStatusRejectsCodeIssuedByOtherVehicle() {
		givenCode(usedCode(LinkMode.NEW, null));

		assertNotCodeOwner(() -> getLinkStatusService.getLinkStatus(CODE_ID, OTHER_VEHICLE_ID,
				OTHER_VEHICLE_OWN_DEVICE_ID));

		verify(issueTokenPort, never()).issueTokens(any());
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("연결 상태 조회는 승인 전 코드에 WAITING과 pollAfterSeconds 2를 돌려준다")
	void getLinkStatusReturnsWaitingWithPollAfterTwoSecondsForUnapprovedCode() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID, false, "K7P2QX9M", LinkCodeStatus.ISSUED, NOW.plusSeconds(180),
				null, null, null));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(result).isEqualTo(new LinkStatusResult(LinkStatus.WAITING, null, null, null, null, 2));
		verify(issueTokenPort, never()).issueTokens(any());
	}

	@Test
	@DisplayName("연결 상태 조회는 만료된 코드에 EXPIRED를 돌려준다")
	void getLinkStatusReturnsExpiredForExpiredStatusCode() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID, false, "K7P2QX9M", LinkCodeStatus.EXPIRED, NOW.plusSeconds(180),
				null, null, null));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(result).isEqualTo(new LinkStatusResult(LinkStatus.EXPIRED, null, null, null, null, null));
		verify(issueTokenPort, never()).issueTokens(any());
	}

	@Test
	@DisplayName("연결 상태 조회는 expiresAt이 지난 ISSUED 코드에도 EXPIRED를 돌려준다")
	void getLinkStatusReturnsExpiredForIssuedCodePastExpiresAt() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID, false, "K7P2QX9M", LinkCodeStatus.ISSUED, NOW.minusSeconds(1),
				null, null, null));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(result).isEqualTo(new LinkStatusResult(LinkStatus.EXPIRED, null, null, null, null, null));
		verify(issueTokenPort, never()).issueTokens(any());
	}

	@Test
	@DisplayName("연결 상태 조회는 NEW로 승인된 코드에 LINKED와 vehicleId · familyId · 차량 토큰을 돌려준다")
	void getLinkStatusReturnsLinkedWithVehicleIdFamilyIdAndVehicleTokensForNewApprovedCode() {
		LinkCode code = usedCode(LinkMode.NEW, null);
		givenCode(code);
		givenLinkedVehicle();
		givenIssuedDevice(liveVehicleDevice(VEHICLE_DEVICE_ID));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(result.status()).isEqualTo(LinkStatus.LINKED);
		assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(result.pollAfterSeconds()).isNull();

		Jwt jwt = decode(result.accessToken());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(VEHICLE_ID));
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("VEHICLE");
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(FAMILY_ID);
		assertThat(jwt.<Object>getClaim("role")).isNull();
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(VEHICLE_DEVICE_ID);

		ArgumentCaptor<Device> savedDevice = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort).saveDevice(savedDevice.capture());
		assertThat(savedDevice.getValue().getDeviceId()).isEqualTo(VEHICLE_DEVICE_ID);
		assertThat(savedDevice.getValue().getRefreshTokenHash())
				.isEqualTo(issueTokenPort.hashRefreshToken(result.refreshToken()));
		assertThat(savedDevice.getValue().getRefreshTokenExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));

		ArgumentCaptor<LinkCode> savedCode = ArgumentCaptor.forClass(LinkCode.class);
		verify(saveLinkCodePort).saveLinkCode(savedCode.capture());
		assertThat(savedCode.getValue().getCodeId()).isEqualTo(CODE_ID);
		assertThat(savedCode.getValue().getTokenDeliveredAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("연결 코드는 발급한 deviceId를 기록하고, 연결 상태 조회는 발급한 기기가 아니면 NOT_CODE_OWNER로 거절하며 차량 토큰은 그 기기에만 준다")
	void getLinkStatusRejectsOtherDeviceOfSameVehicleAndIssuesTokensOnlyToIssuingDevice() {
		givenCode(usedCode(LinkMode.NEW, null));

		assertNotCodeOwner(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, OTHER_VEHICLE_DEVICE_ID));

		verify(issueTokenPort, never()).issueTokens(any());
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(saveDevicePort, never()).saveDevice(any());

		givenLinkedVehicle();
		givenIssuedDevice(liveVehicleDevice(VEHICLE_DEVICE_ID));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(result.status()).isEqualTo(LinkStatus.LINKED);
		assertThat(((Number) decode(result.accessToken()).getClaim("deviceId")).longValue())
				.isEqualTo(VEHICLE_DEVICE_ID);
		ArgumentCaptor<Device> savedDevice = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort).saveDevice(savedDevice.capture());
		assertThat(savedDevice.getValue().getDeviceId()).isEqualTo(VEHICLE_DEVICE_ID);
	}

	@Test
	@DisplayName("연결 상태 조회는 같은 차량의 다른 기기가 승인 전 코드를 조회해도 NOT_CODE_OWNER로 거절한다")
	void getLinkStatusRejectsOtherDeviceOfSameVehicleForWaitingCode() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID, false, "K7P2QX9M", LinkCodeStatus.ISSUED,
				NOW.plusSeconds(180), null, null, null));

		assertNotCodeOwner(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, OTHER_VEHICLE_DEVICE_ID));
	}

	@Test
	@DisplayName("연결 상태 조회는 살아 있는 차량 기기가 여럿이어도 deviceId 크기와 상관없이 코드를 발급한 기기에 차량 토큰을 준다")
	void getLinkStatusIssuesVehicleTokensToIssuingDeviceEvenWhenLargerLiveDeviceExists() {
		givenCode(usedCode(LinkMode.NEW, null));
		givenLinkedVehicle();
		givenIssuedDevice(liveVehicleDevice(VEHICLE_DEVICE_ID));
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.VEHICLE, VEHICLE_ID)).thenReturn(List.of(
				liveVehicleDevice(600L), liveVehicleDevice(OTHER_VEHICLE_DEVICE_ID),
				liveVehicleDevice(VEHICLE_DEVICE_ID)));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(((Number) decode(result.accessToken()).getClaim("deviceId")).longValue())
				.isEqualTo(VEHICLE_DEVICE_ID);
		ArgumentCaptor<Device> savedDevice = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort).saveDevice(savedDevice.capture());
		assertThat(savedDevice.getAllValues()).extracting(Device::getDeviceId).containsExactly(VEHICLE_DEVICE_ID);
	}

	@Test
	@DisplayName("연결 상태 조회는 RELINK로 승인된 코드에 RELINKED를 돌려준다")
	void getLinkStatusReturnsRelinkedForRelinkApprovedCode() {
		givenCode(usedCode(LinkMode.RELINK, null));
		givenLinkedVehicle();
		givenIssuedDevice(liveVehicleDevice(VEHICLE_DEVICE_ID));

		LinkStatusResult result = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(result.status()).isEqualTo(LinkStatus.RELINKED);
		assertThat(result.vehicleId()).isEqualTo(VEHICLE_ID);
		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(result.pollAfterSeconds()).isNull();
	}

	@Test
	@DisplayName("차량 토큰을 내려준 코드를 다시 조회하면 LINK_CODE_INVALID로 거절한다")
	void getLinkStatusRejectsCodeWhoseTokensWereAlreadyDelivered() {
		givenCode(usedCode(LinkMode.NEW, NOW.minusSeconds(2)));

		assertLinkCodeInvalid(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID));

		verify(issueTokenPort, never()).issueTokens(any());
		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("차량 토큰을 내려준 바로 다음 조회는 LINK_CODE_INVALID로 거절한다")
	void getLinkStatusRejectsSecondCallAfterDeliveringTokens() {
		givenCode(usedCode(LinkMode.NEW, null));
		givenLinkedVehicle();
		givenIssuedDevice(liveVehicleDevice(VEHICLE_DEVICE_ID));

		LinkStatusResult first = getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID);

		assertThat(first.status()).isEqualTo(LinkStatus.LINKED);
		assertLinkCodeInvalid(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID));
	}

	@Test
	@DisplayName("연결 상태 조회는 없는 코드면 LINK_CODE_INVALID로 거절한다")
	void getLinkStatusRejectsUnknownCode() {
		when(loadLinkCodePort.loadLinkCode(CODE_ID)).thenReturn(Optional.empty());

		assertLinkCodeInvalid(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID));
	}

	@Test
	@DisplayName("연결 상태 조회는 승인된 코드인데 발급한 기기가 없으면 LINK_CODE_INVALID로 거절한다")
	void getLinkStatusRejectsApprovedCodeWhenIssuingDeviceIsMissing() {
		givenCode(usedCode(LinkMode.NEW, null));
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(linkedVehicle()));
		lenient().when(loadDevicePort.loadDevice(VEHICLE_DEVICE_ID)).thenReturn(Optional.empty());
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.VEHICLE, VEHICLE_ID))
				.thenReturn(List.of(liveVehicleDevice(OTHER_VEHICLE_DEVICE_ID)));

		assertLinkCodeInvalid(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID));

		verify(issueTokenPort, never()).issueTokens(any());
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(saveDevicePort, never()).saveDevice(any());
	}

	@Test
	@DisplayName("연결 상태 조회는 승인된 코드인데 발급한 기기가 폐기됐으면 LINK_CODE_INVALID로 거절한다")
	void getLinkStatusRejectsApprovedCodeWhenIssuingDeviceIsRevoked() {
		givenCode(usedCode(LinkMode.NEW, null));
		lenient().when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(linkedVehicle()));
		lenient().when(loadDevicePort.loadDevice(VEHICLE_DEVICE_ID)).thenReturn(Optional.of(
				Device.restore(VEHICLE_DEVICE_ID, OwnerType.VEHICLE, VEHICLE_ID, Platform.VEHICLE, null, null,
						OLD_VEHICLE_REFRESH_HASH, NOW.minusSeconds(1), NOW.plus(Duration.ofDays(14)))));
		lenient().when(loadDevicePort.loadLiveDevices(OwnerType.VEHICLE, VEHICLE_ID))
				.thenReturn(List.of(liveVehicleDevice(OTHER_VEHICLE_DEVICE_ID)));

		assertLinkCodeInvalid(() -> getLinkStatusService.getLinkStatus(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID));

		verify(issueTokenPort, never()).issueTokens(any());
		verify(saveLinkCodePort, never()).saveLinkCode(any());
		verify(saveDevicePort, never()).saveDevice(any());
	}

	private void givenCode(LinkCode code) {
		when(loadLinkCodePort.loadLinkCode(CODE_ID)).thenReturn(Optional.of(code));
	}

	private void givenLinkedVehicle() {
		when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(linkedVehicle()));
	}

	private void givenIssuedDevice(Device device) {
		when(loadDevicePort.loadDevice(device.getDeviceId())).thenReturn(Optional.of(device));
	}

	private static VehicleInfo linkedVehicle() {
		return new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", true);
	}

	private static LinkCode usedCode(LinkMode mode, Instant tokenDeliveredAt) {
		return LinkCode.restore(CODE_ID, VEHICLE_ID, VEHICLE_DEVICE_ID, false, "K7P2QX9M", LinkCodeStatus.USED, NOW.plusSeconds(180),
				NOW.minusSeconds(5), mode, tokenDeliveredAt);
	}

	private static Device liveVehicleDevice(long deviceId) {
		return Device.restore(deviceId, OwnerType.VEHICLE, VEHICLE_ID, Platform.VEHICLE, null, null,
				OLD_VEHICLE_REFRESH_HASH, null, NOW.plus(Duration.ofDays(14)));
	}

	private static void assertNotCodeOwner(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN);
			assertThat(e.code()).isEqualTo("NOT_CODE_OWNER");
		});
	}

	private static void assertLinkCodeInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(e.code()).isEqualTo("LINK_CODE_INVALID");
		});
	}

	private static Jwt decode(String token) {
		return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET_BYTES, "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build()
				.decode(token);
	}

}
