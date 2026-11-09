package com.example.pickupservice.notification.application.port.in;

import java.util.List;

public interface NotifyDeviceReplacedUseCase {

	void notifyDeviceReplaced(List<Long> previousDeviceIds);
}
