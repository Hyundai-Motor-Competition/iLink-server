package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.CurrentAuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

public class PreLinkTokenInterceptor implements HandlerInterceptor {

	private static final String ISSUE_LINK_CODE_PATH = "/v1/vehicle-link-codes";
	private static final Pattern LINK_STATUS_PATH = Pattern.compile("/v1/vehicle-link-codes/[^/]+/status");

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		AuthPrincipal principal = CurrentAuthPrincipal.find().orElse(null);
		if (principal == null || !isPreLinkVehicle(principal) || isAllowedForPreLink(request)) {
			return true;
		}
		throw BusinessException.of(ErrorCode.FORBIDDEN);
	}

	private static boolean isPreLinkVehicle(AuthPrincipal principal) {
		return principal.type() == PrincipalType.VEHICLE && principal.familyId() == null;
	}

	private static boolean isAllowedForPreLink(HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getServletPath();
		return ("POST".equals(method) && ISSUE_LINK_CODE_PATH.equals(path))
				|| ("GET".equals(method) && LINK_STATUS_PATH.matcher(path).matches());
	}
}
