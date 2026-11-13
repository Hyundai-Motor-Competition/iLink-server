package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.AcceptInviteCommand;
import com.example.pickupservice.auth.application.port.in.AcceptInviteResult;
import com.example.pickupservice.auth.application.port.in.AcceptInviteUseCase;
import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AcceptInviteService implements AcceptInviteUseCase {

	private final LoadInvitePort loadInvitePort;
	private final SaveInvitePort saveInvitePort;
	private final LoadMemberUseCase loadMemberUseCase;
	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final IssueTokenPort issueTokenPort;
	private final Clock clock;

	@Override
	@Transactional
	public AcceptInviteResult acceptInvite(String token, AcceptInviteCommand command) {
		Instant now = clock.instant();
		MemberInvite invite = InviteRules.loadUsableInvite(loadInvitePort, token, now);
		MemberInfo member = loadMemberUseCase.findMember(invite.getUserId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		ConsentRules.requireAgreed(InviteRules.requiredConsents(member.role()), command.consents());
		revokePreviousDevices(member.userId(), now);
		IssuedTokens tokens = registerMemberDevice(member, command, now);
		invite.markUsed(now);
		saveInvitePort.saveInvite(invite);
		return new AcceptInviteResult(member.userId(), member.familyId(), member.role(), tokens.accessToken(),
				tokens.refreshToken());
	}

	private void revokePreviousDevices(long userId, Instant now) {
		for (Device previous : loadDevicePort.loadLiveDevices(OwnerType.USER, userId)) {
			previous.revoke(now);
			saveDevicePort.saveDevice(previous);
		}
	}

	private IssuedTokens registerMemberDevice(MemberInfo member, AcceptInviteCommand command, Instant now) {
		Device device = saveDevicePort.saveDevice(Device.register(OwnerType.USER, member.userId(),
				command.platform(), command.pushToken(), now, PlaceholderRefreshHash.next(), null));
		return new DeviceTokenRotator(issueTokenPort, saveDevicePort).issueTo(device, new TokenSubject(
				PrincipalType.USER, member.userId(), member.familyId(), member.role(), device.getDeviceId()));
	}
}
