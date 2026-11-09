package com.example.pickupservice.pickup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HelpRequestRuleTest {

	private static final Duration INTERVAL = Duration.ofSeconds(60);
	private static final Instant LAST_REQUESTED_AT = Instant.parse("2026-11-09T09:10:00Z");

	@Test
	@DisplayName("도움 요청 규칙은 같은 아이의 마지막 요청 뒤 60초 안이면 HELP_REQUEST_TOO_SOON과 마지막 시각 + 60초를 돌려준다")
	void rejectsWithinIntervalAndReturnsLastRequestPlusInterval() {
		Instant now = LAST_REQUESTED_AT.plusSeconds(30);

		HelpRequestDecision decision = HelpRequestRule.decide(Optional.of(LAST_REQUESTED_AT), now, INTERVAL);

		assertThat(decision).isEqualTo(new HelpRequestDecision(false, Instant.parse("2026-11-09T09:11:00Z")));
	}

	@Test
	@DisplayName("도움 요청 규칙은 마지막 요청 뒤 59초여도 HELP_REQUEST_TOO_SOON과 마지막 시각 + 60초를 돌려준다")
	void rejectsAtFiftyNineSecondsAfterLastRequest() {
		Instant now = LAST_REQUESTED_AT.plusSeconds(59);

		HelpRequestDecision decision = HelpRequestRule.decide(Optional.of(LAST_REQUESTED_AT), now, INTERVAL);

		assertThat(decision).isEqualTo(new HelpRequestDecision(false, Instant.parse("2026-11-09T09:11:00Z")));
	}

	@Test
	@DisplayName("도움 요청 규칙은 마지막 요청과 같은 시각의 재요청도 HELP_REQUEST_TOO_SOON으로 돌려준다")
	void rejectsAtSameInstantAsLastRequest() {
		HelpRequestDecision decision = HelpRequestRule.decide(Optional.of(LAST_REQUESTED_AT), LAST_REQUESTED_AT, INTERVAL);

		assertThat(decision).isEqualTo(new HelpRequestDecision(false, Instant.parse("2026-11-09T09:11:00Z")));
	}

	@Test
	@DisplayName("도움 요청 규칙은 마지막 요청 뒤 정확히 60초면 허용하고 지금 + 60초를 돌려준다")
	void allowsExactlyAtIntervalAndReturnsNowPlusInterval() {
		Instant now = LAST_REQUESTED_AT.plusSeconds(60);

		HelpRequestDecision decision = HelpRequestRule.decide(Optional.of(LAST_REQUESTED_AT), now, INTERVAL);

		assertThat(decision).isEqualTo(new HelpRequestDecision(true, Instant.parse("2026-11-09T09:12:00Z")));
	}

	@Test
	@DisplayName("도움 요청 규칙은 마지막 요청이 없으면 허용하고 지금 + 60초를 돌려준다")
	void allowsWhenNoPreviousRequest() {
		Instant now = Instant.parse("2026-11-09T09:15:30Z");

		HelpRequestDecision decision = HelpRequestRule.decide(Optional.empty(), now, INTERVAL);

		assertThat(decision).isEqualTo(new HelpRequestDecision(true, Instant.parse("2026-11-09T09:16:30Z")));
	}
}
