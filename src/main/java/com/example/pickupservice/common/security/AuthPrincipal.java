package com.example.pickupservice.common.security;

import java.io.Serializable;

public record AuthPrincipal(PrincipalType type, long id, Long familyId, Role role) implements Serializable {
}
