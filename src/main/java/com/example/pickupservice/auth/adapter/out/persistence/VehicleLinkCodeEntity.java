package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
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
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "vehicle_link_code", indexes = @Index(name = "idx_vehicle_link_code_code", columnList = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleLinkCodeEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codeId;

	@Column(nullable = false)
	private Long vehicleId;

	private Long issuedDeviceId;

	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean issuedBeforeLink;

	@Column(nullable = false, columnDefinition = "char(8)")
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LinkCodeStatus status;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant usedAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private LinkMode mode;

	private Instant tokenDeliveredAt;

	public VehicleLinkCodeEntity(Long codeId, Long vehicleId, Long issuedDeviceId, boolean issuedBeforeLink,
			String code, LinkCodeStatus status, Instant expiresAt, Instant usedAt, LinkMode mode,
			Instant tokenDeliveredAt) {
		this.codeId = codeId;
		this.vehicleId = vehicleId;
		this.issuedDeviceId = issuedDeviceId;
		this.issuedBeforeLink = issuedBeforeLink;
		this.code = code;
		this.status = status;
		this.expiresAt = expiresAt;
		this.usedAt = usedAt;
		this.mode = mode;
		this.tokenDeliveredAt = tokenDeliveredAt;
	}

}
