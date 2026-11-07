package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.adapter.out.token.JwtTokenIssuer;
import com.example.pickupservice.auth.application.port.in.TokenResult;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final byte[] SECRET_BYTES = "pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final String OLD_REFRESH_TOKEN = "rt_old-refresh-token-of-device-501";
	private static final long GUARDIAN_ID = 31L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;

	@Mock
	private LoadDevicePort loadDevicePort;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Spy
	private JwtTokenIssuer issueTokenPort = new JwtTokenIssuer(SECRET, ACCESS_TTL, Clock.fixed(NOW, ZoneOffset.UTC));

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadVehicleUseCase loadVehicleUseCase;

	@InjectMocks
	private RefreshTokenService refreshTokenService;

	@Test
	@DisplayName("토큰 발급은 refresh 원문을 돌려주고 Device에는 그 SHA-256 해시만 저장한다")
	void refreshReturnsRawRefreshTokenAndSavesOnlyItsSha256HashToDevice() {
		Device device = liveGuardianDevice();
		givenStoredDevices(device);
		givenGuardianMember();

		TokenResult result = refreshTokenService.refresh(OLD_REFRESH_TOKEN);

		ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
		verify(saveDevicePort).saveDevice(saved.capture());
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(saved.getValue().getDeviceId()).isEqualTo(501L);
		assertThat(saved.getValue().getRefreshTokenHash())
				.isEqualTo(sha256Hex(result.refreshToken()))
				.isNotEqualTo(result.refreshToken());
	}

	@Test
	@DisplayName("토큰 갱신이 발급한 access 토큰에는 USER 기기 주인의 familyId · role과 deviceId가 들어 있다")
	void refreshOfUserDeviceIssuesAccessTokenWithMemberFamilyIdRoleAndDeviceId() {
		givenStoredDevices(liveGuardianDevice());
		givenGuardianMember();

		TokenResult result = refreshTokenService.refresh(OLD_REFRESH_TOKEN);

		Jwt jwt = decode(result.accessToken());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(GUARDIAN_ID));
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("USER");
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(FAMILY_ID);
		assertThat(jwt.getClaimAsString("role")).isEqualTo("GUARDIAN");
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(501L);
		assertThat(result.accessTokenExpiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));
	}

	@Test
	@DisplayName("토큰 갱신이 발급한 access 토큰에는 VEHICLE 기기 차량의 familyId가 들어 있고 role은 비어 있다")
	void refreshOfVehicleDeviceIssuesAccessTokenWithVehicleFamilyIdAndNoRole() {
		Device device = Device.restore(601L, OwnerType.VEHICLE, VEHICLE_ID, Platform.VEHICLE, null, null,
				issueTokenPort.hashRefreshToken(OLD_REFRESH_TOKEN), null);
		givenStoredDevices(device);
		when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(
				new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", "아이오닉 5", "12가3456", true)));

		TokenResult result = refreshTokenService.refresh(OLD_REFRESH_TOKEN);

		Jwt jwt = decode(result.accessToken());
		assertThat(jwt.getSubject()).isEqualTo(String.valueOf(VEHICLE_ID));
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("VEHICLE");
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(FAMILY_ID);
		assertThat(jwt.<Object>getClaim("role")).isNull();
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(601L);
	}

	@Test
	@DisplayName("토큰 갱신은 새 refresh 토큰을 주고 이전 refresh 토큰으로 다시 갱신하면 REFRESH_TOKEN_INVALID로 거절한다")
	void refreshRotatesTokenAndRejectsPreviousRefreshToken() {
		givenStoredDevices(liveGuardianDevice());
		givenGuardianMember();

		TokenResult first = refreshTokenService.refresh(OLD_REFRESH_TOKEN);

		assertThat(first.refreshToken()).isNotBlank().isNotEqualTo(OLD_REFRESH_TOKEN);
		assertThat(first.accessToken()).isNotBlank();
		assertRefreshTokenInvalid(OLD_REFRESH_TOKEN);

		TokenResult second = refreshTokenService.refresh(first.refreshToken());
		assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
	}

	@Test
	@DisplayName("폐기된 기기의 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다")
	void refreshWithRevokedDeviceTokenIsRejected() {
		Device revoked = Device.restore(501L, OwnerType.USER, GUARDIAN_ID, Platform.ANDROID, "fcm-token-1",
				NOW.minusSeconds(3600), issueTokenPort.hashRefreshToken(OLD_REFRESH_TOKEN), NOW.minusSeconds(60));
		givenStoredDevices(revoked);

		assertRefreshTokenInvalid(OLD_REFRESH_TOKEN);

		verify(saveDevicePort, never()).saveDevice(any());
		assertThat(revoked.getRefreshTokenHash()).isEqualTo(issueTokenPort.hashRefreshToken(OLD_REFRESH_TOKEN));
	}

	@Test
	@DisplayName("모르는 refresh 토큰으로 갱신하면 REFRESH_TOKEN_INVALID로 거절한다")
	void refreshWithUnknownTokenIsRejected() {
		givenStoredDevices();

		assertRefreshTokenInvalid("rt_unknown-token");

		verify(saveDevicePort, never()).saveDevice(any());
	}

	private Device liveGuardianDevice() {
		return Device.restore(501L, OwnerType.USER, GUARDIAN_ID, Platform.ANDROID, "fcm-token-1",
				NOW.minusSeconds(3600), issueTokenPort.hashRefreshToken(OLD_REFRESH_TOKEN), null);
	}

	private void givenStoredDevices(Device... devices) {
		when(loadDevicePort.loadByRefreshTokenHash(anyString())).thenAnswer(invocation -> {
			String hash = invocation.getArgument(0);
			for (Device device : devices) {
				if (hash.equals(device.getRefreshTokenHash())) {
					return Optional.of(device);
				}
			}
			return Optional.empty();
		});
		if (devices.length > 0 && devices[0].isLive()) {
			when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
		}
	}

	private void givenGuardianMember() {
		when(loadMemberUseCase.findMember(GUARDIAN_ID)).thenReturn(Optional.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "01012345678")));
	}

	private void assertRefreshTokenInvalid(String refreshToken) {
		assertThatThrownBy(() -> refreshTokenService.refresh(refreshToken))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(e.code()).isEqualTo("REFRESH_TOKEN_INVALID");
				});
	}

	private static Jwt decode(String token) {
		return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET_BYTES, "HmacSHA256"))
				.macAlgorithm(MacAlgorithm.HS256)
				.build()
				.decode(token);
	}

	private static String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
