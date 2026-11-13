package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.ApproveLinkCommand;
import com.example.pickupservice.auth.application.port.in.ApproveLinkUseCase;
import com.example.pickupservice.auth.application.port.in.VehicleLinkResult;
import com.example.pickupservice.auth.application.port.out.IssueTokenPort;
import com.example.pickupservice.auth.application.port.out.IssuedTokens;
import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.PublishAuthEventPort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.application.port.out.TokenSubject;
import com.example.pickupservice.auth.domain.Consents;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageCommand;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageUseCase;
import com.example.pickupservice.user.application.port.in.CreateGuardianCommand;
import com.example.pickupservice.user.application.port.in.CreateGuardianUseCase;
import com.example.pickupservice.user.application.port.in.GuardianAccount;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApproveLinkService implements ApproveLinkUseCase {

	private final LoadLinkCodePort loadLinkCodePort;
	private final SaveLinkCodePort saveLinkCodePort;
	private final LoadVehicleUseCase loadVehicleUseCase;
	private final CreateGuardianUseCase createGuardianUseCase;
	private final LoadMemberUseCase loadMemberUseCase;
	private final ChangeProfileImageUseCase changeProfileImageUseCase;
	private final LoadDevicePort loadDevicePort;
	private final SaveDevicePort saveDevicePort;
	private final IssueTokenPort issueTokenPort;
	private final PublishAuthEventPort publishAuthEventPort;
	private final LinkCodeIssueCounter linkCodeIssueCounter;
	private final Clock clock;

	@Override
	@Transactional
	public VehicleLinkResult approveLink(ApproveLinkCommand command) {
		Instant now = clock.instant();
		LinkCode linkCode = loadUsableCode(command.code(), now);
		VehicleInfo vehicle = loadVehicleUseCase.findVehicle(linkCode.getVehicleId())
				.orElseThrow(AuthErrors::linkCodeInvalid);
		verifyMode(command.mode(), linkCode, vehicle);
		ConsentRules.requireAgreed(Consents.requiredForGuardianLink(), command.consents());
		GuardianAccount guardian = resolveGuardian(command, vehicle, now);
		IssuedTokens tokens = registerGuardianDevice(guardian, command, now);
		linkCode.approve(command.mode(), now);
		saveLinkCodePort.saveLinkCode(linkCode);
		if (linkCode.getIssuedDeviceId() != null) {
			linkCodeIssueCounter.reset(linkCode.getIssuedDeviceId());
		}
		return new VehicleLinkResult(vehicle.vehicleId(), guardian.familyId(), guardian.userId(),
				tokens.accessToken(), tokens.refreshToken());
	}

	private LinkCode loadUsableCode(String code, Instant now) {
		LinkCode linkCode = loadLinkCodePort.loadLinkCodeByCode(code).orElseThrow(AuthErrors::linkCodeInvalid);
		if (!LinkCodeRules.isUsable(linkCode, now)) {
			throw AuthErrors.linkCodeExpired();
		}
		return linkCode;
	}

	private static void verifyMode(LinkMode requested, LinkCode linkCode, VehicleInfo vehicle) {
		if (requested != LinkCodeRules.actualModeOf(linkCode, vehicle)) {
			throw AuthErrors.linkModeMismatch();
		}
	}

	private GuardianAccount resolveGuardian(ApproveLinkCommand command, VehicleInfo vehicle, Instant now) {
		if (command.mode() == LinkMode.NEW) {
			return createGuardianUseCase.createGuardian(
					new CreateGuardianCommand(vehicle.vehicleId(), null, command.name(), command.profileImage()));
		}
		GuardianAccount guardian = findGuardian(vehicle.familyId());
		revokePreviousDevices(guardian.userId(), now);
		if (command.profileImage() != null) {
			changeProfileImageUseCase.changeProfileImage(
					new ChangeProfileImageCommand(guardian.userId(), command.profileImage()));
		}
		return guardian;
	}

	private GuardianAccount findGuardian(long familyId) {
		return loadMemberUseCase.findMembers(familyId).stream()
				.filter(member -> member.role() == Role.GUARDIAN)
				.findFirst()
				.map(member -> new GuardianAccount(familyId, member.userId()))
				.orElseThrow(AuthErrors::linkCodeInvalid);
	}

	private void revokePreviousDevices(long userId, Instant now) {
		List<Device> previous = loadDevicePort.loadLiveDevices(OwnerType.USER, userId);
		if (previous.isEmpty()) {
			return;
		}
		for (Device device : previous) {
			device.revoke(now);
			saveDevicePort.saveDevice(device);
		}
		publishAuthEventPort.publishDeviceReplaced(previous.stream().map(Device::getDeviceId).toList());
	}

	private IssuedTokens registerGuardianDevice(GuardianAccount guardian, ApproveLinkCommand command, Instant now) {
		Device device = saveDevicePort.saveDevice(Device.register(OwnerType.USER, guardian.userId(),
				command.platform(), command.pushToken(), now, PlaceholderRefreshHash.next(), null));
		return new DeviceTokenRotator(issueTokenPort, saveDevicePort).issueTo(device, new TokenSubject(
				PrincipalType.USER, guardian.userId(), guardian.familyId(), Role.GUARDIAN, device.getDeviceId()));
	}
}
