package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import com.example.pickupservice.user.application.port.in.RegisterMemberUseCase;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.PhoneRule;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterMemberService implements RegisterMemberUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;

	@Override
	@Transactional
	public long registerMember(RegisterMemberCommand command) {
		if (command.requesterRole() == Role.CO_GUARDIAN && command.role() == Role.CO_GUARDIAN) {
			throw BusinessException.of(ErrorCode.FORBIDDEN);
		}
		if (command.role() != Role.CHILD) {
			NonChildMemberLimit.checkRoomFor(loadUserPort, command.familyId());
		}
		if (command.role() != Role.CHILD && !PhoneRule.isValid(command.phone())) {
			throw BusinessException.invalidField("phone", "자녀 외 멤버는 국제 형식 전화번호가 필요합니다");
		}
		User saved = saveUserPort.saveUser(
				User.create(command.familyId(), command.role(), command.name(), command.phone()));
		return saved.getUserId();
	}
}
