package com.example.pickupservice.common.fixture;

import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@TestComponent
@RequiredArgsConstructor
public class AccessTokenFixture {

	private static final String SECRET_PROPERTY = "pickup.jwt.secret";
	private static final int OTHER_KEY_BYTES = 32;
	private static final Duration VALIDITY = Duration.ofMinutes(30);
	private static final Duration EXPIRED_AGO = Duration.ofMinutes(10);

	private final Environment environment;

	public static String bearer(String token) {
		return "Bearer " + token;
	}

	public String vehicle(long vehicleId, long familyId) {
		return sign(principalClaims(PrincipalType.VEHICLE, vehicleId, familyId));
	}

	public String user(long userId, long familyId, Role role) {
		return sign(userClaims(userId, familyId, role));
	}

	public String expiredUser(long userId, long familyId, Role role) {
		return sign(userClaims(userId, familyId, role), Instant.now().minus(EXPIRED_AGO), applicationKey());
	}

	public String userSignedWithOtherKey(long userId, long familyId, Role role) {
		return sign(userClaims(userId, familyId, role), unexpiredExpiresAt(), randomOtherKey());
	}

	public String signedWithClaims(Map<String, Object> claims) {
		return sign(JwtClaimsSet.builder().claims(allClaims -> allClaims.putAll(claims)));
	}

	public String signedWithClaimsWithoutExpiry(Map<String, Object> claims) {
		JwtClaimsSet claimsSet = JwtClaimsSet.builder()
				.claims(allClaims -> allClaims.putAll(claims))
				.issuedAt(Instant.now())
				.build();
		return encode(claimsSet, applicationKey());
	}

	private static JwtClaimsSet.Builder userClaims(long userId, long familyId, Role role) {
		return principalClaims(PrincipalType.USER, userId, familyId).claim("role", role.name());
	}

	private static JwtClaimsSet.Builder principalClaims(PrincipalType type, long id, long familyId) {
		return JwtClaimsSet.builder()
				.subject(String.valueOf(id))
				.claim("principalType", type.name())
				.claim("familyId", familyId);
	}

	private String sign(JwtClaimsSet.Builder claims) {
		return sign(claims, unexpiredExpiresAt(), applicationKey());
	}

	private static String sign(JwtClaimsSet.Builder claims, Instant expiresAt, SecretKey key) {
		JwtClaimsSet claimsSet = claims.issuedAt(expiresAt.minus(VALIDITY)).expiresAt(expiresAt).build();
		return encode(claimsSet, key);
	}

	private static String encode(JwtClaimsSet claimsSet, SecretKey key) {
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder(key).encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
	}

	private static Instant unexpiredExpiresAt() {
		return Instant.now().plus(VALIDITY);
	}

	private static JwtEncoder encoder(SecretKey key) {
		return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
	}

	private SecretKey applicationKey() {
		return hmacKey(Base64.getDecoder().decode(environment.getRequiredProperty(SECRET_PROPERTY)));
	}

	private static SecretKey randomOtherKey() {
		byte[] secret = new byte[OTHER_KEY_BYTES];
		new SecureRandom().nextBytes(secret);
		return hmacKey(secret);
	}

	private static SecretKey hmacKey(byte[] secret) {
		return new SecretKeySpec(secret, "HmacSHA256");
	}

}
