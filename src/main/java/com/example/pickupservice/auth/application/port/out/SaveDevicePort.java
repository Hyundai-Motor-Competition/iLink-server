package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.Device;

public interface SaveDevicePort {

	Device saveDevice(Device device);
}
