package com.example.pickupservice.auth.application.port.out;

import java.util.List;

public interface PublishAuthEventPort {

	void publishDeviceReplaced(List<Long> previousDeviceIds);
}
