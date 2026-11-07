package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.common.security.Role;

public record AcceptInviteResult(long userId, long familyId, Role role, String accessToken, String refreshToken) {
}
