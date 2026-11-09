package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.adapter.in.web.doc.PickupDataSettingsApiDoc;
import com.example.pickupservice.pickup.application.port.in.UpdateDataSettingsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PickupDataSettingsController implements PickupDataSettingsApiDoc {

	private final UpdateDataSettingsUseCase updateDataSettingsUseCase;

	@Override
	@PreAuthorize("hasRole('GUARDIAN')")
	@PutMapping("/v1/guardians/me/data-settings")
	public ResponseEntity<DataSettingsResponse> updateDataSettings(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody UpdateDataSettingsRequest request) {
		return ResponseEntity.ok(DataSettingsResponse.from(
				updateDataSettingsUseCase.updateDataSettings(request.toCommand(principal))));
	}

}
