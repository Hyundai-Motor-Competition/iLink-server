package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;

record IdempotentRequest(AuthPrincipal principal, String key, String requestHash) {

	PrincipalType principalType() {
		return principal == null ? null : principal.type();
	}

	Long principalId() {
		return principal == null ? null : principal.id();
	}

}
