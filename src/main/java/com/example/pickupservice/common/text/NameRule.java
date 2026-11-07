package com.example.pickupservice.common.text;

public final class NameRule {

	private static final int MIN_LENGTH = 1;
	private static final int MAX_LENGTH = 20;
	private static final int ASCII_LIMIT = 0x80;
	private static final int ZERO_WIDTH_JOINER = 0x200D;
	private static final int VARIATION_SELECTOR_16 = 0xFE0F;

	private NameRule() {
	}

	public static boolean isValid(String name) {
		if (name == null) {
			return false;
		}
		int length = name.codePointCount(0, name.length());
		if (length < MIN_LENGTH || length > MAX_LENGTH) {
			return false;
		}
		return name.codePoints().noneMatch(NameRule::isForbidden);
	}

	private static boolean isForbidden(int codePoint) {
		if (Character.isISOControl(codePoint)) {
			return true;
		}
		if (codePoint == ZERO_WIDTH_JOINER || codePoint == VARIATION_SELECTOR_16) {
			return true;
		}
		// 숫자 · # · * 같은 ASCII 문자도 isEmoji가 true라서 제외한다
		if (codePoint < ASCII_LIMIT) {
			return false;
		}
		return Character.isEmoji(codePoint) || Character.isExtendedPictographic(codePoint);
	}
}
