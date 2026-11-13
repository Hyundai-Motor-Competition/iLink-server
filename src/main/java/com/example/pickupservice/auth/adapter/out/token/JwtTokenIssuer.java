package com.example.pickupservice.auth.adapter.out.token;

import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class JwtTokenIssuer implements IssueTokenPort {

	private static final String REFRESH_TOKEN_PREFIX = "rt_";
	private static final int REFRESH_TOKEN_BYTES = 32;

	private final JwtEncoder jwtEncoder;
	private final Duration accessTtl;
	private final Duration refreshTtl;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	public JwtTokenIssuer(@Value("${pickup.jwt.secret}") String base64Secret,
			@Value("${pickup.jwt.access-ttl}") Duration accessTtl,
			@Value("${pickup.jwt.refresh-ttl}") Duration refreshTtl, Clock clock) {
		SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256");
		this.jwtEncoder = NimbusJwtEncoder.withSecretKey(key).build();
		this.accessTtl = accessTtl;
		this.refreshTtl = refreshTtl;
		this.clock = clock;
	}

	@Override
	public IssuedTokens issueTokens(TokenSubject subject) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(accessTtl);
		String accessToken = encodeAccessToken(subject, issuedAt, expiresAt);
		String refreshToken = newRefreshToken();
		return new IssuedTokens(accessToken, expiresAt, refreshToken, hashRefreshToken(refreshToken),
				issuedAt.plus(refreshTtl));
	}

	@Override
	public String hashRefreshToken(String refreshToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private String encodeAccessToken(TokenSubject subject, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.subject(String.valueOf(subject.id()))
				.claim("principalType", subject.type().name())
				.claim("deviceId", subject.deviceId())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt);
		if (subject.familyId() != null) {
			claims.claim("familyId", subject.familyId());
		}
		if (subject.role() != null) {
			claims.claim("role", subject.role().name());
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

	private String newRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return REFRESH_TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
