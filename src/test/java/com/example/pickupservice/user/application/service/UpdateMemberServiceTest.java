package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.in.UpdateMemberCommand;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateMemberServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long OTHER_FAMILY_ID = 99L;
	private static final long GUARDIAN_ID = 101L;
	private static final long CO_GUARDIAN_ID = 160L;
	private static final long DELEGATE_ID = 150L;
	private static final long CHILD_ID = 131L;

	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private SaveUserPort saveUserPort;

	@Mock
	private LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;

	@InjectMocks
	private UpdateMemberService updateMemberService;

	@Test
	@DisplayName("멤버 수정은 보낸 필드만 바꾸고 보내지 않은 필드는 그대로 둔다")
	void updatesOnlyNameWhenOnlyNameIsSent() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.of(
				User.restore(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "최대리", "+821077778888")));
		given(saveUserPort.saveUser(any())).willAnswer(returnsFirstArg());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.willReturn(Map.of(DELEGATE_ID, DeviceStatus.CONNECTED));
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_ID, Role.GUARDIAN, DELEGATE_ID, "최대리인", null, null);

		MemberResult result = updateMemberService.updateMember(command);

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getUserId()).isEqualTo(DELEGATE_ID);
		assertThat(savedUser.getValue().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(savedUser.getValue().getName()).isEqualTo("최대리인");
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.DELEGATE);
		assertThat(savedUser.getValue().getPhone()).isEqualTo("+821077778888");
		assertThat(result).isEqualTo(new MemberResult(
				DELEGATE_ID, Role.DELEGATE, "최대리인", "+821077778888", DeviceStatus.CONNECTED));
	}

	@Test
	@DisplayName("멤버 수정은 phone만 보내면 phone만 바꾸고 deviceStatus가 없으면 NONE으로 돌려준다")
	void updatesOnlyPhoneWhenOnlyPhoneIsSent() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.of(
				User.restore(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "최대리", "+821077778888")));
		given(saveUserPort.saveUser(any())).willAnswer(returnsFirstArg());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).willReturn(Map.of());
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_ID, Role.GUARDIAN, DELEGATE_ID, null, "+821011112222", null);

		MemberResult result = updateMemberService.updateMember(command);

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getName()).isEqualTo("최대리");
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.DELEGATE);
		assertThat(savedUser.getValue().getPhone()).isEqualTo("+821011112222");
		assertThat(result).isEqualTo(new MemberResult(
				DELEGATE_ID, Role.DELEGATE, "최대리", "+821011112222", DeviceStatus.NONE));
	}

	@Test
	@DisplayName("CO_GUARDIAN이 멤버의 role을 바꾸려 하면 FORBIDDEN으로 거절한다")
	void rejectsCoGuardianChangingRole() {
		lenient().when(loadUserPort.loadUser(DELEGATE_ID)).thenReturn(Optional.of(
				User.restore(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "최대리", "+821077778888")));
		lenient().when(saveUserPort.saveUser(any())).thenAnswer(returnsFirstArg());
		lenient().when(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).thenReturn(Map.of());
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, CO_GUARDIAN_ID, Role.CO_GUARDIAN, DELEGATE_ID, null, null, Role.TEACHER);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updateMemberService.updateMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.FORBIDDEN.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("CO_GUARDIAN은 멤버의 name을 바꿀 수 있다")
	void coGuardianChangesName() {
		given(loadUserPort.loadUser(CHILD_ID)).willReturn(Optional.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));
		given(saveUserPort.saveUser(any())).willAnswer(returnsFirstArg());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.willReturn(Map.of(CHILD_ID, DeviceStatus.INVITE_PENDING));
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, CO_GUARDIAN_ID, Role.CO_GUARDIAN, CHILD_ID, "김민준", null, null);

		MemberResult result = updateMemberService.updateMember(command);

		assertThat(result).isEqualTo(new MemberResult(
				CHILD_ID, Role.CHILD, "김민준", null, DeviceStatus.INVITE_PENDING));
		verify(saveUserPort).saveUser(any());
	}

	@Test
	@DisplayName("GUARDIAN은 다른 멤버의 role을 바꿀 수 있다")
	void guardianChangesOtherMemberRole() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.of(
				User.restore(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "최대리", "+821077778888")));
		given(saveUserPort.saveUser(any())).willAnswer(returnsFirstArg());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).willReturn(Map.of());
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_ID, Role.GUARDIAN, DELEGATE_ID, null, null, Role.TEACHER);

		MemberResult result = updateMemberService.updateMember(command);

		ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
		verify(saveUserPort).saveUser(savedUser.capture());
		assertThat(savedUser.getValue().getRole()).isEqualTo(Role.TEACHER);
		assertThat(savedUser.getValue().getName()).isEqualTo("최대리");
		assertThat(savedUser.getValue().getPhone()).isEqualTo("+821077778888");
		assertThat(result.role()).isEqualTo(Role.TEACHER);
	}

	@Test
	@DisplayName("GUARDIAN이 자기 자신의 role을 바꾸려 하면 거절한다")
	void rejectsGuardianChangingOwnRole() {
		lenient().when(loadUserPort.loadUser(GUARDIAN_ID)).thenReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));
		lenient().when(saveUserPort.saveUser(any())).thenAnswer(returnsFirstArg());
		lenient().when(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).thenReturn(Map.of());
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_ID, Role.GUARDIAN, GUARDIAN_ID, null, null, Role.CO_GUARDIAN);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updateMemberService.updateMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("다른 가족의 userId를 수정하려 하면 NOT_FOUND로 거절한다")
	void rejectsUpdatingMemberOfOtherFamily() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.of(
				User.restore(DELEGATE_ID, OTHER_FAMILY_ID, Role.DELEGATE, "남대리", "+821033334444")));
		lenient().when(saveUserPort.saveUser(any())).thenAnswer(returnsFirstArg());
		lenient().when(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).thenReturn(Map.of());
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_ID, Role.GUARDIAN, DELEGATE_ID, "바꾼이름", null, null);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updateMemberService.updateMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveUserPort, never()).saveUser(any());
	}

	@Test
	@DisplayName("없는 userId를 수정하려 하면 NOT_FOUND로 거절한다")
	void rejectsUpdatingMissingMember() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.empty());
		lenient().when(saveUserPort.saveUser(any())).thenAnswer(returnsFirstArg());
		lenient().when(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).thenReturn(Map.of());
		UpdateMemberCommand command = new UpdateMemberCommand(
				FAMILY_ID, GUARDIAN_ID, Role.GUARDIAN, DELEGATE_ID, "바꾼이름", null, null);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> updateMemberService.updateMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveUserPort, never()).saveUser(any());
	}
}
