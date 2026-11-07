package com.example.pickupservice.auth.application.port.in;

import java.time.Instant;

public record InviteResult(String inviteUrl, Instant expiresAt) {
}
