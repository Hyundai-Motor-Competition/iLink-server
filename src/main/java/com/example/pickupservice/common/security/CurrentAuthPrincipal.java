package com.example.pickupservice.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class CurrentAuthPrincipal {

	private CurrentAuthPrincipal() {
	}

	public static Optional<AuthPrincipal> find() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
				.flatMap(CurrentAuthPrincipal::of);
	}

	public static Optional<AuthPrincipal> of(Authentication authentication) {
		return authentication.getPrincipal() instanceof AuthPrincipal principal
				? Optional.of(principal)
				: Optional.empty();
	}

}
