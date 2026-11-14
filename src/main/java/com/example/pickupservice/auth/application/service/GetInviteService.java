package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.GetInviteUseCase;
import com.example.pickupservice.auth.application.port.in.InviteInfo;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class GetInviteService implements GetInviteUseCase {

	private final LoadInvitePort loadInvitePort;
	private final LoadMemberUseCase loadMemberUseCase;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public InviteInfo getInvite(String token) {
		MemberInvite invite = InviteRules.loadUsableInvite(loadInvitePort, token, clock.instant());
		MemberInfo member = loadMemberUseCase.findMember(invite.getUserId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		InviteRules.requireNotRemoved(member);
		return new InviteInfo(findGuardianName(member.familyId()), member.name(), member.role(),
				InviteRules.requiredConsents(member.role()), invite.getExpiresAt());
	}

	private String findGuardianName(long familyId) {
		return loadMemberUseCase.findMembers(familyId).stream()
				.filter(member -> member.role() == Role.GUARDIAN)
				.map(MemberInfo::name)
				.findFirst()
				.orElse(null);
	}
}
