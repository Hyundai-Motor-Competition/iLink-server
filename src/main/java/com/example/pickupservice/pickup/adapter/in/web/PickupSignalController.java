package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.doc.PickupSignalApiDoc;
import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeUseCase;
import com.example.pickupservice.pickup.application.port.in.ReceiveTelemetryUseCase;
import com.example.pickupservice.pickup.application.port.in.ReceiveVehicleEventUseCase;
import com.example.pickupservice.pickup.application.port.in.ReportChildLocationUseCase;
import com.example.pickupservice.pickup.application.port.in.RequestHelpUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PickupSignalController implements PickupSignalApiDoc {

	private final ReceiveTelemetryUseCase receiveTelemetryUseCase;
	private final ReceiveVehicleEventUseCase receiveVehicleEventUseCase;
	private final ExtendPrepTimeUseCase extendPrepTimeUseCase;
	private final ReportChildLocationUseCase reportChildLocationUseCase;
	private final RequestHelpUseCase requestHelpUseCase;

	@Override
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/vehicle-telemetry")
	public ResponseEntity<Void> receiveTelemetry(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody VehicleTelemetryRequest request) {
		receiveTelemetryUseCase.receive(request.toCommand(principal, sessionId));
		return ResponseEntity.accepted().build();
	}

	@Override
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/vehicle-events")
	public ResponseEntity<Void> receiveVehicleEvent(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody VehicleEventRequest request) {
		receiveVehicleEventUseCase.receive(request.toCommand(principal, sessionId));
		return ResponseEntity.accepted().build();
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasRole('CHILD')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/prep-time-extensions")
	public ResponseEntity<PrepExtensionResponse> extendPrepTime(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody PrepTimeExtensionRequest request) {
		return ResponseEntity.ok(PrepExtensionResponse.from(
				extendPrepTimeUseCase.extend(request.toCommand(principal, sessionId))));
	}

	@Override
	@PreAuthorize("hasRole('CHILD')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/child-locations")
	public ResponseEntity<Void> reportChildLocation(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody ChildLocationRequest request) {
		reportChildLocationUseCase.report(request.toCommand(principal, sessionId));
		return ResponseEntity.accepted().build();
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasRole('CHILD')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/help-requests")
	public ResponseEntity<HelpRequestResponse> requestHelp(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody HelpRequestRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(HelpRequestResponse.from(
				requestHelpUseCase.request(request.toCommand(principal, sessionId))));
	}

}
