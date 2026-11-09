package com.example.pickupservice.pickup.domain;

import java.time.Instant;

public record HelpRequestDecision(boolean allowed, Instant nextAllowedAt) {
}
