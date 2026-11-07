package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;

public record AcademyCandidateResponse(
		String externalPlaceId,
		String name,
		String address,
		LatLng location
) {

	public static AcademyCandidateResponse from(AcademyCandidate candidate) {
		return new AcademyCandidateResponse(
				candidate.externalPlaceId(), candidate.name(), candidate.address(), candidate.location());
	}
}
