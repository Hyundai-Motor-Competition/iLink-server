package com.example.pickupservice.notification.application.port.in;

public interface RecordDeliveryUseCase {

	void recordDelivery(DeliveryReceiptCommand command);
}
