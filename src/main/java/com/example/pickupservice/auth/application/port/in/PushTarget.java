package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.Platform;

public record PushTarget(long userId, long deviceId, Platform platform, String pushToken) {
}
