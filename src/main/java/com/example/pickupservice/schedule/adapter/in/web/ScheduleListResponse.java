package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.ScheduleView;

import java.util.List;

public record ScheduleListResponse(
		List<ScheduleItemResponse> items
) {

	public static ScheduleListResponse from(List<ScheduleView> views) {
		return new ScheduleListResponse(views.stream().map(ScheduleItemResponse::from).toList());
	}
}
