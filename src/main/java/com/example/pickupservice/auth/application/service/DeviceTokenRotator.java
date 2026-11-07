package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Device;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class DeviceTokenRotator {

	private final IssueTokenPort issueTokenPort;
	private final SaveDevicePort saveDevicePort;

	IssuedTokens issueTo(Device device, TokenSubject subject) {
		IssuedTokens tokens = issueTokenPort.issueTokens(subject);
		device.rotateRefreshToken(tokens.refreshTokenHash());
		saveDevicePort.saveDevice(device);
		return tokens;
	}
}
