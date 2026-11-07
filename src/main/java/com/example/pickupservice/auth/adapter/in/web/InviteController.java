package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.adapter.in.web.doc.InviteApiDoc;
import com.example.pickupservice.auth.application.port.in.AcceptInviteUseCase;
import com.example.pickupservice.auth.application.port.in.GetInviteUseCase;
import com.example.pickupservice.auth.application.port.in.IssueInviteUseCase;
import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InviteController implements InviteApiDoc {

	private final IssueInviteUseCase issueInviteUseCase;
	private final GetInviteUseCase getInviteUseCase;
	private final AcceptInviteUseCase acceptInviteUseCase;

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PostMapping("/v1/families/me/members/{userId}/invites")
	@ResponseStatus(HttpStatus.CREATED)
	public InviteResponse issueInvite(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long userId,
			@RequestBody(required = false) IssueInviteRequest request) {
		return InviteResponse.from(issueInviteUseCase.issueInvite(
				IssueInviteRequest.toCommand(request, principal.familyId(), userId)));
	}

	@Override
	@GetMapping("/v1/invites/{token}")
	public InviteInfoResponse getInvite(@PathVariable String token) {
		return InviteInfoResponse.from(getInviteUseCase.getInvite(token));
	}

	@Override
	@IdempotencyKeyRequired
	@PostMapping("/v1/invites/{token}/acceptance")
	@ResponseStatus(HttpStatus.CREATED)
	public AcceptInviteResponse acceptInvite(@PathVariable String token, @Valid @RequestBody AcceptInviteRequest request) {
		return AcceptInviteResponse.from(acceptInviteUseCase.acceptInvite(token, request.toCommand()));
	}

}
