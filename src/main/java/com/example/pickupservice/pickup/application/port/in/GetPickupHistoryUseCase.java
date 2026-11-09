package com.example.pickupservice.pickup.application.port.in;

import java.util.List;

public interface GetPickupHistoryUseCase {

	List<HistoryItem> history(HistoryQuery query);

}
