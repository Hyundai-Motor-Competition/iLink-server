package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.InviteResult;
import com.example.pickupservice.auth.application.port.in.IssueInviteCommand;
import com.example.pickupservice.auth.application.port.in.IssueInviteUseCase;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class IssueInviteService implements IssueInviteUseCase {

	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadDevicePort loadDevicePort;
	private final LoadInvitePort loadInvitePort;
	private final SaveInvitePort saveInvitePort;
	private final InviteTokenGenerator inviteTokenGenerator;
	private final String inviteBaseUrl;
	private final Clock clock;

	public IssueInviteService(LoadMemberUseCase loadMemberUseCase, LoadDevicePort loadDevicePort,
			LoadInvitePort loadInvitePort, SaveInvitePort saveInvitePort, InviteTokenGenerator inviteTokenGenerator,
			@Value("${pickup.invite.base-url}") String inviteBaseUrl, Clock clock) {
		this.loadMemberUseCase = loadMemberUseCase;
		this.loadDevicePort = loadDevicePort;
		this.loadInvitePort = loadInvitePort;
		this.saveInvitePort = saveInvitePort;
		this.inviteTokenGenerator = inviteTokenGenerator;
		this.inviteBaseUrl = inviteBaseUrl;
		this.clock = clock;
	}

	@Override
	@Transactional
	public InviteResult issueInvite(IssueInviteCommand command) {
		verifyFamilyMember(command.familyId(), command.userId());
		if (!command.replaceDevice() && hasLiveDevice(command.userId())) {
			throw AuthErrors.deviceAlreadyConnected();
		}
		Instant now = clock.instant();
		revokePreviousInvite(command.userId(), now);
		MemberInvite invite = saveInvitePort.saveInvite(
				MemberInvite.issue(command.userId(), inviteTokenGenerator.generate(), now));
		return new InviteResult(inviteBaseUrl + invite.getToken(), invite.getExpiresAt());
	}

	private void verifyFamilyMember(long familyId, long userId) {
		MemberInfo member = loadMemberUseCase.findMember(userId)
				.filter(found -> found.familyId() == familyId)
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		InviteRules.requireNotRemoved(member);
		if (member.role() == Role.GUARDIAN) {
			throw BusinessException.of(ErrorCode.FORBIDDEN);
		}
	}

	private boolean hasLiveDevice(long userId) {
		return !loadDevicePort.loadLiveDevices(OwnerType.USER, userId).isEmpty();
	}

	private void revokePreviousInvite(long userId, Instant now) {
		loadInvitePort.loadLatestInvite(userId)
				.filter(previous -> !previous.isUsed() && !previous.isExpired(now))
				.ifPresent(previous -> {
					previous.revoke(now);
					saveInvitePort.saveInvite(previous);
				});
	}
}
