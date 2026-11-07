package com.example.pickupservice.auth.application.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRandomInviteTokenGenerator implements InviteTokenGenerator {

	private static final int BYTE_LENGTH = 32;

	private final SecureRandom random = new SecureRandom();

	@Override
	public String generate() {
		byte[] bytes = new byte[BYTE_LENGTH];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
