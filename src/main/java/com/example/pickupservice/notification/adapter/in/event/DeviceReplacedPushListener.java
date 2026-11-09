package com.example.pickupservice.notification.adapter.in.event;

import com.example.pickupservice.auth.application.port.out.DeviceReplacedEvent;
import com.example.pickupservice.notification.application.port.in.NotifyDeviceReplacedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DeviceReplacedPushListener {

	private final NotifyDeviceReplacedUseCase notifyDeviceReplacedUseCase;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDeviceReplaced(DeviceReplacedEvent event) {
		notifyDeviceReplacedUseCase.notifyDeviceReplaced(event.previousDeviceIds());
	}
}
