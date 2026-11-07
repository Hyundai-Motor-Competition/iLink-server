package com.example.pickupservice.auth.domain;

import java.util.regex.Pattern;

public final class VinRule {

	private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

	private VinRule() {
	}

	public static boolean isValid(String vin) {
		return vin != null && VIN_PATTERN.matcher(vin).matches();
	}
}
