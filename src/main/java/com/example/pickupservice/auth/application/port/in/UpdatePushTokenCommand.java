package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.Platform;

public record UpdatePushTokenCommand(long deviceId, Platform platform, String pushToken) {
}
