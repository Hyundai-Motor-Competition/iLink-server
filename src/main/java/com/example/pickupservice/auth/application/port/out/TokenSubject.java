package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;

public record TokenSubject(PrincipalType type, long id, Long familyId, Role role, long deviceId) {
}
