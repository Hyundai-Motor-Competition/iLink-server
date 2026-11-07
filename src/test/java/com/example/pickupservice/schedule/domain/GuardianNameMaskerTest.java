package com.example.pickupservice.schedule.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianNameMaskerTest {

	@Test
	@DisplayName("이름 마스킹은 김보호를 김*호로 바꾼다")
	void masksMiddleCharacterOfThreeCharacterName() {
		assertThat(GuardianNameMasker.mask("김보호")).isEqualTo("김*호");
	}

	@ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
	@CsvSource({
			"남궁민수, 남**수",
			"제갈공명이, 제***이",
			"이보호자님, 이***님"
	})
	@DisplayName("이름 마스킹은 4자 이상이면 첫 글자와 마지막 글자만 남기고 가운데를 모두 *로 바꾼다")
	void keepsFirstAndLastCharacterAndMasksAllMiddleCharacters(String name, String expected) {
		assertThat(GuardianNameMasker.mask(name)).isEqualTo(expected);
	}

	@Test
	@DisplayName("이름 마스킹은 2자 이름이면 두 번째 글자를 *로 바꾼다(김호 -> 김*)")
	void masksSecondCharacterOfTwoCharacterName() {
		assertThat(GuardianNameMasker.mask("김호")).isEqualTo("김*");
	}

	@Test
	@DisplayName("이름 마스킹은 null 이름에 null을 돌려준다")
	void returnsNullForNullName() {
		assertThat(GuardianNameMasker.mask(null)).isNull();
	}
}
