package com.example.pickupservice.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

	@Test
	@DisplayName("conflictWithOverlap(409 · SCHEDULE_OVERLAP · 메시지 · [801, 802])로 만든 예외는 status 409 · code SCHEDULE_OVERLAP · 메시지 그대로 · overlapScheduleIds [801, 802] · retryAfterSeconds null · fields null이다")
	void conflictWithOverlapKeepsOverlapScheduleIds() {
		BusinessException exception = BusinessException.conflictWithOverlap(
				HttpStatus.CONFLICT, "SCHEDULE_OVERLAP", "겹치는 일정이 있습니다", List.of(801L, 802L));

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("SCHEDULE_OVERLAP");
		assertThat(exception.getMessage()).isEqualTo("겹치는 일정이 있습니다");
		assertThat(exception.overlapScheduleIds()).containsExactly(801L, 802L);
		assertThat(exception.retryAfterSeconds()).isNull();
		assertThat(exception.fields()).isNull();
	}

	@Test
	@DisplayName("of(409 · SOME_DOMAIN_CODE · 메시지)로 만든 예외의 overlapScheduleIds는 null이다")
	void ofWithoutOverlapHasNullOverlapScheduleIds() {
		BusinessException exception = BusinessException.of(HttpStatus.CONFLICT, "SOME_DOMAIN_CODE", "도메인 규칙에 맞지 않는 요청입니다");

		assertThat(exception.overlapScheduleIds()).isNull();
	}

}
