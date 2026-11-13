package com.example.pickupservice.common.logging;

import com.example.pickupservice.common.error.ErrorCodeAttribute;
import com.example.pickupservice.common.error.FilterStageEnd;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.CurrentAuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Optional;

final class RequestLogLine {

	static final String STREAM_OPENED_FORMAT = "requestId={} principal={} method={} path={} status={} stream=open";
	static final String COMPLETED_FORMAT = "requestId={} principal={} method={} path={} status={} durationMs={}{}{}";
	static final String NO_FIELD = "";

	private static final String UNKNOWN_PATH = "unknown";
	private static final String ANONYMOUS = "ANONYMOUS";

	private RequestLogLine() {
	}

	static String currentPrincipal() {
		return principalOf(CurrentAuthPrincipal.find());
	}

	static String filterStagePrincipalOf(HttpServletRequest request) {
		return principalOf(FilterStageEnd.authenticationOf(request).flatMap(CurrentAuthPrincipal::of));
	}

	static String pathPatternOf(HttpServletRequest request) {
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		return pattern == null ? UNKNOWN_PATH : String.valueOf(pattern);
	}

	static String errorCodeFieldOf(HttpServletRequest request) {
		Object errorCode = request.getAttribute(ErrorCodeAttribute.NAME);
		return errorCode == null ? NO_FIELD : " errorCode=" + errorCode;
	}

	private static String principalOf(Optional<AuthPrincipal> principal) {
		return principal.map(found -> found.type() + ":" + found.id()).orElse(ANONYMOUS);
	}

}
