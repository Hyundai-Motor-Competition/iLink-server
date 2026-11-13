package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.adapter.in.web.doc.VehicleLinkCodeApiDoc;
import com.example.pickupservice.auth.application.port.in.ApproveLinkCommand;
import com.example.pickupservice.auth.application.port.in.ApproveLinkUseCase;
import com.example.pickupservice.auth.application.port.in.CheckLinkCodeUseCase;
import com.example.pickupservice.auth.application.port.in.GetLinkStatusUseCase;
import com.example.pickupservice.auth.application.port.in.IssueLinkCodeCommand;
import com.example.pickupservice.auth.application.port.in.IssueLinkCodeUseCase;
import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.logging.PollingLog;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.web.ProfileImageParts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class VehicleLinkCodeController implements VehicleLinkCodeApiDoc {

	private final IssueLinkCodeUseCase issueLinkCodeUseCase;
	private final GetLinkStatusUseCase getLinkStatusUseCase;
	private final CheckLinkCodeUseCase checkLinkCodeUseCase;
	private final ApproveLinkUseCase approveLinkUseCase;
	private final LinkStatusPollGuard linkStatusPollGuard;

	@Override
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/vehicle-link-codes")
	@ResponseStatus(HttpStatus.CREATED)
	public LinkCodeResponse issueLinkCode(@AuthenticationPrincipal AuthPrincipal principal) {
		IssueLinkCodeCommand command = new IssueLinkCodeCommand(CurrentDevice.id(), principal.id(), principal.familyId());
		return LinkCodeResponse.from(issueLinkCodeUseCase.issueLinkCode(command));
	}

	@Override
	@PollingLog(stateField = "status")
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping("/v1/vehicle-link-codes/{codeId}/status")
	public LinkStatusResponse getLinkStatus(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long codeId) {
		long deviceId = CurrentDevice.id();
		linkStatusPollGuard.check(deviceId);
		return LinkStatusResponse.from(getLinkStatusUseCase.getLinkStatus(codeId, principal.id(), deviceId));
	}

	@Override
	@GetMapping("/v1/vehicle-link-codes/by-code/{code}")
	public LinkCodeInfoResponse checkLinkCode(@PathVariable String code, HttpServletRequest request) {
		return LinkCodeInfoResponse.from(checkLinkCodeUseCase.checkLinkCode(code, request.getRemoteAddr()));
	}

	@Override
	@IdempotencyKeyRequired
	@PostMapping(path = "/v1/vehicle-links", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public VehicleLinkResponse approveLink(@Valid @RequestBody ApproveLinkRequest request) {
		return VehicleLinkResponse.from(approveLinkUseCase.approveLink(request.toCommand()));
	}

	@Override
	@IdempotencyKeyRequired
	@PostMapping(path = "/v1/vehicle-links", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public VehicleLinkResponse approveLinkWithProfileImage(
			@Valid @RequestPart("request") ApproveLinkRequest request,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
		ApproveLinkCommand command = request.toCommand()
				.withProfileImage(ProfileImageParts.toProfileImage(profileImage));
		return VehicleLinkResponse.from(approveLinkUseCase.approveLink(command));
	}

}
