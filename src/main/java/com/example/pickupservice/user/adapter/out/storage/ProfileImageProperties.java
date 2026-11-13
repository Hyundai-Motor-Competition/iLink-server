package com.example.pickupservice.user.adapter.out.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("pickup.profile")
public record ProfileImageProperties(
		String endpoint,
		String publicEndpoint,
		String bucket,
		String accessKey,
		String secretKey,
		Duration urlTtl
) {
}
