package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.ConsentAgreement;
import com.example.pickupservice.auth.domain.ConsentType;
import jakarta.validation.constraints.NotNull;

public record ConsentRequest(
		@NotNull ConsentType type,
		boolean agreed
) {

	public ConsentAgreement toAgreement() {
		return new ConsentAgreement(type, agreed);
	}

}
