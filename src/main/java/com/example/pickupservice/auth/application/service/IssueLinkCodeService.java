package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.IssueLinkCodeCommand;
import com.example.pickupservice.auth.application.port.in.IssueLinkCodeUseCase;
import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IssueLinkCodeService implements IssueLinkCodeUseCase {

	private static final String QR_PAYLOAD_PREFIX = "pickup://link?code=";

	private final LoadLinkCodePort loadLinkCodePort;
	private final SaveLinkCodePort saveLinkCodePort;
	private final LinkCodeGenerator linkCodeGenerator;
	private final LinkCodeIssueCounter linkCodeIssueCounter;
	private final LoadVehicleUseCase loadVehicleUseCase;
	private final Clock clock;

	@Override
	@Transactional
	public LinkCodeResult issueLinkCode(IssueLinkCodeCommand command) {
		long vehicleId = command.vehicleId();
		long deviceId = command.deviceId();
		boolean issuedBeforeLink = command.familyId() == null;
		verifyVehicleCanIssue(vehicleId, issuedBeforeLink);
		if (linkCodeIssueCounter.isLimitReached(deviceId)) {
			throw AuthErrors.linkCodeReissueLimit();
		}
		expireIssuedCodesOfDevice(vehicleId, deviceId);
		LinkCode saved = saveLinkCodePort.saveLinkCode(LinkCode.issue(vehicleId, deviceId, issuedBeforeLink,
				linkCodeGenerator.generate(), clock.instant()));
		linkCodeIssueCounter.increment(deviceId);
		return new LinkCodeResult(saved.getCodeId(), saved.getCode(), QR_PAYLOAD_PREFIX + saved.getCode(),
				saved.getExpiresAt(), linkCodeIssueCounter.remaining(deviceId));
	}

	private void verifyVehicleCanIssue(long vehicleId, boolean issuedBeforeLink) {
		Optional<VehicleInfo> vehicle = loadVehicleUseCase.findVehicle(vehicleId);
		if (vehicle.filter(info -> !info.linked()).isPresent()) {
			throw AuthErrors.vehicleUnlinked();
		}
		if (issuedBeforeLink && vehicle.map(VehicleInfo::familyId).isPresent()) {
			throw AuthErrors.vehicleAlreadyLinked();
		}
	}

	private void expireIssuedCodesOfDevice(long vehicleId, long deviceId) {
		for (LinkCode previous : loadLinkCodePort.loadIssuedCodes(vehicleId)) {
			Long issuedDeviceId = previous.getIssuedDeviceId();
			if (issuedDeviceId != null && issuedDeviceId != deviceId) {
				continue;
			}
			previous.expire();
			saveLinkCodePort.saveLinkCode(previous);
		}
	}
}
