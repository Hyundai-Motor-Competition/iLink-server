package com.example.pickupservice.auth.application.port.in;

public record IssueInviteCommand(long familyId, long userId, boolean replaceDevice) {
}
