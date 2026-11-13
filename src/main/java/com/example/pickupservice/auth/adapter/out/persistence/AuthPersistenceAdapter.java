package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.application.port.out.LoadDevicePort;
import com.example.pickupservice.auth.application.port.out.LoadInvitePort;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.application.port.out.SaveDevicePort;
import com.example.pickupservice.auth.application.port.out.SaveInvitePort;
import com.example.pickupservice.auth.application.port.out.SaveLinkCodePort;
import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AuthPersistenceAdapter implements LoadDevicePort, SaveDevicePort, LoadLinkCodePort, SaveLinkCodePort,
		LoadInvitePort, SaveInvitePort {

	private final DeviceJpaRepository deviceJpaRepository;
	private final VehicleLinkCodeJpaRepository vehicleLinkCodeJpaRepository;
	private final MemberInviteJpaRepository memberInviteJpaRepository;

	@Override
	public Optional<Device> loadDevice(long deviceId) {
		return deviceJpaRepository.findById(deviceId).map(AuthPersistenceAdapter::toDomain);
	}

	@Override
	public List<Device> loadLiveDevices(OwnerType type, long ownerId) {
		return deviceJpaRepository.findByOwnerTypeAndOwnerIdAndRevokedAtIsNull(type, ownerId).stream()
				.map(AuthPersistenceAdapter::toDomain)
				.toList();
	}

	@Override
	public Optional<Device> loadByRefreshTokenHash(String hash) {
		return deviceJpaRepository.findByRefreshTokenHash(hash).map(AuthPersistenceAdapter::toDomain);
	}

	@Override
	public Device saveDevice(Device device) {
		return toDomain(deviceJpaRepository.save(toEntity(device)));
	}

	@Override
	public Optional<LinkCode> loadLinkCode(long codeId) {
		return vehicleLinkCodeJpaRepository.findById(codeId).map(AuthPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<LinkCode> loadLinkCodeByCode(String code) {
		return vehicleLinkCodeJpaRepository.findFirstByCodeOrderByCodeIdDesc(code)
				.map(AuthPersistenceAdapter::toDomain);
	}

	@Override
	public List<LinkCode> loadIssuedCodes(long vehicleId) {
		return vehicleLinkCodeJpaRepository.findByVehicleIdAndStatus(vehicleId, LinkCodeStatus.ISSUED).stream()
				.map(AuthPersistenceAdapter::toDomain)
				.toList();
	}

	@Override
	public LinkCode saveLinkCode(LinkCode linkCode) {
		return toDomain(vehicleLinkCodeJpaRepository.save(toEntity(linkCode)));
	}

	@Override
	public Optional<MemberInvite> loadInviteByToken(String token) {
		return memberInviteJpaRepository.findByToken(token).map(AuthPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<MemberInvite> loadLatestInvite(long userId) {
		return memberInviteJpaRepository.findFirstByUserIdOrderByInviteIdDesc(userId)
				.map(AuthPersistenceAdapter::toDomain);
	}

	@Override
	public MemberInvite saveInvite(MemberInvite invite) {
		return toDomain(memberInviteJpaRepository.save(toEntity(invite)));
	}

	private static DeviceEntity toEntity(Device device) {
		return new DeviceEntity(device.getDeviceId(), device.getOwnerType(), device.getOwnerId(), device.getPlatform(),
				device.getPushToken(), device.getPushUpdatedAt(), device.getRefreshTokenHash(), device.getRevokedAt(),
				device.getRefreshTokenExpiresAt());
	}

	private static VehicleLinkCodeEntity toEntity(LinkCode linkCode) {
		return new VehicleLinkCodeEntity(linkCode.getCodeId(), linkCode.getVehicleId(), linkCode.getIssuedDeviceId(),
				linkCode.isIssuedBeforeLink(), linkCode.getCode(),
				linkCode.getStatus(), linkCode.getExpiresAt(), linkCode.getUsedAt(), linkCode.getMode(),
				linkCode.getTokenDeliveredAt());
	}

	private static MemberInviteEntity toEntity(MemberInvite invite) {
		return new MemberInviteEntity(invite.getInviteId(), invite.getUserId(), invite.getToken(),
				invite.getExpiresAt(), invite.getUsedAt());
	}

	private static Device toDomain(DeviceEntity entity) {
		return Device.restore(entity.getDeviceId(), entity.getOwnerType(), entity.getOwnerId(), entity.getPlatform(),
				entity.getPushToken(), entity.getPushUpdatedAt(), entity.getRefreshTokenHash(),
				entity.getRevokedAt(), entity.getRefreshTokenExpiresAt());
	}

	private static LinkCode toDomain(VehicleLinkCodeEntity entity) {
		return LinkCode.restore(entity.getCodeId(), entity.getVehicleId(), entity.getIssuedDeviceId(),
				entity.isIssuedBeforeLink(), entity.getCode(), entity.getStatus(),
				entity.getExpiresAt(), entity.getUsedAt(), entity.getMode(), entity.getTokenDeliveredAt());
	}

	private static MemberInvite toDomain(MemberInviteEntity entity) {
		return MemberInvite.restore(entity.getInviteId(), entity.getUserId(), entity.getToken(),
				entity.getExpiresAt(), entity.getUsedAt());
	}

}
