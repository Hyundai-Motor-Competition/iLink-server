package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.time.TimeUtils;
import com.example.pickupservice.schedule.application.port.in.TodayPickup;
import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;

import java.util.List;

public record TodayPickupsResponse(
		List<Item> items
) {

	public static TodayPickupsResponse from(List<TodayPickup> pickups) {
		return new TodayPickupsResponse(pickups.stream().map(Item::from).toList());
	}

	public record Item(
			long scheduleId,
			String childName,
			String academyName,
			String targetAt,
			TodayPickupStatus status
	) {

		static Item from(TodayPickup pickup) {
			return new Item(pickup.scheduleId(), pickup.childName(), pickup.academyName(),
					TimeUtils.toKst(pickup.targetAt()), pickup.status());
		}
	}
}
