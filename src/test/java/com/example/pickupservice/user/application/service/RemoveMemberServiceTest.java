package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RemoveMemberCommand;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.RevokeMemberDevicesPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveMemberServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long OTHER_FAMILY_ID = 99L;
	private static final long GUARDIAN_ID = 101L;
	private static final long OTHER_GUARDIAN_ID = 102L;
	private static final long CHILD_ID = 131L;
	private static final long TEACHER_ID = 170L;
	private static final long DELEGATE_ID = 150L;

	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private SaveUserPort saveUserPort;

	@Mock
	private CheckActivePickupPort checkActivePickupPort;

	@Mock
	private RevokeMemberDevicesPort revokeMemberDevicesPort;

	@InjectMocks
	private RemoveMemberService removeMemberService;

	@Test
	@DisplayName("멤버 해제는 GUARDIAN 자신을 대상으로 하면 CANNOT_REMOVE_GUARDIAN으로 거절한다")
	void rejectsRemovingGuardianSelf() {
		lenient().when(loadUserPort.loadUser(GUARDIAN_ID)).thenReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));
		lenient().when(checkActivePickupPort.isInActivePickup(anyLong())).thenReturn(false);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, GUARDIAN_ID);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> removeMemberService.removeMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("CANNOT_REMOVE_GUARDIAN");
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).deleteUser(anyLong());
		verify(revokeMemberDevicesPort, never()).revokeDevicesOf(anyLong());
	}

	@Test
	@DisplayName("멤버 해제는 대상의 role이 GUARDIAN이면 요청자가 달라도 CANNOT_REMOVE_GUARDIAN으로 거절한다")
	void rejectsRemovingMemberWhoseRoleIsGuardian() {
		lenient().when(loadUserPort.loadUser(OTHER_GUARDIAN_ID)).thenReturn(Optional.of(
				User.restore(OTHER_GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "이보호", "+821022223333")));
		lenient().when(checkActivePickupPort.isInActivePickup(anyLong())).thenReturn(false);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, OTHER_GUARDIAN_ID);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> removeMemberService.removeMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("CANNOT_REMOVE_GUARDIAN");
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(saveUserPort, never()).deleteUser(anyLong());
		verify(revokeMemberDevicesPort, never()).revokeDevicesOf(anyLong());
	}

	@Test
	@DisplayName("멤버 해제는 진행 중 픽업에 묶인 멤버면 MEMBER_IN_ACTIVE_PICKUP으로 거절한다")
	void rejectsRemovingMemberInActivePickup() {
		lenient().when(loadUserPort.loadUser(CHILD_ID)).thenReturn(Optional.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));
		given(checkActivePickupPort.isInActivePickup(CHILD_ID)).willReturn(true);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, CHILD_ID);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> removeMemberService.removeMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo("MEMBER_IN_ACTIVE_PICKUP");
		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		verify(saveUserPort, never()).deleteUser(anyLong());
		verify(revokeMemberDevicesPort, never()).revokeDevicesOf(anyLong());
	}

	@Test
	@DisplayName("멤버 해제가 성공하면 RevokeMemberDevicesPort로 그 멤버의 기기 · 토큰 폐기를 요청한다")
	void revokesDevicesOfRemovedMember() {
		given(loadUserPort.loadUser(TEACHER_ID)).willReturn(Optional.of(
				User.restore(TEACHER_ID, FAMILY_ID, Role.TEACHER, "박선생", "+821055556666")));
		given(checkActivePickupPort.isInActivePickup(TEACHER_ID)).willReturn(false);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, TEACHER_ID);

		removeMemberService.removeMember(command);

		verify(revokeMemberDevicesPort, times(1)).revokeDevicesOf(TEACHER_ID);
		verify(saveUserPort, times(1)).deleteUser(TEACHER_ID);
		verify(revokeMemberDevicesPort, never()).revokeDevicesOf(GUARDIAN_ID);
	}

	@Test
	@DisplayName("멤버 해제는 같은 가족의 CHILD를 지우고 그 자녀의 기기 · 토큰 폐기를 요청한다")
	void removesChildAndRevokesDevices() {
		given(loadUserPort.loadUser(CHILD_ID)).willReturn(Optional.of(
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));
		given(checkActivePickupPort.isInActivePickup(CHILD_ID)).willReturn(false);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, CHILD_ID);

		removeMemberService.removeMember(command);

		verify(saveUserPort, times(1)).deleteUser(CHILD_ID);
		verify(revokeMemberDevicesPort, times(1)).revokeDevicesOf(CHILD_ID);
	}

	@Test
	@DisplayName("다른 가족의 userId를 해제하려 하면 NOT_FOUND로 거절한다")
	void rejectsRemovingMemberOfOtherFamily() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.of(
				User.restore(DELEGATE_ID, OTHER_FAMILY_ID, Role.DELEGATE, "남대리", "+821033334444")));
		lenient().when(checkActivePickupPort.isInActivePickup(anyLong())).thenReturn(false);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, DELEGATE_ID);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> removeMemberService.removeMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveUserPort, never()).deleteUser(anyLong());
		verify(revokeMemberDevicesPort, never()).revokeDevicesOf(anyLong());
	}

	@Test
	@DisplayName("없는 userId를 해제하려 하면 NOT_FOUND로 거절한다")
	void rejectsRemovingMissingMember() {
		given(loadUserPort.loadUser(DELEGATE_ID)).willReturn(Optional.empty());
		lenient().when(checkActivePickupPort.isInActivePickup(anyLong())).thenReturn(false);
		RemoveMemberCommand command = new RemoveMemberCommand(FAMILY_ID, GUARDIAN_ID, DELEGATE_ID);

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> removeMemberService.removeMember(command));

		assertThat(exception).isNotNull();
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		verify(saveUserPort, never()).deleteUser(anyLong());
		verify(revokeMemberDevicesPort, never()).revokeDevicesOf(anyLong());
	}
}
