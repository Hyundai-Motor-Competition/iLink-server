package com.example.pickupservice.pickup.adapter.in.web;

// spotId와 candidate 중 하나를 고르는 요청(@ValidSpotChoice 대상). here를 받는 요청은 here true 혼자도 허용한다
interface SpotChoiceRequest {

	Long spotId();

	SpotCandidateRequest candidate();

	default Boolean here() {
		return null;
	}

}
