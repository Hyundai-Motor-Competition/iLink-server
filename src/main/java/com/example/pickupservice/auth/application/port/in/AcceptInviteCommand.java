package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.Platform;

import java.util.List;

public record AcceptInviteCommand(List<ConsentAgreement> consents, Platform platform, String pushToken) {
}
