package com.example.pickupservice.place.domain;

import java.util.ArrayList;
import java.util.List;

public final class SpotCautionRule {

	private SpotCautionRule() {
	}

	public static List<SpotCaution> cautionsOf(SpotCandidate candidate) {
		List<SpotCaution> cautions = new ArrayList<>();
		if (candidate.kind() == SpotKind.CURB_STOP) {
			cautions.add(SpotCaution.LINE_DATA_2021);
			if (candidate.lineKind() == LineKind.DOTTED_YELLOW) {
				cautions.add(SpotCaution.STOP_5MIN_ONLY);
			}
			if (candidate.nearEnforcementCctv()) {
				cautions.add(SpotCaution.ENFORCEMENT_CAMERA_NEAR);
			}
		}
		return cautions;
	}
}
