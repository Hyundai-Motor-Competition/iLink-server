package com.example.pickupservice.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component
public class AuthPrincipalJwtConverter implements Converter<Jwt, AuthPrincipalAuthenticationToken> {

	private static final String PRINCIPAL_TYPE_CLAIM = "principalType";
	private static final String FAMILY_ID_CLAIM = "familyId";
	private static final String ROLE_CLAIM = "role";

	@Override
	public AuthPrincipalAuthenticationToken convert(Jwt jwt) {
		AuthPrincipal principal = new AuthPrincipal(principalType(jwt), subjectId(jwt), familyId(jwt), role(jwt));
		return new AuthPrincipalAuthenticationToken(principal, jwt);
	}

	private static PrincipalType principalType(Jwt jwt) {
		String value = jwt.getClaimAsString(PRINCIPAL_TYPE_CLAIM);
		if (value == null) {
			throw invalidClaim(PRINCIPAL_TYPE_CLAIM);
		}
		return enumClaim(PrincipalType.class, PRINCIPAL_TYPE_CLAIM, value);
	}

	private static long subjectId(Jwt jwt) {
		try {
			return Long.parseLong(jwt.getSubject());
		} catch (NumberFormatException e) {
			throw invalidClaim(JwtClaimNames.SUB);
		}
	}

	private static Long familyId(Jwt jwt) {
		Object value = jwt.getClaim(FAMILY_ID_CLAIM);
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		throw invalidClaim(FAMILY_ID_CLAIM);
	}

	private static Role role(Jwt jwt) {
		String value = jwt.getClaimAsString(ROLE_CLAIM);
		return value == null ? null : enumClaim(Role.class, ROLE_CLAIM, value);
	}

	private static <E extends Enum<E>> E enumClaim(Class<E> type, String claim, String value) {
		try {
			return Enum.valueOf(type, value);
		} catch (IllegalArgumentException e) {
			throw invalidClaim(claim);
		}
	}

	private static InvalidBearerTokenException invalidClaim(String claim) {
		return new InvalidBearerTokenException("Invalid " + claim + " claim");
	}

}
