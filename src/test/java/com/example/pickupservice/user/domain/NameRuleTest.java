package com.example.pickupservice.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NameRuleTest {

	@Test
	@DisplayName("이름 규칙은 0자와 21자 이름을 거절하고 1자와 20자 이름을 받는다")
	void rejectsZeroAndTwentyOneCharactersAndAcceptsOneAndTwenty() {
		assertThat(NameRule.isValid("")).isFalse();
		assertThat(NameRule.isValid("가".repeat(21))).isFalse();
		assertThat(NameRule.isValid("가")).isTrue();
		assertThat(NameRule.isValid("가".repeat(20))).isTrue();
	}

	@Test
	@DisplayName("이름 규칙은 null 이름을 거절한다")
	void rejectsNullName() {
		assertThat(NameRule.isValid(null)).isFalse();
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {"민준", "김보호", "a", "Teacher Kim", "abcdefghijabcdefghij"})
	@DisplayName("이름 규칙은 1~20자 한글 · 영문 이름을 받는다")
	void acceptsKoreanAndEnglishNamesWithinOneToTwentyCharacters(String name) {
		assertThat(NameRule.isValid(name)).isTrue();
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {"abcdefghijabcdefghijk", "가나다라마바사아자차카타파하가나다라마바사"})
	@DisplayName("이름 규칙은 21자 한글 · 영문 이름을 거절한다")
	void rejectsKoreanAndEnglishNamesOfTwentyOneCharacters(String name) {
		assertThat(NameRule.isValid(name)).isFalse();
	}

	@Test
	@DisplayName("이름 규칙은 이모지나 제어문자가 든 이름을 거절한다")
	void rejectsNamesContainingEmojiOrControlCharacters() {
		assertThat(NameRule.isValid("민준😀")).isFalse();
		assertThat(NameRule.isValid("😀")).isFalse();
		assertThat(NameRule.isValid("👨‍👩‍👧")).isFalse();
		assertThat(NameRule.isValid("민\0준")).isFalse();
		assertThat(NameRule.isValid("민\n준")).isFalse();
		assertThat(NameRule.isValid("민\t준")).isFalse();
		assertThat(NameRule.isValid("\u007F")).isFalse();
		assertThat(NameRule.isValid("민준")).isTrue();
	}

	static Stream<Arguments> emojiNames() {
		return Stream.of(
				Arguments.of("이름 뒤 이모지 U+1F600", "민준😀"),
				Arguments.of("이모지 하나만(1코드포인트)", "😀"),
				Arguments.of("ZWJ 조합 가족 이모지", "👨‍👩‍👧"),
				Arguments.of("이름 앞 하트 이모지 U+2764 U+FE0F", "❤️서윤"),
				Arguments.of("BMP 별 이모지 U+2B50", "김⭐보호")
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("emojiNames")
	@DisplayName("이름 규칙은 이모지가 든 이름을 거절한다")
	void rejectsNamesContainingEmoji(String label, String name) {
		assertThat(NameRule.isValid(name)).isFalse();
	}

	static Stream<Arguments> controlCharacterNames() {
		return Stream.of(
				Arguments.of("NUL U+0000", "민\0준"),
				Arguments.of("줄바꿈 U+000A", "민\n준"),
				Arguments.of("탭 U+0009", "민\t준"),
				Arguments.of("캐리지 리턴 U+000D", "민준\r"),
				Arguments.of("DEL U+007F 하나만", "\u007F"),
				Arguments.of("C1 제어문자 U+0085", "민\u0085준")
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("controlCharacterNames")
	@DisplayName("이름 규칙은 제어문자가 든 이름을 거절한다")
	void rejectsNamesContainingControlCharacters(String label, String name) {
		assertThat(NameRule.isValid(name)).isFalse();
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {"민준", "Teacher Kim", "김-보호", "선생님 2", "Kim0123456789", "#1 Kim*"})
	@DisplayName("이름 규칙은 이모지 · 제어문자가 없는 한글 · 영문 · 숫자 · 공백 · 기호 이름을 받는다")
	void acceptsNamesWithoutEmojiOrControlCharacters(String name) {
		assertThat(NameRule.isValid(name)).isTrue();
	}
}
