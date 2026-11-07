package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;

import java.util.List;

public record AcademySearchResponse(
		List<AcademyCandidateResponse> items
) {

	public static AcademySearchResponse from(List<AcademyCandidate> candidates) {
		return new AcademySearchResponse(candidates.stream().map(AcademyCandidateResponse::from).toList());
	}
}
