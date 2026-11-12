package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.place.adapter.in.web.doc.PickupSpotApiDoc;
import com.example.pickupservice.place.application.port.in.AddPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.DeletePickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.DeleteSpotCommand;
import com.example.pickupservice.place.application.port.in.ListPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.ListSpotsQuery;
import com.example.pickupservice.place.application.port.in.UpdatePickupSpotUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PickupSpotController implements PickupSpotApiDoc {

	private static final int MIN_RADIUS_M = 50;
	private static final int MAX_RADIUS_M = 3000;

	private final ListPickupSpotsUseCase listPickupSpotsUseCase;
	private final AddPickupSpotUseCase addPickupSpotUseCase;
	private final UpdatePickupSpotUseCase updatePickupSpotUseCase;
	private final DeletePickupSpotUseCase deletePickupSpotUseCase;

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE') or hasAuthority('PRINCIPAL_VEHICLE')")
	@GetMapping("/v1/families/me/pickup-spots")
	public PickupSpotsResponse listSpots(
			@AuthenticationPrincipal AuthPrincipal principal,
			@RequestParam(required = false) Long academyId,
			@RequestParam(required = false) String near,
			@RequestParam(required = false) Integer radiusM) {
		LatLng nearPoint = validateNearAndRadius(near, radiusM);
		return PickupSpotsResponse.from(listPickupSpotsUseCase.listSpots(
				new ListSpotsQuery(principal.familyId(), academyId, nearPoint, radiusM)));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PostMapping("/v1/families/me/pickup-spots")
	@ResponseStatus(HttpStatus.CREATED)
	public AddSpotResponse addSpot(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody AddSpotRequest request) {
		return AddSpotResponse.from(addPickupSpotUseCase.addSpot(
				request.toCommand(principal.familyId(), principal.id())));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@PatchMapping("/v1/pickup-spots/{spotId}")
	public SpotResponse updateSpot(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long spotId,
			@Valid @RequestBody UpdateSpotRequest request) {
		return SpotResponse.from(updatePickupSpotUseCase.updateSpot(
				request.toCommand(principal.familyId(), spotId)));
	}

	@Override
	@IdempotencyKeyRequired
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN')")
	@DeleteMapping("/v1/pickup-spots/{spotId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSpot(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long spotId) {
		deletePickupSpotUseCase.deleteSpot(new DeleteSpotCommand(principal.familyId(), spotId));
	}

	private static LatLng validateNearAndRadius(String near, Integer radiusM) {
		if (near == null && radiusM == null) {
			return null;
		}
		if (near == null) {
			throw BusinessException.invalidField("near", "radiusM과 함께 보내야 합니다");
		}
		if (radiusM == null) {
			throw BusinessException.invalidField("radiusM", "near와 함께 보내야 합니다");
		}
		if (radiusM < MIN_RADIUS_M || radiusM > MAX_RADIUS_M) {
			throw BusinessException.invalidField("radiusM", "50~3000 사이여야 합니다");
		}
		return LatLngParam.parse("near", near);
	}

}
