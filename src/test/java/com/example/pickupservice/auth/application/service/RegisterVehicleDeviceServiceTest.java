package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.adapter.out.token.JwtTokenIssuer;
import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceCommand;
import com.example.pickupservice.auth.application.port.in.VehicleDeviceResult;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.RegisterVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterVehicleDeviceServiceTest {

	private static final byte[] SECRET_BYTES = "pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final String VIN = "KMHJ381ABNU123456";
	private static final String MODEL = "아이오닉 5";
	private static final long SAVED_DEVICE_ID = 701L;

	@Mock
	private LoadVehicleUseCase loadVehicleUseCase;

	@Mock
	private RegisterVehicleUseCase registerVehicleUseCase;

	@Mock
	private SaveDevicePort saveDevicePort;

	@Spy
	private JwtTokenIssuer issueTokenPort = new JwtTokenIssuer(SECRET, ACCESS_TTL, REFRESH_TTL,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Mock
	private Clock clock;

	@InjectMocks
	private RegisterVehicleDeviceService registerVehicleDeviceService;

	private final List<Device> savedDevices = new ArrayList<>();
	private final List<IssuedTokens> issuedTokens = new ArrayList<>();

	@BeforeEach
	void setUpClock() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("차량 기기 등록은 연결된 VIN이면 409 VEHICLE_ALREADY_LINKED 「이미 보호자가 연결된 차량입니다」로 거절한다")
	void registerVehicleDeviceRejectsVinLinkedToFamily() {
		when(loadVehicleUseCase.findLinkedVehicleByVin(VIN)).thenReturn(Optional.of(
				new VehicleInfo(77L, 12L, VIN, MODEL, "12가3456", true)));

		assertThatThrownBy(() -> registerVehicleDeviceService.registerVehicleDevice(command()))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(e.code()).isEqualTo("VEHICLE_ALREADY_LINKED");
					assertThat(e.getMessage()).isEqualTo("이미 보호자가 연결된 차량입니다");
				});

		verify(registerVehicleUseCase, never()).registerVehicle(anyString(), anyString());
		verify(saveDevicePort, never()).saveDevice(any());
		verify(issueTokenPort, never()).issueTokens(any());
	}

	@Test
	@DisplayName("차량 기기 등록은 같은 VIN의 연결 대기 행이 있으면 새 Vehicle을 만들지 않고 그 vehicleId를 쓴다")
	void registerVehicleDeviceReusesPendingVehicleRowOfSameVin() {
		when(loadVehicleUseCase.findLinkedVehicleByVin(VIN)).thenReturn(Optional.of(
				new VehicleInfo(88L, null, VIN, MODEL, null, false)));
		givenDeviceStore();

		VehicleDeviceResult result = registerVehicleDeviceService.registerVehicleDevice(command());

		verify(registerVehicleUseCase, never()).registerVehicle(anyString(), anyString());
		assertThat(result.vehicleId()).isEqualTo(88L);
		assertThat(lastSavedDevice().getOwnerId()).isEqualTo(88L);
	}

	@Test
	@DisplayName("차량 기기 등록은 같은 VIN 행이 없으면 registerVehicle로 새 Vehicle을 만들고 그 vehicleId를 쓴다")
	void registerVehicleDeviceCreatesVehicleWhenNoRowOfSameVin() {
		when(loadVehicleUseCase.findLinkedVehicleByVin(VIN)).thenReturn(Optional.empty());
		when(registerVehicleUseCase.registerVehicle(VIN, MODEL)).thenReturn(99L);
		givenDeviceStore();

		VehicleDeviceResult result = registerVehicleDeviceService.registerVehicleDevice(command());

		verify(registerVehicleUseCase).registerVehicle(VIN, MODEL);
		assertThat(result.vehicleId()).isEqualTo(99L);
		assertThat(lastSavedDevice().getOwnerId()).isEqualTo(99L);
	}

	@Test
	@DisplayName("차량 기기 등록은 ownerType VEHICLE · platform VEHICLE인 Device를 만든다")
	void registerVehicleDeviceCreatesVehicleOwnedVehiclePlatformDevice() {
		when(loadVehicleUseCase.findLinkedVehicleByVin(VIN)).thenReturn(Optional.empty());
		when(registerVehicleUseCase.registerVehicle(VIN, MODEL)).thenReturn(99L);
		givenDeviceStore();
		recordIssuedTokens();

		VehicleDeviceResult result = registerVehicleDeviceService.registerVehicleDevice(command());

		Device device = lastSavedDevice();
		IssuedTokens tokens = lastIssuedTokens();
		assertThat(device.getOwnerType()).isEqualTo(OwnerType.VEHICLE);
		assertThat(device.getPlatform()).isEqualTo(Platform.VEHICLE);
		assertThat(device.getOwnerId()).isEqualTo(99L);
		assertThat(device.getPushToken()).isNull();
		assertThat(device.isLive()).isTrue();
		assertThat(device.getRefreshTokenHash())
				.isEqualTo(sha256Hex(tokens.refreshToken()))
				.isNotEqualTo(tokens.refreshToken());
		assertThat(result.deviceId()).isEqualTo(SAVED_DEVICE_ID);
	}

	@Test
	@DisplayName("차량 기기 등록이 만든 Device의 refresh 토큰 만료 시각은 발급자가 준 refreshTokenExpiresAt(발급 시각 + 14일)이다")
	void registerVehicleDeviceStoresRefreshTokenExpiryFromIssuedTokens() {
		when(loadVehicleUseCase.findLinkedVehicleByVin(VIN)).thenReturn(Optional.empty());
		when(registerVehicleUseCase.registerVehicle(VIN, MODEL)).thenReturn(99L);
		givenDeviceStore();
		recordIssuedTokens();

		registerVehicleDeviceService.registerVehicleDevice(command());

		assertThat(lastSavedDevice().getRefreshTokenExpiresAt())
				.isEqualTo(lastIssuedTokens().refreshTokenExpiresAt())
				.isEqualTo(NOW.plus(REFRESH_TTL));
	}

	@Test
	@DisplayName("차량 기기 등록이 준 deviceToken은 familyId가 빈 연결 전 VEHICLE 토큰이고 저장된 deviceId가 들어 있다")
	void registerVehicleDeviceReturnsUnlinkedVehicleAccessTokenWithSavedDeviceId() {
		when(loadVehicleUseCase.findLinkedVehicleByVin(VIN)).thenReturn(Optional.of(
				new VehicleInfo(88L, null, VIN, MODEL, null, false)));
		givenDeviceStore();
		recordIssuedTokens();

		VehicleDeviceResult result = registerVehicleDeviceService.registerVehicleDevice(command());

		assertThat(result.deviceToken()).isEqualTo(lastIssuedTokens().accessToken());
		Jwt jwt = decode(result.deviceToken());
		assertThat(jwt.getSubject()).isEqualTo("88");
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("VEHICLE");
		assertThat(jwt.<Object>getClaim("familyId")).isNull();
		assertThat(jwt.<Object>getClaim("role")).isNull();
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(SAVED_DEVICE_ID);
	}

	private static RegisterVehicleDeviceCommand command() {
		return new RegisterVehicleDeviceCommand(VIN, MODEL);
	}

	private void givenDeviceStore() {
		when(saveDevicePort.saveDevice(any(Device.class))).thenAnswer(invocation -> {
			Device device = invocation.getArgument(0);
			Device saved = device.getDeviceId() != null ? device
					: Device.restore(SAVED_DEVICE_ID, device.getOwnerType(), device.getOwnerId(),
							device.getPlatform(), device.getPushToken(), device.getPushUpdatedAt(),
							device.getRefreshTokenHash(), device.getRevokedAt(), device.getRefreshTokenExpiresAt());
			savedDevices.add(saved);
			return saved;
		});
	}

	private void recordIssuedTokens() {
		doAnswer(invocation -> {
			IssuedTokens tokens = (IssuedTokens) invocation.callRealMethod();
			issuedTokens.add(tokens);
			return tokens;
		}).when(issueTokenPort).issueTokens(any(TokenSubject.class));
	}

	private Device lastSavedDevice() {
		assertThat(savedDevices).isNotEmpty();
		return savedDevices.getLast();
	}

	private IssuedTokens lastIssuedTokens() {
		assertThat(issuedTokens).hasSize(1);
		return issuedTokens.getLast();
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
