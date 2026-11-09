package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;

import java.time.Instant;

final class KstTime {

	private KstTime() {
	}

	static String kst(Instant instant) {
		return instant == null ? null : TimeUtils.toKst(instant);
	}

}
