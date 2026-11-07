package com.example.pickupservice.user.domain;

public final class PlateNumberMasker {

	private static final String LAST_FOUR_DIGITS = "\\d{4}$";
	private static final String MASK = "****";

	private PlateNumberMasker() {
	}

	public static String mask(String plateNumber) {
		if (plateNumber == null) {
			return null;
		}
		return plateNumber.replaceFirst(LAST_FOUR_DIGITS, MASK);
	}
}
