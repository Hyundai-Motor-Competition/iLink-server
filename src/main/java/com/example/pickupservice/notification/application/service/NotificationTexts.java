package com.example.pickupservice.notification.application.service;

import java.util.Map;

final class NotificationTexts {

	private static final Map<String, String[]> TEXTS = Map.of(
			"SESSION_STARTED", new String[] {"픽업 시작", "픽업이 시작됐어요"},
			"DEPARTURE", new String[] {"출발 알림", "차량이 출발했어요"},
			"CALL", new String[] {"이제 나와요", "차량이 도착했어요. 이제 나와요"},
			"PREP_EXTENDED", new String[] {"준비 시간 연장", "준비 시간이 늘어났어요"},
			"HELP", new String[] {"도움 요청", "도움이 필요해요"});

	private NotificationTexts() {
	}

	static String title(String type) {
		String[] text = TEXTS.get(type);
		return text == null ? null : text[0];
	}

	static String body(String type) {
		String[] text = TEXTS.get(type);
		return text == null ? null : text[1];
	}
}
