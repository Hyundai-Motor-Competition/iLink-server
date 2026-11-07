package com.example.pickupservice.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlateNumberMaskerTest {

	@Test
	@DisplayName("번호판 마스킹은 「12가 3456」을 「12가 ****」로 바꾼다")
	void masksLastFourDigitsOfPlateNumber() {
		assertThat(PlateNumberMasker.mask("12가 3456")).isEqualTo("12가 ****");
	}

	@ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
	@CsvSource({
			"12가 3456, 12가 ****",
			"123가 4567, 123가 ****",
			"서울12가3456, 서울12가****"
	})
	@DisplayName("번호판 마스킹은 앞부분을 그대로 두고 마지막 숫자 4자리만 *로 바꾼다")
	void keepsPrefixAndMasksOnlyLastFourDigits(String plateNumber, String expected) {
		assertThat(PlateNumberMasker.mask(plateNumber)).isEqualTo(expected);
	}

	@Test
	@DisplayName("번호판 마스킹은 null 번호판에 null을 돌려준다")
	void returnsNullForNullPlateNumber() {
		assertThat(PlateNumberMasker.mask(null)).isNull();
	}
}
