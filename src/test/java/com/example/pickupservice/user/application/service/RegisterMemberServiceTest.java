package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterMemberServiceTest {

	private static final long FAMILY_ID = 12L;

	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private SaveUserPort saveUserPort;

	@InjectMocks
	private RegisterMemberService registerMemberService;

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CO_GUARDIAN", "DELEGATE", "TEACHER"})
	@DisplayName("멤버 등록은 자녀 외 멤버가 이미 5명이면 MEMBER_LIMIT_EXCEEDED로 거절한다")
	void rejectsNonChildMemberWhenFiveNonChildMembersAlreadyExist(Role role) {
		given(loadUserPort.countNonChildMembers(FAMILY_ID)).willReturn(5L);
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "이선생", "+821098765432");

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("MEMBER_LIMIT_EXCEEDED");
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 자녀 외 멤버가 4명이면 자녀 외 멤버를 등록하고 userId를 돌려준다")
	void registersNonChildMemberWhenFourNonChildMembersExist() {
		given(loadUserPort.countNonChildMembers(FAMILY_ID)).willReturn(4L);
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(140L, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.TEACHER, "이선생", "+821098765432");

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(140L);
		verify(saveUserPort).saveUser(any());
	}

	@Test
	@DisplayName("멤버 등록은 자녀 외 멤버가 5명이어도 CHILD는 등록한다")
	void registersChildEvenWhenFiveNonChildMembersAlreadyExist() {
		lenient().when(loadUserPort.countNonChildMembers(FAMILY_ID)).thenReturn(5L);
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(131L, FAMILY_ID, Role.CHILD, "민준", null));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "민준", null);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(131L);
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getUserId()).isNull();
		assertThat(savedUser.getValue().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.CHILD);
		assertThat(savedUser.getValue().getName()).isEqualTo("민준");
		assertThat(savedUser.getValue().getPhone()).isNull();
	}

	@Test
	@DisplayName("CO_GUARDIAN이 CO_GUARDIAN을 등록하려 하면 FORBIDDEN으로 거절한다")
	void rejectsCoGuardianRegisteringCoGuardian() {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(160L, FAMILY_ID, Role.CO_GUARDIAN, "박공동", "+821055556666"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.CO_GUARDIAN, Role.CO_GUARDIAN, "박공동", "+821055556666");

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.FORBIDDEN.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
		verify(saveUserPort, never()).saveUser(any());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"DELEGATE", "TEACHER", "CHILD"})
	@DisplayName("CO_GUARDIAN은 DELEGATE · TEACHER · CHILD를 등록할 수 있다")
	void coGuardianRegistersDelegateTeacherAndChild(Role role) {
		String phone = role == Role.CHILD ? null : "+821077778888";
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(150L, FAMILY_ID, role, "최멤버", phone));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.CO_GUARDIAN, role, "최멤버", phone);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(150L);
		verify(saveUserPort).saveUser(any());
	}

	@Test
	@DisplayName("GUARDIAN은 CO_GUARDIAN을 등록할 수 있다")
	void guardianRegistersCoGuardian() {
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(160L, FAMILY_ID, Role.CO_GUARDIAN, "박공동", "+821055556666"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CO_GUARDIAN, "박공동", "+821055556666");

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(160L);
		verify(saveUserPort).saveUser(any());
	}

	static Stream<Arguments> nonChildRolesWithoutPhone() {
		return Stream.of(Role.CO_GUARDIAN, Role.DELEGATE, Role.TEACHER)
				.flatMap(role -> Stream.of(Arguments.of(role, null), Arguments.of(role, "")));
	}

	@ParameterizedTest
	@MethodSource("nonChildRolesWithoutPhone")
	@DisplayName("CHILD가 아닌 멤버를 phone 없이 등록하려 하면 거절한다")
	void rejectsNonChildMemberWithoutPhone(Role role, String phone) {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(170L, FAMILY_ID, role, "정멤버", phone));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "정멤버", phone);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"CO_GUARDIAN", "DELEGATE", "TEACHER"})
	@DisplayName("CHILD가 아닌 멤버를 국제 형식이 아닌 phone으로 등록하려 하면 VALIDATION_FAILED로 거절한다")
	void rejectsNonChildMemberWithNonInternationalPhone(Role role) {
		lenient().when(saveUserPort.saveUser(any())).thenReturn(
				User.restore(171L, FAMILY_ID, role, "정멤버", "01012345678"));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, role, "정멤버", "01012345678");

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> registerMemberService.registerMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("CHILD는 phone 없이 등록된다")
	void registersChildWithoutPhone() {
		given(saveUserPort.saveUser(any())).willReturn(
				User.restore(132L, FAMILY_ID, Role.CHILD, "서윤", null));
		RegisterMemberCommand command = new RegisterMemberCommand(
				FAMILY_ID, Role.GUARDIAN, Role.CHILD, "서윤", null);

		long userId = registerMemberService.registerMember(command);

		assertThat(userId).isEqualTo(132L);
		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.CHILD);
		assertThat(savedUser.getValue().getName()).isEqualTo("서윤");
		assertThat(savedUser.getValue().getPhone()).isNull();
	}
}
