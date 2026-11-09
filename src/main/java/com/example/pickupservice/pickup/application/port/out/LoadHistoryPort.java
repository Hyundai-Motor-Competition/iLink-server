package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.pickup.application.port.in.HistoryItem;

import java.time.LocalDate;
import java.util.List;

public interface LoadHistoryPort {

	List<HistoryItem> loadHistory(long childUserId, LocalDate from, LocalDate to);

}
