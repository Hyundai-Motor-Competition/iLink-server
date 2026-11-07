package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.ConsentAgreement;
import com.example.pickupservice.auth.domain.ConsentType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class ConsentRules {

	private ConsentRules() {
	}

	static void requireAgreed(Collection<ConsentType> required, List<ConsentAgreement> consents) {
		Set<ConsentType> agreed = consents.stream()
				.filter(ConsentAgreement::agreed)
				.map(ConsentAgreement::type)
				.collect(Collectors.toSet());
		if (!agreed.containsAll(required)) {
			throw AuthErrors.consentRequired();
		}
	}
}
