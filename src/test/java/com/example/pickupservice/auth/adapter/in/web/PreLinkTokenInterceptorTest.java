package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.AuthPrincipalAuthenticationToken;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreLinkTokenInterceptorTest {

	private static final long VEHICLE_ID = 88L;

	private final PreLinkTokenInterceptor interceptor = new PreLinkTokenInterceptor();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("연결 전 기기 토큰으로는 LINK-01 · LINK-02 외의 API를 부를 수 없다")
	void preLinkVehicleTokenCanCallOnlyLink01AndLink02() throws Exception {
		authenticate(preLinkVehicle());

		assertThat(preHandle("POST", "/v1/vehicle-link-codes")).isTrue();
		assertThat(preHandle("GET", "/v1/vehicle-link-codes/5012/status")).isTrue();
		assertForbidden("PUT", "/v1/devices/me/push-token");
		assertForbidden("DELETE", "/v1/vehicle-devices/me/session");
	}

	@Test
	@DisplayName("연결 전 기기 토큰은 LINK-01 · LINK-02 경로라도 메서드가 다르거나 다른 하위 경로면 FORBIDDEN이다")
	void preLinkVehicleTokenIsForbiddenOnOtherMethodOrSubPathOfLinkCodes() {
		authenticate(preLinkVehicle());

		assertForbidden("GET", "/v1/vehicle-link-codes");
		assertForbidden("POST", "/v1/vehicle-link-codes/5012/status");
		assertForbidden("GET", "/v1/vehicle-link-codes/5012");
		assertForbidden("GET", "/v1/vehicle-link-codes/5012/status/extra");
		assertForbidden("GET", "/v1/vehicles/me/schedules");
	}

	@Test
	@DisplayName("연결된 VEHICLE 토큰은 LINK-01 · LINK-02 외의 API도 막지 않는다")
	void linkedVehicleTokenPassesAnyPath() throws Exception {
		authenticate(new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, 12L, null));

		assertThat(preHandle("PUT", "/v1/devices/me/push-token")).isTrue();
		assertThat(preHandle("DELETE", "/v1/vehicle-devices/me/session")).isTrue();
		assertThat(preHandle("POST", "/v1/vehicle-link-codes")).isTrue();
	}

	@Test
	@DisplayName("USER 토큰은 familyId와 상관없이 막지 않는다")
	void userTokenPassesAnyPath() throws Exception {
		authenticate(new AuthPrincipal(PrincipalType.USER, 31L, 12L, Role.GUARDIAN));

		assertThat(preHandle("PUT", "/v1/devices/me/push-token")).isTrue();
		assertThat(preHandle("POST", "/v1/families/me/members/40/invites")).isTrue();
	}

	@Test
	@DisplayName("인증 없는 요청은 막지 않는다")
	void anonymousRequestPasses() throws Exception {
		assertThat(preHandle("POST", "/v1/vehicle-devices")).isTrue();
		assertThat(preHandle("GET", "/v1/vehicle-link-codes/by-code/K7P2QX9M")).isTrue();
		assertThat(preHandle("POST", "/v1/vehicle-links")).isTrue();
	}

	private static AuthPrincipal preLinkVehicle() {
		return new AuthPrincipal(PrincipalType.VEHICLE, VEHICLE_ID, null, null);
	}

	private static void authenticate(AuthPrincipal principal) {
		SecurityContextHolder.getContext().setAuthentication(new AuthPrincipalAuthenticationToken(principal, null));
	}

	private boolean preHandle(String method, String path) throws Exception {
		return interceptor.preHandle(request(method, path), new MockHttpServletResponse(), new Object());
	}

	private void assertForbidden(String method, String path) {
		assertThatThrownBy(() -> preHandle(method, path))
				.as("%s %s", method, path)
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN);
					assertThat(e.code()).isEqualTo("FORBIDDEN");
				});
	}

	private static MockHttpServletRequest request(String method, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setServletPath(path);
		return request;
	}

}
