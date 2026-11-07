package com.example.pickupservice.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCallModeTest {

	@Test
	@DisplayName("JoinCallMode는 AUTO · GUARDIAN_CONFIRM 두 값을 갖는다")
	void joinCallModeHasExactlyTwoValuesInOrder() {
		assertThat(JoinCallMode.values()).containsExactly(
				JoinCallMode.AUTO,
				JoinCallMode.GUARDIAN_CONFIRM
		);
	}

	@Test
	@DisplayName("JoinCallMode 값 이름은 AUTO · GUARDIAN_CONFIRM 문자열과 같다")
	void joinCallModeNamesMatchDesignStrings() {
		assertThat(JoinCallMode.values())
				.extracting(JoinCallMode::name)
				.containsExactly("AUTO", "GUARDIAN_CONFIRM");
	}

	@Test
	@DisplayName("JoinCallMode 값 이름은 VARCHAR(20)에 들어가는 20자 이하다")
	void joinCallModeNamesFitInVarchar20() {
		assertThat(JoinCallMode.values())
				.extracting(JoinCallMode::name)
				.allSatisfy(name -> assertThat(name.length()).isLessThanOrEqualTo(20));
	}
}
