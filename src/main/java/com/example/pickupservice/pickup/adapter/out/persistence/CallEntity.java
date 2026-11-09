package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pickup_call")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long callId;

	@Column(nullable = false)
	private Long sessionId;

	private Long prevCallId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CallTriggerType triggerType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CallStatus status;

	@Column(nullable = false)
	private int spotVersion;

	private Instant sentAt;

	private Instant deliveredAt;

	private Instant acknowledgedAt;

	private Instant revokedAt;

	@Column(length = 100)
	private String revokeReason;

	public CallEntity(Long callId, Long sessionId, Long prevCallId, CallTriggerType triggerType, CallStatus status,
			int spotVersion, Instant sentAt, Instant deliveredAt, Instant acknowledgedAt, Instant revokedAt,
			String revokeReason) {
		this.callId = callId;
		this.sessionId = sessionId;
		this.prevCallId = prevCallId;
		this.triggerType = triggerType;
		this.status = status;
		this.spotVersion = spotVersion;
		this.sentAt = sentAt;
		this.deliveredAt = deliveredAt;
		this.acknowledgedAt = acknowledgedAt;
		this.revokedAt = revokedAt;
		this.revokeReason = revokeReason;
	}

}
