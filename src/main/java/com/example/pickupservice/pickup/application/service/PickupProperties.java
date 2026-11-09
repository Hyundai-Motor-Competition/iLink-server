package com.example.pickupservice.pickup.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// 설계서 「서버 기준값」
@ConfigurationProperties("pickup.session")
public record PickupProperties(
		Duration etaValidFor,
		Duration callTriggerEta,
		Duration boardingCancelWindow,
		int callTriggerDistanceMeters,
		Duration helpRequestInterval,
		Duration prepExtensionLimit,
		int dataRetentionDays
) {

	public static PickupProperties defaults() {
		return new PickupProperties(
				Duration.ofMinutes(1),
				Duration.ofSeconds(180),
				Duration.ofSeconds(10),
				700,
				Duration.ofSeconds(60),
				Duration.ofMinutes(15),
				90);
	}

}
