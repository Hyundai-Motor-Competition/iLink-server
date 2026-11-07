package com.example.pickupservice.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FamilyTest {

	@Test
	@DisplayName("새로 만든 Family의 joinCallMode는 AUTO이고 settingsVersion은 0이다")
	void newFamilyStartsWithAutoCallModeAndSettingsVersionZero() {
		Family family = Family.create();

		assertThat(family.getJoinCallMode()).isEqualTo(JoinCallMode.AUTO);
		assertThat(family.getSettingsVersion()).isZero();
	}

	@Test
	@DisplayName("새로 만든 Family는 저장 전이라 familyId가 null이다")
	void newFamilyHasNoFamilyIdBeforeSave() {
		Family family = Family.create();

		assertThat(family.getFamilyId()).isNull();
	}

	@Test
	@DisplayName("Family.changeCallMode를 부르면 joinCallMode가 바뀌고 settingsVersion이 1 오른다")
	void changeCallModeUpdatesModeAndIncrementsSettingsVersion() {
		Family family = Family.create();

		family.changeCallMode(JoinCallMode.GUARDIAN_CONFIRM);

		assertThat(family.getJoinCallMode()).isEqualTo(JoinCallMode.GUARDIAN_CONFIRM);
		assertThat(family.getSettingsVersion()).isEqualTo(1);
	}

	@Test
	@DisplayName("Family.changeCallMode를 두 번 부르면 settingsVersion이 2가 되고 마지막 joinCallMode가 남는다")
	void changeCallModeTwiceIncrementsSettingsVersionEachTime() {
		Family family = Family.create();

		family.changeCallMode(JoinCallMode.GUARDIAN_CONFIRM);
		family.changeCallMode(JoinCallMode.AUTO);

		assertThat(family.getJoinCallMode()).isEqualTo(JoinCallMode.AUTO);
		assertThat(family.getSettingsVersion()).isEqualTo(2);
	}

	@Test
	@DisplayName("Family.restore는 familyId · joinCallMode · settingsVersion을 그대로 가진 Family를 만든다")
	void restoreKeepsFamilyIdCallModeAndSettingsVersion() {
		Family family = Family.restore(12L, JoinCallMode.GUARDIAN_CONFIRM, 7);

		assertThat(family.getFamilyId()).isEqualTo(12L);
		assertThat(family.getJoinCallMode()).isEqualTo(JoinCallMode.GUARDIAN_CONFIRM);
		assertThat(family.getSettingsVersion()).isEqualTo(7);
	}
}
