package com.example.pickupservice.auth.application.port.in;

import java.time.Instant;

public record LinkCodeResult(long codeId, String code, String qrPayload, Instant expiresAt, int reissuesLeft) {
}
