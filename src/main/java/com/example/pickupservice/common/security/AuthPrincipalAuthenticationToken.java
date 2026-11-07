package com.example.pickupservice.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public final class AuthPrincipalAuthenticationToken extends AbstractAuthenticationToken {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String PRINCIPAL_AUTHORITY_PREFIX = "PRINCIPAL_";
	private static final String ROLE_AUTHORITY_PREFIX = "ROLE_";

	private final AuthPrincipal principal;
	private final Jwt jwt;

	public AuthPrincipalAuthenticationToken(AuthPrincipal principal, Jwt jwt) {
		super(authoritiesOf(principal));
		this.principal = principal;
		this.jwt = jwt;
		setAuthenticated(true);
	}

	private static List<GrantedAuthority> authoritiesOf(AuthPrincipal principal) {
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(PRINCIPAL_AUTHORITY_PREFIX + principal.type().name()));
		if (principal.type() == PrincipalType.USER && principal.role() != null) {
			authorities.add(new SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + principal.role().name()));
		}
		return authorities;
	}

	@Override
	public AuthPrincipal getPrincipal() {
		return principal;
	}

	@Override
	public Jwt getCredentials() {
		return jwt;
	}

}
