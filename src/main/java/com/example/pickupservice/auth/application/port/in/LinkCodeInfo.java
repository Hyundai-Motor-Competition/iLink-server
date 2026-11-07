package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.LinkMode;

import java.time.Instant;

public record LinkCodeInfo(LinkMode status, String vehicleModel, String plateNumberMasked, Instant expiresAt) {
}
