package com.example.pickupservice.schedule.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidScheduleExceptionTest {

	private static final Path SOURCE = Path.of(
			"src/main/java/com/example/pickupservice/schedule/domain/InvalidScheduleException.java");

	@Test
	@DisplayName("일정 도메인 예외는 RuntimeException을 바로 상속한다 (common.error.BusinessException이 아니다)")
	void extendsRuntimeExceptionDirectly() {
		assertThat(InvalidScheduleException.class.getSuperclass()).isEqualTo(RuntimeException.class);
	}

	@Test
	@DisplayName("일정 도메인 예외는 받은 field와 reason을 그대로 돌려준다")
	void keepsFieldAndReason() {
		InvalidScheduleException exception = new InvalidScheduleException("targetPickup", "0~60이어야 합니다");

		assertThat(exception.field()).isEqualTo("targetPickup");
		assertThat(exception.reason()).isEqualTo("0~60이어야 합니다");
	}

	@Test
	@DisplayName("일정 도메인 예외 소스는 Spring과 common.error를 import하지 않는다")
	void sourceHasNoSpringOrCommonErrorImport() throws IOException {
		String source = Files.readString(SOURCE, StandardCharsets.UTF_8);

		assertThat(source).doesNotContain("org.springframework").doesNotContain("common.error");
	}
}
