package com.example.pickupservice.schedule.domain;

public final class GuardianNameMasker {

	private GuardianNameMasker() {
	}

	public static String mask(String name) {
		if (name == null) {
			return null;
		}
		int[] codePoints = name.codePoints().toArray();
		int length = codePoints.length;
		if (length < 2) {
			return name;
		}
		StringBuilder masked = new StringBuilder().appendCodePoint(codePoints[0]);
		if (length == 2) {
			return masked.append('*').toString();
		}
		masked.append("*".repeat(length - 2));
		return masked.appendCodePoint(codePoints[length - 1]).toString();
	}
}
