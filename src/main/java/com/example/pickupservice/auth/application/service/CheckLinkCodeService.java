package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.CheckLinkCodeUseCase;
import com.example.pickupservice.auth.application.port.in.LinkCodeInfo;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import com.example.pickupservice.user.domain.PlateNumberMasker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class CheckLinkCodeService implements CheckLinkCodeUseCase {

	private final LoadLinkCodePort loadLinkCodePort;
	private final LoadVehicleUseCase loadVehicleUseCase;
	private final LinkCodeAttemptLimiter linkCodeAttemptLimiter;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public LinkCodeInfo checkLinkCode(String code, String clientKey) {
		if (linkCodeAttemptLimiter.isBlocked(clientKey)) {
			throw AuthErrors.linkCodeAttemptLimit();
		}
		LinkCode linkCode = loadUsableCode(code, clientKey);
		VehicleInfo vehicle = loadVehicleUseCase.findVehicle(linkCode.getVehicleId())
				.orElseThrow(AuthErrors::linkCodeInvalid);
		return new LinkCodeInfo(LinkCodeRules.actualModeOf(vehicle), vehicle.model(), PlateNumberMasker.mask(vehicle.plateNumber()),
				linkCode.getExpiresAt());
	}

	private LinkCode loadUsableCode(String code, String clientKey) {
		LinkCode linkCode = loadLinkCodePort.loadLinkCodeByCode(code).orElse(null);
		if (linkCode == null) {
			linkCodeAttemptLimiter.recordFailure(clientKey);
			throw AuthErrors.linkCodeInvalid();
		}
		if (!LinkCodeRules.isUsable(linkCode, clock.instant())) {
			linkCodeAttemptLimiter.recordFailure(clientKey);
			throw AuthErrors.linkCodeExpired();
		}
		return linkCode;
	}
}
