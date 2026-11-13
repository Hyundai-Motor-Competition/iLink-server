package com.example.pickupservice.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class FilterStageEnd {

	private static final String MARK = FilterStageEnd.class.getName() + ".mark";
	private static final String AUTHENTICATION = FilterStageEnd.class.getName() + ".authentication";

	private FilterStageEnd() {
	}

	public static void mark(HttpServletRequest request) {
		request.setAttribute(MARK, Boolean.TRUE);
		request.setAttribute(AUTHENTICATION, SecurityContextHolder.getContext().getAuthentication());
	}

	public static boolean isMarked(HttpServletRequest request) {
		return Boolean.TRUE.equals(request.getAttribute(MARK));
	}

	public static Optional<Authentication> authenticationOf(HttpServletRequest request) {
		return request.getAttribute(AUTHENTICATION) instanceof Authentication authentication
				? Optional.of(authentication)
				: Optional.empty();
	}

}
