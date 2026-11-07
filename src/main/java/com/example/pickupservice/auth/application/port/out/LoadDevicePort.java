package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;

import java.util.List;
import java.util.Optional;

public interface LoadDevicePort {

	Optional<Device> loadDevice(long deviceId);

	List<Device> loadLiveDevices(OwnerType type, long ownerId);

	Optional<Device> loadByRefreshTokenHash(String hash);
}
