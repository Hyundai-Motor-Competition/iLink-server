package com.example.pickupservice.pickup.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Call {

	private Long callId;
	private long sessionId;
	private Long prevCallId;
	private CallTriggerType triggerType;
	private CallStatus status;
	private int spotVersion;
	private Instant acceptedAt;
	private Instant sentAt;
	private Instant deliveredAt;
	private Instant acknowledgedAt;
	private Instant revokedAt;
	private String revokeReason;

	public static Call accept(long sessionId, CallTriggerType triggerType, int spotVersion) {
		return new Call(null, sessionId, null, triggerType, CallStatus.ACCEPTED, spotVersion, null, null, null, null,
				null, null);
	}

	public static Call acceptAfter(long sessionId, long prevCallId, CallTriggerType triggerType, int spotVersion) {
		return new Call(null, sessionId, prevCallId, triggerType, CallStatus.ACCEPTED, spotVersion, null, null, null,
				null, null, null);
	}

	public static Call restore(Long callId, long sessionId, Long prevCallId, CallTriggerType triggerType,
			CallStatus status, int spotVersion, Instant acceptedAt, Instant sentAt, Instant deliveredAt,
			Instant acknowledgedAt, Instant revokedAt, String revokeReason) {
		return new Call(callId, sessionId, prevCallId, triggerType, status, spotVersion, acceptedAt, sentAt,
				deliveredAt, acknowledgedAt, revokedAt, revokeReason);
	}

	public void markSent(Instant at) {
		if (status == CallStatus.REVOKED || sentAt != null) {
			return;
		}
		this.sentAt = at;
		if (status == CallStatus.DELIVERED || status == CallStatus.ACKNOWLEDGED) {
			return;
		}
		this.status = CallStatus.SENT;
	}

	public void markDelivered(Instant at) {
		if (status == CallStatus.REVOKED || deliveredAt != null) {
			return;
		}
		this.deliveredAt = at;
		if (status == CallStatus.ACKNOWLEDGED) {
			return;
		}
		this.status = CallStatus.DELIVERED;
	}

	public void acknowledgeIfAllChildren(List<SessionChild> children, Instant at) {
		if (status == CallStatus.ACKNOWLEDGED) {
			return;
		}
		boolean allAcknowledged = children.stream().allMatch(child -> child.getAcknowledgedAt() != null);
		if (!allAcknowledged) {
			return;
		}
		this.status = CallStatus.ACKNOWLEDGED;
		this.acknowledgedAt = at;
	}

	public void revoke(String reason, Instant at) {
		if (status == CallStatus.REVOKED) {
			throw new PickupRuleException(PickupViolation.CALL_ALREADY_REVOKED);
		}
		this.status = CallStatus.REVOKED;
		this.revokedAt = at;
		this.revokeReason = reason;
	}
}
