package com.example.pickupservice.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter KST_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(SEOUL);

	private TimeUtils() {
	}

	public static String toKst(Instant utc) {
		return KST_FORMAT.format(utc);
	}

	public static Instant toUtc(OffsetDateTime requested) {
		return requested.toInstant();
	}

	public static LocalDate todayKst(Clock clock) {
		return LocalDate.ofInstant(clock.instant(), SEOUL);
	}

}
