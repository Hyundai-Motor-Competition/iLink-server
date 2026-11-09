package com.example.pickupservice.notification.application.port.out;

import com.example.pickupservice.auth.application.port.in.PushTarget;

import java.util.Map;

public record PushMessage(
		PushTarget target,
		String title,
		String body,
		Map<String, String> data
) {
}
