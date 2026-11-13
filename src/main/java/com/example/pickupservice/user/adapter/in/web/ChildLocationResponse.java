package com.example.pickupservice.user.adapter.in.web;

public record ChildLocationResponse(
		boolean childLocationEnabled,
		int settingsVersion
) {
}
