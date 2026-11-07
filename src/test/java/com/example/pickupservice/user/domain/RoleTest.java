package com.example.pickupservice.user.domain;

import com.example.pickupservice.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

	@Test
	@DisplayName("Role은 GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD 다섯 값을 갖는다")
	void roleHasExactlyFiveValuesInOrder() {
		assertThat(Role.values()).containsExactly(
				Role.GUARDIAN,
				Role.CO_GUARDIAN,
				Role.DELEGATE,
				Role.TEACHER,
				Role.CHILD
		);
	}

	@Test
	@DisplayName("Role 값 이름은 GUARDIAN · CO_GUARDIAN · DELEGATE · TEACHER · CHILD 문자열과 같다")
	void roleNamesMatchDesignStrings() {
		assertThat(Role.values())
				.extracting(Role::name)
				.containsExactly("GUARDIAN", "CO_GUARDIAN", "DELEGATE", "TEACHER", "CHILD");
	}
}
