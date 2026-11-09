package com.example.pickupservice.notification.adapter.out.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pickup.fcm")
public record FcmProperties(String projectId, String credentialsPath) {
}
