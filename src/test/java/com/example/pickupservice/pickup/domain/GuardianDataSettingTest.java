package com.example.pickupservice.pickup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianDataSettingTest {

	private static final long GUARDIAN_USER_ID = 11L;

	@Test
	@DisplayName("행이 없는 보호자의 GuardianDataSetting은 learningConsent false로 만들어진다")
	void defaultForCreatesSettingWithLearningConsentFalse() {
		GuardianDataSetting setting = GuardianDataSetting.defaultFor(GUARDIAN_USER_ID);

		assertThat(setting.getUserId()).isEqualTo(GUARDIAN_USER_ID);
		assertThat(setting.isLearningConsent()).isFalse();
	}

	@Test
	@DisplayName("GuardianDataSetting.changeLearningConsent는 동의 값을 true로 바꾸고 다시 false로 바꿀 수 있다")
	void changeLearningConsentTogglesConsent() {
		GuardianDataSetting setting = GuardianDataSetting.defaultFor(GUARDIAN_USER_ID);

		setting.changeLearningConsent(true);
		assertThat(setting.isLearningConsent()).isTrue();

		setting.changeLearningConsent(false);
		assertThat(setting.isLearningConsent()).isFalse();
	}

	@Test
	@DisplayName("GuardianDataSetting.restore는 저장된 userId · learningConsent를 그대로 되살린다")
	void restoreKeepsUserIdAndLearningConsent() {
		GuardianDataSetting setting = GuardianDataSetting.restore(GUARDIAN_USER_ID, true);

		assertThat(setting.getUserId()).isEqualTo(GUARDIAN_USER_ID);
		assertThat(setting.isLearningConsent()).isTrue();
	}
}
