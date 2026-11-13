package com.example.pickupservice.common.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsTest {

	@Test
	@DisplayName("TimeUtils.toKst는 UTC 2026-11-03T09:12:00Z를 2026-11-03T18:12:00+09:00 문자열로 바꾼다")
	void toKstConvertsUtcInstantToSeoulOffsetString() {
		Instant utc = Instant.parse("2026-11-03T09:12:00Z");

		String kst = TimeUtils.toKst(utc);

		assertThat(kst).isEqualTo("2026-11-03T18:12:00+09:00");
	}

	@Test
	@DisplayName("TimeUtils.toKst는 한국 시간으로 날짜가 넘어가는 UTC 2026-11-05T15:30:00Z를 2026-11-06T00:30:00+09:00으로 바꾼다")
	void toKstMovesToNextDateWhenSeoulTimePassesMidnight() {
		Instant utc = Instant.parse("2026-11-05T15:30:00Z");

		String kst = TimeUtils.toKst(utc);

		assertThat(kst).isEqualTo("2026-11-06T00:30:00+09:00");
	}

	@Test
	@DisplayName("TimeUtils.toUtc는 2026-11-03T18:12:00+09:00을 UTC 2026-11-03T09:12:00Z로 바꾼다")
	void toUtcConvertsSeoulOffsetDateTimeToUtcInstant() {
		OffsetDateTime requested = OffsetDateTime.parse("2026-11-03T18:12:00+09:00");

		Instant utc = TimeUtils.toUtc(requested);

		assertThat(utc).isEqualTo(Instant.parse("2026-11-03T09:12:00Z"));
	}

	@Test
	@DisplayName("TimeUtils.toUtc는 한국 자정을 넘긴 2026-11-06T00:30:00+09:00을 전날 UTC 2026-11-05T15:30:00Z로 바꾼다")
	void toUtcMovesToPreviousDateWhenSeoulTimeIsAfterMidnight() {
		OffsetDateTime requested = OffsetDateTime.parse("2026-11-06T00:30:00+09:00");

		Instant utc = TimeUtils.toUtc(requested);

		assertThat(utc).isEqualTo(Instant.parse("2026-11-05T15:30:00Z"));
	}

	@Test
	@DisplayName("TimeUtils.todayKst는 UTC 2026-11-05T15:30:00Z 시점에 한국 날짜 2026-11-06을 돌려준다")
	void todayKstReturnsSeoulDateWhenUtcDateIsStillPreviousDay() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-05T15:30:00Z"), ZoneOffset.UTC);

		LocalDate today = TimeUtils.todayKst(clock);

		assertThat(today).isEqualTo(LocalDate.of(2026, 11, 6));
	}

	@Test
	@DisplayName("TimeUtils.todayKst는 한국 자정 1초 전인 UTC 2026-11-05T14:59:59Z 시점에 한국 날짜 2026-11-05를 돌려준다")
	void todayKstReturnsSameDateJustBeforeSeoulMidnight() {
		Clock clock = Clock.fixed(Instant.parse("2026-11-05T14:59:59Z"), ZoneOffset.UTC);

		LocalDate today = TimeUtils.todayKst(clock);

		assertThat(today).isEqualTo(LocalDate.of(2026, 11, 5));
	}

	private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");

	@Test
	@DisplayName("TimeUtils.secondsUntil은 허용 시각까지 1ms 남았으면 1초로 올린다")
	void secondsUntilRoundsUpOneMillisecondToOneSecond() {
		assertThat(TimeUtils.secondsUntil(NOW, NOW.plusMillis(1))).isEqualTo(1);
	}

	@Test
	@DisplayName("TimeUtils.secondsUntil은 지금이 허용 시각과 같으면(0ms 남음) 최소값 1을 돌려준다")
	void secondsUntilReturnsOneWhenNoTimeLeft() {
		assertThat(TimeUtils.secondsUntil(NOW, NOW)).isEqualTo(1);
	}

	@Test
	@DisplayName("TimeUtils.secondsUntil은 허용 시각이 이미 지났으면 최소값 1을 돌려준다")
	void secondsUntilReturnsOneWhenUntilIsInThePast() {
		assertThat(TimeUtils.secondsUntil(NOW, NOW.minusSeconds(5))).isEqualTo(1);
	}

	@Test
	@DisplayName("TimeUtils.secondsUntil은 허용 시각까지 29.5초 남았으면 30초로 올린다")
	void secondsUntilRoundsUpTwentyNinePointFiveSecondsToThirty() {
		assertThat(TimeUtils.secondsUntil(NOW, NOW.plusMillis(29_500))).isEqualTo(30);
	}

	@Test
	@DisplayName("TimeUtils.secondsUntil은 허용 시각까지 딱 60초 남았으면 60초를 돌려준다")
	void secondsUntilReturnsExactSecondsWhenNoFraction() {
		assertThat(TimeUtils.secondsUntil(NOW, NOW.plusSeconds(60))).isEqualTo(60);
	}

}
