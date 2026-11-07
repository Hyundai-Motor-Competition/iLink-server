package com.example.pickupservice.auth.application.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomLinkCodeGenerator implements LinkCodeGenerator {

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int LENGTH = 8;

	private final SecureRandom random = new SecureRandom();

	@Override
	public String generate() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
		}
		return code.toString();
	}
}
