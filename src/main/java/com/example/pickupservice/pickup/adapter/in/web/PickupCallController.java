package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.doc.PickupCallApiDoc;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallCommand;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallUseCase;
import com.example.pickupservice.pickup.application.port.in.CallHoldCommand;
import com.example.pickupservice.pickup.application.port.in.HoldCallUseCase;
import com.example.pickupservice.pickup.application.port.in.RevokeCallUseCase;
import com.example.pickupservice.pickup.application.port.in.SendCallUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PickupCallController implements PickupCallApiDoc {

	private final RevokeCallUseCase revokeCallUseCase;
	private final AcknowledgeCallUseCase acknowledgeCallUseCase;
	private final SendCallUseCase sendCallUseCase;
	private final HoldCallUseCase holdCallUseCase;

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/calls")
	public ResponseEntity<SendCallResponse> send(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody SendCallRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(SendCallResponse.from(
				sendCallUseCase.send(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/calls/{callId}/revocation")
	public ResponseEntity<SessionViewResponse> revoke(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long callId,
			@Valid @RequestBody RevokeCallRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				revokeCallUseCase.revoke(request.toCommand(principal, callId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasRole('CHILD')")
	@PostMapping("/v1/calls/{callId}/acknowledgement")
	public ResponseEntity<AcknowledgeCallResponse> acknowledge(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long callId) {
		return ResponseEntity.ok(AcknowledgeCallResponse.from(
				acknowledgeCallUseCase.acknowledge(new AcknowledgeCallCommand(principal, callId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@PutMapping("/v1/pickup-sessions/{sessionId}/call-hold")
	public ResponseEntity<SessionViewResponse> holdCall(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody HoldCallRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				holdCallUseCase.holdCall(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@DeleteMapping("/v1/pickup-sessions/{sessionId}/call-hold")
	public ResponseEntity<SessionViewResponse> releaseCallHold(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@RequestParam int stateVersion) {
		return ResponseEntity.ok(SessionViewResponse.from(
				holdCallUseCase.releaseCallHold(new CallHoldCommand(principal, sessionId, stateVersion))));
	}

}
