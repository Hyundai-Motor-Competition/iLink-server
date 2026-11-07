package com.example.pickupservice.user.domain;

import com.example.pickupservice.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

	@Test
	@DisplayName("User.create는 userId가 빈 User를 만들고 User.restore는 userId · familyId · role · name · phone을 그대로 가진 User를 만든다")
	void createLeavesUserIdEmptyAndRestoreKeepsAllFields() {
		User created = User.create(12L, Role.CO_GUARDIAN, "이보호", "+821098765432");

		assertThat(created.getUserId()).isNull();
		assertThat(created.getFamilyId()).isEqualTo(12L);
		assertThat(created.getRole()).isEqualTo(Role.CO_GUARDIAN);
		assertThat(created.getName()).isEqualTo("이보호");
		assertThat(created.getPhone()).isEqualTo("+821098765432");

		User restored = User.restore(101L, 12L, Role.GUARDIAN, "김보호", "+821012345678");

		assertThat(restored.getUserId()).isEqualTo(101L);
		assertThat(restored.getFamilyId()).isEqualTo(12L);
		assertThat(restored.getRole()).isEqualTo(Role.GUARDIAN);
		assertThat(restored.getName()).isEqualTo("김보호");
		assertThat(restored.getPhone()).isEqualTo("+821012345678");
	}

	@Test
	@DisplayName("User.create로 CHILD를 phone 없이 만들면 phone이 null이다")
	void createChildWithoutPhoneKeepsPhoneNull() {
		User child = User.create(12L, Role.CHILD, "민준", null);

		assertThat(child.getUserId()).isNull();
		assertThat(child.getRole()).isEqualTo(Role.CHILD);
		assertThat(child.getName()).isEqualTo("민준");
		assertThat(child.getPhone()).isNull();
	}

	@Test
	@DisplayName("User.restore는 phone이 null인 CHILD를 그대로 복원한다")
	void restoreChildWithNullPhone() {
		User child = User.restore(131L, 12L, Role.CHILD, "민준", null);

		assertThat(child.getUserId()).isEqualTo(131L);
		assertThat(child.getFamilyId()).isEqualTo(12L);
		assertThat(child.getRole()).isEqualTo(Role.CHILD);
		assertThat(child.getName()).isEqualTo("민준");
		assertThat(child.getPhone()).isNull();
	}

	@Test
	@DisplayName("User.changeName · changePhone · changeRole은 그 필드만 바꾸고 나머지는 그대로 둔다")
	void changeMethodsChangeOnlyTheirOwnField() {
		User user = User.restore(141L, 12L, Role.DELEGATE, "박대리", "+821011112222");

		user.changeName("박인계");

		assertThat(user.getName()).isEqualTo("박인계");
		assertThat(user.getUserId()).isEqualTo(141L);
		assertThat(user.getFamilyId()).isEqualTo(12L);
		assertThat(user.getRole()).isEqualTo(Role.DELEGATE);
		assertThat(user.getPhone()).isEqualTo("+821011112222");

		user.changePhone("+821033334444");

		assertThat(user.getPhone()).isEqualTo("+821033334444");
		assertThat(user.getUserId()).isEqualTo(141L);
		assertThat(user.getFamilyId()).isEqualTo(12L);
		assertThat(user.getRole()).isEqualTo(Role.DELEGATE);
		assertThat(user.getName()).isEqualTo("박인계");

		user.changeRole(Role.TEACHER);

		assertThat(user.getRole()).isEqualTo(Role.TEACHER);
		assertThat(user.getUserId()).isEqualTo(141L);
		assertThat(user.getFamilyId()).isEqualTo(12L);
		assertThat(user.getName()).isEqualTo("박인계");
		assertThat(user.getPhone()).isEqualTo("+821033334444");
	}

	@Test
	@DisplayName("User.changeName은 CHILD의 이름만 바꾸고 null인 phone과 role은 그대로 둔다")
	void changeNameOfChildKeepsNullPhoneAndRole() {
		User child = User.restore(131L, 12L, Role.CHILD, "민준", null);

		child.changeName("민준이");

		assertThat(child.getName()).isEqualTo("민준이");
		assertThat(child.getUserId()).isEqualTo(131L);
		assertThat(child.getFamilyId()).isEqualTo(12L);
		assertThat(child.getRole()).isEqualTo(Role.CHILD);
		assertThat(child.getPhone()).isNull();
	}

	@Test
	@DisplayName("User.changePhone은 phone이 없던 CHILD에게 번호를 채우고 나머지는 그대로 둔다")
	void changePhoneFillsPhoneOfChildWithoutPhone() {
		User child = User.restore(131L, 12L, Role.CHILD, "민준", null);

		child.changePhone("+821055556666");

		assertThat(child.getPhone()).isEqualTo("+821055556666");
		assertThat(child.getUserId()).isEqualTo(131L);
		assertThat(child.getFamilyId()).isEqualTo(12L);
		assertThat(child.getRole()).isEqualTo(Role.CHILD);
		assertThat(child.getName()).isEqualTo("민준");
	}

	@Test
	@DisplayName("User.create로 만든 User에 changeRole을 부르면 role만 바뀌고 userId는 빈 채로 남는다")
	void changeRoleOnCreatedUserKeepsUserIdEmpty() {
		User created = User.create(12L, Role.TEACHER, "최선생", "+821077778888");

		created.changeRole(Role.CO_GUARDIAN);

		assertThat(created.getRole()).isEqualTo(Role.CO_GUARDIAN);
		assertThat(created.getUserId()).isNull();
		assertThat(created.getFamilyId()).isEqualTo(12L);
		assertThat(created.getName()).isEqualTo("최선생");
		assertThat(created.getPhone()).isEqualTo("+821077778888");
	}
}
