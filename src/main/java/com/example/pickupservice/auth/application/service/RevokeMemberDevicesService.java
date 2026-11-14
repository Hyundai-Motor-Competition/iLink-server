package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.RevokeMemberDevicesUseCase;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RevokeMemberDevicesService implements RevokeMemberDevicesUseCase {

	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final LoadInvitePort loadInvitePort;
	private final SaveInvitePort saveInvitePort;
	private final Clock clock;

	@Override
	@Transactional
	public void revokeDevicesOf(long userId) {
		Instant now = clock.instant();
		for (Device device : loadDevicePort.loadLiveDevices(OwnerType.USER, userId)) {
			device.revoke(now);
			saveDevicePort.saveDevice(device);
		}
		revokeUnusedInvite(userId, now);
	}

	private void revokeUnusedInvite(long userId, Instant now) {
		loadInvitePort.loadLatestInvite(userId)
				.filter(invite -> !invite.isUsed() && !invite.isExpired(now))
				.ifPresent(invite -> {
					invite.revoke(now);
					saveInvitePort.saveInvite(invite);
				});
	}
}
