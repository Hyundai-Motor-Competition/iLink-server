package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "device", indexes = @Index(name = "idx_device_owner", columnList = "ownerType, ownerId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long deviceId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private OwnerType ownerType;

	@Column(nullable = false)
	private Long ownerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Platform platform;

	@Column(length = 255)
	private String pushToken;

	private Instant pushUpdatedAt;

	@Column(nullable = false, columnDefinition = "char(64)")
	private String refreshTokenHash;

	private Instant revokedAt;

	public DeviceEntity(Long deviceId, OwnerType ownerType, Long ownerId, Platform platform, String pushToken,
			Instant pushUpdatedAt, String refreshTokenHash, Instant revokedAt) {
		this.deviceId = deviceId;
		this.ownerType = ownerType;
		this.ownerId = ownerId;
		this.platform = platform;
		this.pushToken = pushToken;
		this.pushUpdatedAt = pushUpdatedAt;
		this.refreshTokenHash = refreshTokenHash;
		this.revokedAt = revokedAt;
	}

}
