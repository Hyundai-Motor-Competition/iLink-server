package com.example.pickupservice.common.fixture;

import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class AccessTokenFixtureTest extends IntegrationTestSupport {

	private static final long VEHICLE_ID = 77L;
	private static final long USER_ID = 101L;
	private static final long FAMILY_ID = 12L;

	private final AccessTokenFixture accessTokenFixture;
	private final JwtDecoder jwtDecoder;

	@Test
	@DisplayName("공통 테스트 픽스처로 만든 VEHICLE access 토큰(vehicleId 77 · familyId 12)을 애플리케이션 JwtDecoder로 해석하면 sub 77 · principalType VEHICLE · familyId 12가 나오고 role은 없다")
	void vehicleTokenDecodedByApplicationJwtDecoderCarriesVehiclePrincipalClaims() {
		String token = accessTokenFixture.vehicle(VEHICLE_ID, FAMILY_ID);

		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo("77");
		assertThat(jwt.getClaims())
				.containsEntry("principalType", "VEHICLE")
				.containsEntry("familyId", 12L)
				.doesNotContainKey("role");
		assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
	}

	@ParameterizedTest(name = "role {0}")
	@ValueSource(strings = {"GUARDIAN", "CO_GUARDIAN", "DELEGATE", "TEACHER", "CHILD"})
	@DisplayName("공통 테스트 픽스처로 만든 USER access 토큰(userId 101 · familyId 12 · role 지정)을 애플리케이션 JwtDecoder로 해석하면 sub 101 · principalType USER · familyId 12 · 지정한 role이 나온다")
	void userTokenDecodedByApplicationJwtDecoderCarriesUserPrincipalClaims(String roleName) {
		String token = accessTokenFixture.user(USER_ID, FAMILY_ID, Role.valueOf(roleName));

		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo("101");
		assertThat(jwt.getClaims())
				.containsEntry("principalType", "USER")
				.containsEntry("familyId", 12L)
				.containsEntry("role", roleName);
		assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
	}

	@Test
	@DisplayName("공통 테스트 픽스처 signedWithClaims로 임의 클레임(sub 101 · principalType USER · familyId 12 · role GUARDIAN · extra x)을 담아 만든 토큰을 애플리케이션 JwtDecoder로 해석하면 서명 검증을 통과해 넘긴 클레임이 그대로 나오고 만료 전이다")
	void tokenSignedWithArbitraryClaimsPassesApplicationJwtDecoderWithSameClaims() {
		Map<String, Object> claims = Map.of(
				"sub", "101",
				"principalType", "USER",
				"familyId", 12L,
				"role", "GUARDIAN",
				"extra", "x");

		Jwt jwt = jwtDecoder.decode(accessTokenFixture.signedWithClaims(claims));

		assertThat(jwt.getClaims()).containsAllEntriesOf(claims);
		assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
	}

}
