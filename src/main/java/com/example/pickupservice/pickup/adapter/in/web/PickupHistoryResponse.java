package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.domain.SessionStatus;

import java.time.LocalDate;
import java.util.List;

import static com.example.pickupservice.pickup.adapter.in.web.KstTime.kst;

public record PickupHistoryResponse(
		List<Item> items
) {

	public static PickupHistoryResponse from(List<HistoryItem> items) {
		return new PickupHistoryResponse(items.stream().map(Item::from).toList());
	}

	public record Item(LocalDate date, long sessionId, SessionStatus status, String completedAt) {

		static Item from(HistoryItem item) {
			return new Item(item.date(), item.sessionId(), item.status(), kst(item.completedAt()));
		}
	}

}
