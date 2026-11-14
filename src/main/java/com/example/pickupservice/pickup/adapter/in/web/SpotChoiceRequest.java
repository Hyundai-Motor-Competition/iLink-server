package com.example.pickupservice.pickup.adapter.in.web;

interface SpotChoiceRequest {

	Long spotId();

	SpotCandidateRequest candidate();

	default Boolean here() {
		return null;
	}

}
