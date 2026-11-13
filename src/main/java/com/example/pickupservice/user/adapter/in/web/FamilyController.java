package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.web.ProfileImageParts;
import com.example.pickupservice.user.adapter.in.web.doc.FamilyApiDoc;
import com.example.pickupservice.user.application.port.in.ChangeCallModeUseCase;
import com.example.pickupservice.user.application.port.in.ChangeChildLocationCommand;
import com.example.pickupservice.user.application.port.in.ChangeChildLocationUseCase;
import com.example.pickupservice.user.application.port.in.ChildLocationResult;
import com.example.pickupservice.user.application.port.in.GetMyFamilyQuery;
import com.example.pickupservice.user.application.port.in.GetMyFamilyUseCase;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import com.example.pickupservice.user.application.port.in.RegisterMemberUseCase;
import com.example.pickupservice.user.application.port.in.RemoveMemberCommand;
import com.example.pickupservice.user.application.port.in.RemoveMemberUseCase;
import com.example.pickupservice.user.application.port.in.UpdateMemberUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class FamilyController implements FamilyApiDoc {

	private final GetMyFamilyUseCase getMyFamilyUseCase;
	private final RegisterMemberUseCase registerMemberUseCase;
	private final UpdateMemberUseCase updateMemberUseCase;
	private final RemoveMemberUseCase removeMemberUseCase;
	private final ChangeCallModeUseCase changeCallModeUseCase;
	private final ChangeChildLocationUseCase changeChildLocationUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@GetMapping("/v1/families/me")
	public FamilyResponse getMyFamily(@AuthenticationPrincipal AuthPrincipal principal) {
		return FamilyResponse.from(getMyFamilyUseCase.getMyFamily(new GetMyFamilyQuery(principal.familyId())));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PostMapping(path = "/v1/families/me/members", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterMemberResponse registerMember(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody RegisterMemberRequest request) {
		long userId = registerMemberUseCase.registerMember(request.toCommand(principal));
		return new RegisterMemberResponse(userId);
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PostMapping(path = "/v1/families/me/members", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterMemberResponse registerMemberWithProfileImage(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestPart("request") RegisterMemberRequest request,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
		RegisterMemberCommand command = request.toCommand(principal)
				.withProfileImage(ProfileImageParts.toProfileImage(profileImage));
		return new RegisterMemberResponse(registerMemberUseCase.registerMember(command));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PatchMapping("/v1/families/me/members/{userId}")
	public MemberResponse updateMember(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long userId,
			@Valid @RequestBody UpdateMemberRequest request) {
		return MemberResponse.from(updateMemberUseCase.updateMember(request.toCommand(principal, userId)));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasRole('GUARDIAN')")
	@DeleteMapping("/v1/families/me/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long userId) {
		removeMemberUseCase.removeMember(new RemoveMemberCommand(principal.familyId(), principal.id(), userId));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PutMapping("/v1/families/me/call-mode")
	public CallModeResponse changeCallMode(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody ChangeCallModeRequest request) {
		return CallModeResponse.from(changeCallModeUseCase.changeCallMode(request.toCommand(principal.familyId())));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PutMapping("/v1/families/me/child-location")
	public ChildLocationResponse changeChildLocation(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody ChangeChildLocationRequest request) {
		ChildLocationResult result = changeChildLocationUseCase.changeChildLocation(
				new ChangeChildLocationCommand(principal.familyId(), request.enabled()));
		return new ChildLocationResponse(result.childLocationEnabled(), result.settingsVersion());
	}

}
