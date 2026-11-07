package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.in.UpdateMemberCommand;
import com.example.pickupservice.user.application.port.in.UpdateMemberUseCase;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UpdateMemberService implements UpdateMemberUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;
	private final LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;

	@Override
	@Transactional
	public MemberResult updateMember(UpdateMemberCommand command) {
		User user = loadUserPort.loadUser(command.userId())
				.filter(found -> Objects.equals(found.getFamilyId(), command.familyId()))
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		checkRoleChangeAllowed(command);
		checkChildPromotionAllowed(user, command);
		applyChanges(user, command);
		User saved = saveUserPort.saveUser(user);
		return MemberResults.withDeviceStatus(List.of(saved), loadMemberDeviceStatusPort).getFirst();
	}

	private void checkRoleChangeAllowed(UpdateMemberCommand command) {
		if (command.role() == null) {
			return;
		}
		if (command.requesterRole() == Role.CO_GUARDIAN) {
			throw BusinessException.of(ErrorCode.FORBIDDEN);
		}
		if (command.requesterRole() == Role.GUARDIAN && command.requesterId() == command.userId()) {
			throw BusinessException.invalidField("role", "보호자 자신의 역할은 바꿀 수 없습니다");
		}
	}

	private void checkChildPromotionAllowed(User user, UpdateMemberCommand command) {
		if (user.getRole() != Role.CHILD || command.role() == null || command.role() == Role.CHILD) {
			return;
		}
		String phone = command.phone() != null ? command.phone() : user.getPhone();
		if (phone == null || phone.isBlank()) {
			throw BusinessException.invalidField("phone", "자녀 외 멤버는 전화번호가 필요합니다");
		}
		NonChildMemberLimit.checkRoomFor(loadUserPort, command.familyId());
	}

	private void applyChanges(User user, UpdateMemberCommand command) {
		if (command.name() != null) {
			user.changeName(command.name());
		}
		if (command.phone() != null) {
			user.changePhone(command.phone());
		}
		if (command.role() != null) {
			user.changeRole(command.role());
		}
	}
}
