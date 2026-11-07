package com.example.pickupservice.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneRuleTest {

	@Test
	@DisplayName("전화번호 규칙은 +821012345678을 받고 01012345678을 거절한다")
	void acceptsInternationalFormatAndRejectsDomesticFormat() {
		assertThat(PhoneRule.isValid("+821012345678")).isTrue();
		assertThat(PhoneRule.isValid("01012345678")).isFalse();
	}

	@Test
	@DisplayName("전화번호 규칙은 null 전화번호를 거절한다")
	void rejectsNullPhone() {
		assertThat(PhoneRule.isValid(null)).isFalse();
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {"+821012345678", "+12025550123", "+821098765432", "+123456789012345"})
	@DisplayName("전화번호 규칙은 + 뒤 첫 자리가 1~9이고 숫자가 15자리 이하인 E.164 번호를 받는다")
	void acceptsE164Numbers(String phone) {
		assertThat(PhoneRule.isValid(phone)).isTrue();
	}

	static Stream<Arguments> invalidPhones() {
		return Stream.of(
				Arguments.of("국내 형식 010", "01012345678"),
				Arguments.of("+ 없는 국가번호", "821012345678"),
				Arguments.of("하이픈 포함", "+82-10-1234-5678"),
				Arguments.of("공백 포함", "+82 10 1234 5678"),
				Arguments.of("+ 뒤 첫 자리 0", "+0123"),
				Arguments.of("+ 하나만", "+"),
				Arguments.of("빈 문자열", ""),
				Arguments.of("숫자 16자리", "+1234567890123456"),
				Arguments.of("앞 공백", " +821012345678"),
				Arguments.of("뒤 줄바꿈", "+821012345678\n"),
				Arguments.of("숫자 아닌 문자", "+82101234567a"),
				Arguments.of("+ 두 개", "++821012345678")
		);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("invalidPhones")
	@DisplayName("전화번호 규칙은 E.164 형식이 아닌 번호를 거절한다")
	void rejectsNonE164Numbers(String label, String phone) {
		assertThat(PhoneRule.isValid(phone)).isFalse();
	}
}
