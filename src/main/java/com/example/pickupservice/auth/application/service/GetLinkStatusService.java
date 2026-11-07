package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.GetLinkStatusUseCase;
import com.example.pickupservice.auth.application.port.in.LinkStatus;
import com.example.pickupservice.auth.application.port.in.LinkStatusResult;
import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class GetLinkStatusService implements GetLinkStatusUseCase {

	private static final int POLL_AFTER_SECONDS = 2;

	private final LoadLinkCodePort loadLinkCodePort;
	private final SaveLinkCodePort saveLinkCodePort;
	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final IssueTokenPort issueTokenPort;
	private final LoadVehicleUseCase loadVehicleUseCase;
	private final Clock clock;

	@Override
	@Transactional
	public LinkStatusResult getLinkStatus(long codeId, long requesterVehicleId) {
		LinkCode code = loadOwnedUndeliveredCode(codeId, requesterVehicleId);
		Instant now = clock.instant();
		if (code.getStatus() == LinkCodeStatus.USED) {
			return deliverTokens(code, now);
		}
		if (code.getStatus() == LinkCodeStatus.EXPIRED || code.isExpired(now)) {
			return new LinkStatusResult(LinkStatus.EXPIRED, null, null, null, null, null);
		}
		return new LinkStatusResult(LinkStatus.WAITING, null, null, null, null, POLL_AFTER_SECONDS);
	}

	private LinkCode loadOwnedUndeliveredCode(long codeId, long requesterVehicleId) {
		LinkCode code = loadLinkCodePort.loadLinkCode(codeId).orElseThrow(AuthErrors::linkCodeInvalid);
		if (code.getVehicleId() != requesterVehicleId) {
			throw AuthErrors.notCodeOwner();
		}
		if (code.getTokenDeliveredAt() != null) {
			throw AuthErrors.linkCodeInvalid();
		}
		return code;
	}

	private LinkStatusResult deliverTokens(LinkCode code, Instant now) {
		VehicleInfo vehicle = loadVehicleUseCase.findVehicle(code.getVehicleId())
				.orElseThrow(AuthErrors::linkCodeInvalid);
		Device device = latestLiveVehicleDevice(vehicle.vehicleId());
		IssuedTokens tokens = new DeviceTokenRotator(issueTokenPort, saveDevicePort).issueTo(device, new TokenSubject(
				PrincipalType.VEHICLE, vehicle.vehicleId(), vehicle.familyId(), null, device.getDeviceId()));
		code.markTokenDelivered(now);
		saveLinkCodePort.saveLinkCode(code);
		return new LinkStatusResult(linkedStatusOf(code), vehicle.vehicleId(), vehicle.familyId(),
				tokens.accessToken(), tokens.refreshToken(), null);
	}

	private Device latestLiveVehicleDevice(long vehicleId) {
		return loadDevicePort.loadLiveDevices(OwnerType.VEHICLE, vehicleId).stream()
				.max(Comparator.comparing(Device::getDeviceId))
				.orElseThrow(AuthErrors::linkCodeInvalid);
	}

	private static LinkStatus linkedStatusOf(LinkCode code) {
		return code.getMode() == LinkMode.RELINK ? LinkStatus.RELINKED : LinkStatus.LINKED;
	}
}
