package com.example.pickupservice.schedule.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class JoinCallModeTest {

	@Test
	@DisplayName("schedule JoinCallMode는 user JoinCallMode와 같은 값 이름(AUTO · GUARDIAN_CONFIRM)을 같은 순서로 갖는다")
	void hasSameValueNamesAsUserJoinCallMode() {
		String[] userNames = Arrays.stream(com.example.pickupservice.user.domain.JoinCallMode.values())
				.map(Enum::name)
				.toArray(String[]::new);

		assertThat(JoinCallMode.values())
				.extracting(Enum::name)
				.containsExactly(userNames)
				.containsExactly("AUTO", "GUARDIAN_CONFIRM");
	}
}
