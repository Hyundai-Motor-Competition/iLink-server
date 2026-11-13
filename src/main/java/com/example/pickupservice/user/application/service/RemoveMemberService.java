package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.RemoveMemberCommand;
import com.example.pickupservice.user.application.port.in.RemoveMemberUseCase;
import com.example.pickupservice.user.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.RevokeMemberDevicesPort;
import com.example.pickupservice.user.application.port.out.SaveUserPort;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RemoveMemberService implements RemoveMemberUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;
	private final CheckActivePickupPort checkActivePickupPort;
	private final RevokeMemberDevicesPort revokeMemberDevicesPort;
	private final Clock clock;

	@Override
	@Transactional
	public void removeMember(RemoveMemberCommand command) {
		User user = loadUserPort.loadUser(command.userId())
				.filter(found -> Objects.equals(found.getFamilyId(), command.familyId()))
				.filter(found -> !found.isRemoved())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		if (command.userId() == command.requesterId() || user.getRole() == Role.GUARDIAN) {
			throw UserErrors.cannotRemoveGuardian();
		}
		if (checkActivePickupPort.isInActivePickup(command.userId())) {
			throw UserErrors.memberInActivePickup();
		}
		user.remove(Instant.now(clock));
		saveUserPort.saveUser(user);
		revokeMemberDevicesPort.revokeDevicesOf(command.userId());
	}
}
