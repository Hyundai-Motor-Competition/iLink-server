package com.example.pickupservice.user.domain;

import java.util.regex.Pattern;

public final class PhoneRule {

	private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");

	private PhoneRule() {
	}

	public static boolean isValid(String phone) {
		if (phone == null) {
			return false;
		}
		return E164.matcher(phone).matches();
	}
}
