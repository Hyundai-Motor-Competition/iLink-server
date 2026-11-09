package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.doc.PickupSessionApiDoc;
import com.example.pickupservice.pickup.application.port.in.CancelSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotUseCase;
import com.example.pickupservice.pickup.application.port.in.CompletePickupUseCase;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteUseCase;
import com.example.pickupservice.pickup.application.port.in.FindActiveSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.GetContactsUseCase;
import com.example.pickupservice.pickup.application.port.in.GetSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.HoldCommand;
import com.example.pickupservice.pickup.application.port.in.HoldSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.ListSessionEventsUseCase;
import com.example.pickupservice.pickup.application.port.in.OpenIncidentUseCase;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.in.StartPickupResult;
import com.example.pickupservice.pickup.application.port.in.StartPickupUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PickupSessionController implements PickupSessionApiDoc {

	private final FindActiveSessionUseCase findActiveSessionUseCase;
	private final StartPickupUseCase startPickupUseCase;
	private final GetSessionUseCase getSessionUseCase;
	private final ConfirmRouteUseCase confirmRouteUseCase;
	private final ChangeSpotUseCase changeSpotUseCase;
	private final HoldSessionUseCase holdSessionUseCase;
	private final CancelSessionUseCase cancelSessionUseCase;
	private final ListSessionEventsUseCase listSessionEventsUseCase;
	private final GetContactsUseCase getContactsUseCase;
	private final CompletePickupUseCase completePickupUseCase;
	private final RejectBoardingUseCase rejectBoardingUseCase;
	private final OpenIncidentUseCase openIncidentUseCase;

	@Override
	@GetMapping("/v1/pickup-sessions/active")
	public ResponseEntity<ActiveSessionResponse> findActive(@AuthenticationPrincipal AuthPrincipal principal) {
		return findActiveSessionUseCase.findActive(principal)
				.map(ActiveSessionResponse::from)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions")
	public ResponseEntity<SessionViewResponse> start(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody StartPickupRequest request) {
		StartPickupResult result = startPickupUseCase.start(request.toCommand(principal));
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(SessionViewResponse.from(result.session()));
	}

	@Override
	@GetMapping("/v1/pickup-sessions/{sessionId}")
	public ResponseEntity<SessionViewResponse> getSession(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId) {
		return ResponseEntity.ok(SessionViewResponse.from(
				getSessionUseCase.getSession(new SessionQuery(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PutMapping("/v1/pickup-sessions/{sessionId}/route")
	public ResponseEntity<SessionViewResponse> confirmRoute(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody ConfirmRouteRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				confirmRouteUseCase.confirmRoute(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN') or hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/spot-changes")
	public ResponseEntity<SessionViewResponse> changeSpot(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody ChangeSpotRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				changeSpotUseCase.changeSpot(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@PutMapping("/v1/pickup-sessions/{sessionId}/hold")
	public ResponseEntity<SessionViewResponse> hold(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody HoldSessionRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				holdSessionUseCase.hold(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@DeleteMapping("/v1/pickup-sessions/{sessionId}/hold")
	public ResponseEntity<SessionViewResponse> releaseHold(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@RequestParam int stateVersion) {
		return ResponseEntity.ok(SessionViewResponse.from(
				holdSessionUseCase.releaseHold(new HoldCommand(principal, sessionId, stateVersion, null))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN') or hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/cancellation")
	public ResponseEntity<SessionViewResponse> cancel(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody CancelSessionRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				cancelSessionUseCase.cancel(request.toCommand(principal, sessionId))));
	}

	@Override
	@GetMapping("/v1/pickup-sessions/{sessionId}/events")
	public ResponseEntity<SessionEventPageResponse> listEvents(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@RequestParam long afterSeq,
			@RequestParam(defaultValue = "100") @Min(1) @Max(100) int limit) {
		return ResponseEntity.ok(SessionEventPageResponse.from(
				listSessionEventsUseCase.listEvents(sessionId, afterSeq, limit, principal)));
	}

	@Override
	@GetMapping("/v1/pickup-sessions/{sessionId}/contacts")
	public ResponseEntity<ContactsResponse> getContacts(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId) {
		return ResponseEntity.ok(ContactsResponse.from(
				getContactsUseCase.getContacts(new SessionQuery(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/completion")
	public ResponseEntity<SessionViewResponse> complete(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody CompletePickupRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				completePickupUseCase.complete(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PostMapping("/v1/pickup-sessions/{sessionId}/boarding-rejection")
	public ResponseEntity<SessionViewResponse> rejectBoarding(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody RejectBoardingRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				rejectBoardingUseCase.rejectBoarding(request.toCommand(principal, sessionId))));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAuthority('PRINCIPAL_VEHICLE')")
	@PutMapping("/v1/pickup-sessions/{sessionId}/incident")
	public ResponseEntity<SessionViewResponse> openIncident(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId,
			@Valid @RequestBody OpenIncidentRequest request) {
		return ResponseEntity.ok(SessionViewResponse.from(
				openIncidentUseCase.openIncident(request.toCommand(principal, sessionId))));
	}

}
