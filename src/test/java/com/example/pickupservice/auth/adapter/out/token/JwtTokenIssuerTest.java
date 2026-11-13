package com.example.pickupservice.auth.adapter.out.token;

import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.common.fixture.MainApplicationYaml;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.AuthPrincipalJwtConverter;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
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

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenIssuerTest {

	private static final byte[] SECRET_BYTES = "pickup-test-jwt-secret-0123456789abcdef".getBytes(StandardCharsets.UTF_8);
	private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);
	private static final String ACCESS_TTL_PROPERTY = "pickup.jwt.access-ttl";
	private static final String REFRESH_TTL_PROPERTY = "pickup.jwt.refresh-ttl";
	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	private final JwtTokenIssuer issuer = new JwtTokenIssuer(SECRET, ACCESS_TTL, REFRESH_TTL,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	@DisplayName("토큰 발급은 refresh 원문을 돌려주고 Device에는 그 SHA-256 해시만 저장한다")
	void issueTokensReturnsRawRefreshTokenWithItsSha256Hash() {
		IssuedTokens tokens = issuer.issueTokens(guardianSubject());

		assertThat(tokens.refreshToken()).isNotBlank();
		assertThat(tokens.refreshTokenHash())
				.isEqualTo(sha256Hex(tokens.refreshToken()))
				.matches("[0-9a-f]{64}")
				.isNotEqualTo(tokens.refreshToken());
		assertThat(issuer.hashRefreshToken(tokens.refreshToken())).isEqualTo(tokens.refreshTokenHash());
	}

	@Test
	@DisplayName("refresh 토큰은 발급할 때마다 다른 불투명 문자열이고 access 토큰과도 다르다")
	void issueTokensCreatesDifferentOpaqueRefreshTokenEachTime() {
		IssuedTokens first = issuer.issueTokens(guardianSubject());
		IssuedTokens second = issuer.issueTokens(guardianSubject());

		assertThat(first.refreshToken()).isNotEqualTo(second.refreshToken());
		assertThat(first.refreshTokenHash()).isNotEqualTo(second.refreshTokenHash());
		assertThat(first.refreshToken()).isNotEqualTo(first.accessToken());
		assertThat(first.refreshToken().split("\\.")).hasSize(1);
	}

	@Test
	@DisplayName("발급한 access 토큰에는 주체 · familyId · role이 들어 있다")
	void accessTokenContainsSubjectFamilyIdAndRole() {
		IssuedTokens tokens = issuer.issueTokens(guardianSubject());

		Jwt jwt = decode(tokens.accessToken());

		assertThat(jwt.getSubject()).isEqualTo("31");
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("USER");
		assertThat(jwt.<Object>getClaim("familyId")).isInstanceOf(Number.class);
		assertThat(((Number) jwt.getClaim("familyId")).longValue()).isEqualTo(12L);
		assertThat(jwt.getClaimAsString("role")).isEqualTo("GUARDIAN");
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(501L);
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));
		assertThat(tokens.accessTokenExpiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));

		AuthPrincipal principal = new AuthPrincipalJwtConverter().convert(jwt).getPrincipal();
		assertThat(principal).isEqualTo(new AuthPrincipal(PrincipalType.USER, 31L, 12L, Role.GUARDIAN));
	}

	@Test
	@DisplayName("연결 전 차량 기기의 access 토큰은 principalType VEHICLE에 familyId · role 클레임이 비어 있다")
	void unlinkedVehicleAccessTokenHasNoFamilyIdAndRole() {
		IssuedTokens tokens = issuer.issueTokens(new TokenSubject(PrincipalType.VEHICLE, 77L, null, null, 601L));

		Jwt jwt = decode(tokens.accessToken());

		assertThat(jwt.getSubject()).isEqualTo("77");
		assertThat(jwt.getClaimAsString("principalType")).isEqualTo("VEHICLE");
		assertThat(jwt.<Object>getClaim("familyId")).isNull();
		assertThat(jwt.<Object>getClaim("role")).isNull();
		assertThat(((Number) jwt.getClaim("deviceId")).longValue()).isEqualTo(601L);

		AuthPrincipal principal = new AuthPrincipalJwtConverter().convert(jwt).getPrincipal();
		assertThat(principal).isEqualTo(new AuthPrincipal(PrincipalType.VEHICLE, 77L, null, null));
	}

	@Test
	@DisplayName("고정 시각에 발급하면 access 토큰 만료는 발급 시각 + 30분(exp claim · accessTokenExpiresAt)이고 refresh 토큰 만료는 발급 시각 + 14일이다")
	void issueTokensSetsAccessExpiryToThirtyMinutesAndRefreshExpiryToFourteenDaysAfterIssue() {
		Instant issuedAt = Instant.parse("2026-11-14T03:00:00Z");
		JwtTokenIssuer fixedIssuer = new JwtTokenIssuer(SECRET, Duration.parse("PT30M"), Duration.parse("P14D"),
				Clock.fixed(issuedAt, ZoneOffset.UTC));

		IssuedTokens tokens = fixedIssuer.issueTokens(guardianSubject());

		Jwt jwt = decode(tokens.accessToken());
		assertThat(jwt.getIssuedAt()).isEqualTo(issuedAt);
		assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-11-14T03:30:00Z"));
		assertThat(tokens.accessTokenExpiresAt()).isEqualTo(Instant.parse("2026-11-14T03:30:00Z"));
		assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(Instant.parse("2026-11-28T03:00:00Z"));
	}

	@Test
	@DisplayName("refresh 토큰 만료 시각은 생성자로 받은 refresh 유효 기간을 따른다")
	void refreshTokenExpiryFollowsConfiguredRefreshTtl() {
		JwtTokenIssuer shortIssuer = new JwtTokenIssuer(SECRET, Duration.ofMinutes(5), Duration.ofDays(1),
				Clock.fixed(NOW, ZoneOffset.UTC));

		IssuedTokens tokens = shortIssuer.issueTokens(guardianSubject());

		assertThat(tokens.accessTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
		assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
	}

	@Test
	@DisplayName("application.yml의 pickup.jwt.access-ttl은 PT30M, pickup.jwt.refresh-ttl은 P14D이고 dev · prod 프로필도 그대로다")
	void jwtTtlsAreConfiguredInMainApplicationYaml() {
		assertThat(MainApplicationYaml.baseDuration(ACCESS_TTL_PROPERTY)).isEqualTo(Duration.ofMinutes(30));
		assertThat(MainApplicationYaml.baseDuration(REFRESH_TTL_PROPERTY)).isEqualTo(Duration.ofDays(14));
		for (String profile : new String[] {"dev", "prod"}) {
			assertThat(MainApplicationYaml.profileOverBaseDuration(profile, ACCESS_TTL_PROPERTY))
					.as(profile + " access-ttl").isEqualTo(Duration.ofMinutes(30));
			assertThat(MainApplicationYaml.profileOverBaseDuration(profile, REFRESH_TTL_PROPERTY))
					.as(profile + " refresh-ttl").isEqualTo(Duration.ofDays(14));
		}
	}

	@Test
	@DisplayName("JwtTokenIssuer의 생성자는 하나이고 access · refresh 유효 기간을 @Value로 pickup.jwt.access-ttl · pickup.jwt.refresh-ttl 설정에서 받는다")
	void constructorBindsAccessAndRefreshTtlFromConfiguration() {
		Constructor<?>[] constructors = JwtTokenIssuer.class.getConstructors();

		assertThat(constructors).hasSize(1);
		Parameter[] parameters = constructors[0].getParameters();
		assertThat(parameters).extracting(Parameter::getType)
				.containsExactly(String.class, Duration.class, Duration.class, Clock.class);
		assertThat(valueOf(parameters[0])).isEqualTo("${pickup.jwt.secret}");
		assertThat(valueOf(parameters[1])).isEqualTo("${" + ACCESS_TTL_PROPERTY + "}");
		assertThat(valueOf(parameters[2])).isEqualTo("${" + REFRESH_TTL_PROPERTY + "}");
	}

	private static String valueOf(Parameter parameter) {
		Value value = parameter.getAnnotation(Value.class);
		assertThat(value).as(parameter + "의 @Value").isNotNull();
		return value.value();
	}

	private static TokenSubject guardianSubject() {
		return new TokenSubject(PrincipalType.USER, 31L, 12L, Role.GUARDIAN, 501L);
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
