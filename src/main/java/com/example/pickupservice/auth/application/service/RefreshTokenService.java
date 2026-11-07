package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.RefreshTokenUseCase;
import com.example.pickupservice.auth.application.port.in.TokenResult;
import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final IssueTokenPort issueTokenPort;
	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadVehicleUseCase loadVehicleUseCase;

	@Override
	@Transactional
	public TokenResult refresh(String refreshToken) {
		Device device = loadDevicePort.loadByRefreshTokenHash(issueTokenPort.hashRefreshToken(refreshToken))
				.filter(Device::isLive)
				.orElseThrow(AuthErrors::refreshTokenInvalid);
		IssuedTokens tokens = new DeviceTokenRotator(issueTokenPort, saveDevicePort).issueTo(device, subjectOf(device));
		return new TokenResult(tokens.accessToken(), tokens.refreshToken(), tokens.accessTokenExpiresAt());
	}

	private TokenSubject subjectOf(Device device) {
		if (device.getOwnerType() == OwnerType.VEHICLE) {
			VehicleInfo vehicle = loadVehicleUseCase.findVehicle(device.getOwnerId())
					.orElseThrow(AuthErrors::refreshTokenInvalid);
			return new TokenSubject(PrincipalType.VEHICLE, vehicle.vehicleId(), vehicle.familyId(), null,
					device.getDeviceId());
		}
		MemberInfo member = loadMemberUseCase.findMember(device.getOwnerId())
				.orElseThrow(AuthErrors::refreshTokenInvalid);
		return new TokenSubject(PrincipalType.USER, member.userId(), member.familyId(), member.role(),
				device.getDeviceId());
	}
}
