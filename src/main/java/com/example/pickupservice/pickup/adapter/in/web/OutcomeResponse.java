package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.pickup.application.port.in.CorrectionView;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;

import static com.example.pickupservice.pickup.adapter.in.web.KstTime.kst;

public record OutcomeResponse(
		long sessionId,
		String completedAt,
		String spotName,
		String boardingMethod,
		String predictedStopAt,
		String actualStopAt,
		Long diffSeconds,
		CorrectionResponse correction
) {

	public static OutcomeResponse from(OutcomeView view) {
		return new OutcomeResponse(
				view.sessionId(),
				kst(view.completedAt()),
				view.spotName(),
				view.boardingMethod(),
				kst(view.predictedStopAt()),
				kst(view.actualStopAt()),
				view.diffSeconds(),
				view.correction() == null ? null : CorrectionResponse.from(view.correction()));
	}

	public record CorrectionResponse(String status, String correctedStopAt) {

		static CorrectionResponse from(CorrectionView correction) {
			return new CorrectionResponse(correction.status(), kst(correction.correctedStopAt()));
		}
	}

}
