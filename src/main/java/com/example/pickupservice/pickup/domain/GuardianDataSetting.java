package com.example.pickupservice.pickup.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuardianDataSetting {

	private long userId;
	private boolean learningConsent;

	public static GuardianDataSetting defaultFor(long userId) {
		return new GuardianDataSetting(userId, false);
	}

	public static GuardianDataSetting restore(long userId, boolean learningConsent) {
		return new GuardianDataSetting(userId, learningConsent);
	}

	public void changeLearningConsent(boolean learningConsent) {
		this.learningConsent = learningConsent;
	}
}
