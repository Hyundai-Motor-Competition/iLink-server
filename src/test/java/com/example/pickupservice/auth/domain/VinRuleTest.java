package com.example.pickupservice.auth.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VinRuleTest {

	@Test
	@DisplayName("VIN 규칙은 17자 영대문자 · 숫자를 받고 I · O · Q가 들어 있으면 거절한다")
	void acceptsSeventeenUppercaseAlphanumericAndRejectsIOQ() {
		assertThat(VinRule.isValid("KMHKN81BFNU123456")).isTrue();
		assertThat(VinRule.isValid("KMHKN81BFNU12345I")).isFalse();
		assertThat(VinRule.isValid("KMHKN81BFNU12345O")).isFalse();
		assertThat(VinRule.isValid("KMHKN81BFNU12345Q")).isFalse();
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {"KMHKN81BFNU123456", "12345678901234567", "ABCDEFGHJKLMNPRST", "WVWZZZ1JZXW000001"})
	@DisplayName("VIN 규칙은 I · O · Q가 없는 17자 영대문자 · 숫자를 받는다")
	void acceptsSeventeenUppercaseAlphanumericWithoutIOQ(String vin) {
		assertThat(VinRule.isValid(vin)).isTrue();
	}

	static Stream<Arguments> invalidVins() {
		return Stream.of(
				Arguments.of("16자", "KMHKN81BFNU12345"),
				Arguments.of("18자", "KMHKN81BFNU1234567"),
				Arguments.of("빈 문자열", ""),
				Arguments.of("소문자 포함", "kMHKN81BFNU123456"),
				Arguments.of("전부 소문자", "kmhkn81bfnu123456"),
				Arguments.of("I 포함", "KMHKN81BFIU123456"),
				Arguments.of("O 포함", "KMHKN81BFOU123456"),
				Arguments.of("Q 포함", "KMHKN81BFQU123456"),
				Arguments.of("가운데 공백", "KMHKN81B NU123456"),
				Arguments.of("앞 공백 + 17자", " KMHKN81BFNU123456"),
				Arguments.of("뒤 공백 + 17자", "KMHKN81BFNU123456 "),
				Arguments.of("하이픈 포함", "KMHKN81B-NU123456"),
				Arguments.of("한글 포함", "KMHKN81B가NU123456"),
				Arguments.of("전각 숫자 포함", "KMHKN81BFNU12345６")
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidVins")
	@DisplayName("VIN 규칙은 길이 · 문자 구성이 틀린 VIN을 거절한다")
	void rejectsVinsWithWrongLengthOrCharacters(String label, String vin) {
		assertThat(VinRule.isValid(vin)).isFalse();
	}

	@Test
	@DisplayName("VIN 규칙은 null을 거절한다")
	void rejectsNull() {
		assertThat(VinRule.isValid(null)).isFalse();
	}
}
