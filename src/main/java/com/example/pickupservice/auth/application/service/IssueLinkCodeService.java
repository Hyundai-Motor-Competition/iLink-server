package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.IssueLinkCodeCommand;
import com.example.pickupservice.auth.application.port.in.IssueLinkCodeUseCase;
import com.example.pickupservice.auth.application.port.in.LinkCodeResult;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.LinkCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class IssueLinkCodeService implements IssueLinkCodeUseCase {

	private static final String QR_PAYLOAD_PREFIX = "pickup://link?code=";

	private final LoadLinkCodePort loadLinkCodePort;
	private final SaveLinkCodePort saveLinkCodePort;
	private final LinkCodeGenerator linkCodeGenerator;
	private final LinkCodeIssueCounter linkCodeIssueCounter;
	private final Clock clock;

	@Override
	@Transactional
	public LinkCodeResult issueLinkCode(IssueLinkCodeCommand command) {
		long vehicleId = command.vehicleId();
		if (linkCodeIssueCounter.isLimitReached(vehicleId)) {
			throw AuthErrors.linkCodeReissueLimit();
		}
		expireIssuedCodes(vehicleId);
		LinkCode saved = saveLinkCodePort.saveLinkCode(
				LinkCode.issue(vehicleId, linkCodeGenerator.generate(), clock.instant()));
		linkCodeIssueCounter.increment(vehicleId);
		return new LinkCodeResult(saved.getCodeId(), saved.getCode(), QR_PAYLOAD_PREFIX + saved.getCode(),
				saved.getExpiresAt(), linkCodeIssueCounter.remaining(vehicleId));
	}

	private void expireIssuedCodes(long vehicleId) {
		for (LinkCode previous : loadLinkCodePort.loadIssuedCodes(vehicleId)) {
			previous.expire();
			saveLinkCodePort.saveLinkCode(previous);
		}
	}
}
