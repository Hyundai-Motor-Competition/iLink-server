package com.example.pickupservice.pickup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallTest {

	private static final long SESSION_ID = 7L;
	private static final long REVOKED_CALL_ID = 41L;
	private static final int SPOT_VERSION = 3;
	private static final Instant SENT_AT = Instant.parse("2026-11-09T09:10:00Z");
	private static final Instant REVOKED_AT = Instant.parse("2026-11-09T09:12:30Z");
	private static final String REVOKE_REASON = "보호자가 합류 안내를 철회함";
	private static final Instant SECOND_REVOKED_AT = Instant.parse("2026-11-09T09:14:00Z");
	private static final String SECOND_REVOKE_REASON = "두 번째 철회 시도";
	private static final long SCHEDULE_ID = 100L;
	private static final long FIRST_CHILD_USER_ID = 31L;
	private static final long SECOND_CHILD_USER_ID = 32L;
	private static final Instant FIRST_ACK_AT = Instant.parse("2026-11-09T09:11:00Z");
	private static final Instant SECOND_ACK_AT = Instant.parse("2026-11-09T09:11:40Z");
	private static final long CALL_ID = 55L;
	private static final Instant ACCEPTED_AT = Instant.parse("2026-11-09T09:09:00Z");
	private static final Instant DELIVERED_AT = Instant.parse("2026-11-09T09:10:20Z");
	private static final Instant LATE_SENT_AT = Instant.parse("2026-11-09T09:13:00Z");
	private static final Instant LATE_DELIVERED_AT = Instant.parse("2026-11-09T09:13:10Z");

	@Test
	@DisplayName("새 Call은 status ACCEPTED와 triggerType · 그때의 spotVersion을 갖는다")
	void acceptCreatesAcceptedCallWithTriggerTypeAndCurrentSpotVersion() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.DIST_700M, SPOT_VERSION);

		assertThat(call.getCallId()).isNull();
		assertThat(call.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(call.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(call.getTriggerType()).isEqualTo(CallTriggerType.DIST_700M);
		assertThat(call.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(call.getPrevCallId()).isNull();
		assertThat(call.getSentAt()).isNull();
		assertThat(call.getRevokedAt()).isNull();
		assertThat(call.getRevokeReason()).isNull();
	}

	@Test
	@DisplayName("철회된 호출 뒤에 만든 Call은 prevCallId로 이전 호출을 가리킨다")
	void acceptAfterRevokedCallPointsPrevCallIdToPreviousCall() {
		Call call = Call.acceptAfter(SESSION_ID, REVOKED_CALL_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);

		assertThat(call.getPrevCallId()).isEqualTo(REVOKED_CALL_ID);
		assertThat(call.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(call.getStatus()).isEqualTo(CallStatus.ACCEPTED);
		assertThat(call.getTriggerType()).isEqualTo(CallTriggerType.ETA_3MIN);
		assertThat(call.getSpotVersion()).isEqualTo(SPOT_VERSION);
	}

	@Test
	@DisplayName("Call.markSent는 status를 SENT로 바꾸고 sentAt을 채운다")
	void markSentChangesStatusToSentAndFillsSentAt() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);

		call.markSent(SENT_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.SENT);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("Call.revoke는 status REVOKED · revokedAt · revokeReason을 채운다")
	void revokeFillsRevokedStatusRevokedAtAndRevokeReason() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);

		call.revoke(REVOKE_REASON, REVOKED_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(call.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(call.getRevokeReason()).isEqualTo(REVOKE_REASON);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("이미 REVOKED인 Call을 revoke하면 CALL_ALREADY_REVOKED로 거절되고 첫 철회 값은 그대로다")
	void revokeAlreadyRevokedCallIsRejectedWithCallAlreadyRevokedAndKeepsFirstRevocation() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		call.revoke(REVOKE_REASON, REVOKED_AT);

		assertThatThrownBy(() -> call.revoke(SECOND_REVOKE_REASON, SECOND_REVOKED_AT))
				.isInstanceOfSatisfying(PickupRuleException.class,
						e -> assertThat(e.violation()).isEqualTo(PickupViolation.CALL_ALREADY_REVOKED));

		assertThat(call.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(call.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(call.getRevokeReason()).isEqualTo(REVOKE_REASON);
	}

	@Test
	@DisplayName("자녀 둘인 세션에서 한 명만 확인하면 Call은 ACKNOWLEDGED가 아니고 status SENT · acknowledgedAt null 그대로다")
	void acknowledgeIfAllChildrenKeepsSentWhenOnlyOneOfTwoChildrenAcknowledged() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		SessionChild first = calledChild(1L, FIRST_CHILD_USER_ID);
		SessionChild second = calledChild(2L, SECOND_CHILD_USER_ID);

		first.acknowledge(FIRST_ACK_AT);
		call.acknowledgeIfAllChildren(List.of(first, second), FIRST_ACK_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.SENT);
		assertThat(call.getAcknowledgedAt()).isNull();
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("자녀 둘인 세션에서 둘 다 확인하면 Call은 ACKNOWLEDGED이고 acknowledgedAt은 두 번째 확인 시각이다")
	void acknowledgeIfAllChildrenMarksAcknowledgedWithSecondAckTimeWhenBothChildrenAcknowledged() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		SessionChild first = calledChild(1L, FIRST_CHILD_USER_ID);
		SessionChild second = calledChild(2L, SECOND_CHILD_USER_ID);
		List<SessionChild> children = List.of(first, second);

		first.acknowledge(FIRST_ACK_AT);
		call.acknowledgeIfAllChildren(children, FIRST_ACK_AT);
		second.acknowledge(SECOND_ACK_AT);
		call.acknowledgeIfAllChildren(children, SECOND_ACK_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.ACKNOWLEDGED);
		assertThat(call.getAcknowledgedAt()).isEqualTo(SECOND_ACK_AT);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("Call.markDelivered는 SENT인 Call을 DELIVERED로 바꾸고 deliveredAt을 채우며 sentAt은 그대로다")
	void markDeliveredChangesSentCallToDeliveredAndFillsDeliveredAt() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);

		call.markDelivered(DELIVERED_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.DELIVERED);
		assertThat(call.getDeliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("이미 deliveredAt이 있는 Call에 다시 온 markDelivered는 첫 deliveredAt을 덮어쓰지 않고 status도 그대로 둔다")
	void secondMarkDeliveredKeepsFirstDeliveredAt() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		call.markDelivered(DELIVERED_AT);

		call.markDelivered(LATE_DELIVERED_AT);

		assertThat(call.getDeliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(call.getStatus()).isEqualTo(CallStatus.DELIVERED);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("발송 보고보다 수신 확인이 먼저 오면 ACCEPTED인 Call도 DELIVERED · deliveredAt이 되고 sentAt은 null 그대로다")
	void markDeliveredOnAcceptedCallChangesToDeliveredAndKeepsSentAtNull() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);

		call.markDelivered(DELIVERED_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.DELIVERED);
		assertThat(call.getDeliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(call.getSentAt()).isNull();
	}

	@Test
	@DisplayName("DELIVERED인 Call에 늦게 온 markSent는 status를 DELIVERED로 두고 sentAt만 채운다")
	void lateMarkSentOnDeliveredCallKeepsDeliveredStatusAndFillsSentAt() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markDelivered(DELIVERED_AT);

		call.markSent(SENT_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.DELIVERED);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
		assertThat(call.getDeliveredAt()).isEqualTo(DELIVERED_AT);
	}

	@Test
	@DisplayName("이미 sentAt이 있는 Call에 다시 온 markSent는 첫 sentAt을 덮어쓰지 않고 status도 그대로 둔다")
	void secondMarkSentKeepsFirstSentAt() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);

		call.markSent(LATE_SENT_AT);

		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
		assertThat(call.getStatus()).isEqualTo(CallStatus.SENT);
	}

	@Test
	@DisplayName("DELIVERED이고 sentAt도 있는 Call에 다시 온 markSent는 첫 sentAt · deliveredAt · status를 그대로 둔다")
	void secondMarkSentOnDeliveredCallKeepsFirstSentAtAndStatus() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		call.markDelivered(DELIVERED_AT);

		call.markSent(LATE_SENT_AT);

		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
		assertThat(call.getDeliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(call.getStatus()).isEqualTo(CallStatus.DELIVERED);
	}

	@Test
	@DisplayName("ACKNOWLEDGED인 Call에 늦게 온 markDelivered는 status를 ACKNOWLEDGED로 두고 deliveredAt만 채운다")
	void lateMarkDeliveredOnAcknowledgedCallKeepsAcknowledgedStatusAndFillsDeliveredAt() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		SessionChild first = calledChild(1L, FIRST_CHILD_USER_ID);
		SessionChild second = calledChild(2L, SECOND_CHILD_USER_ID);
		first.acknowledge(FIRST_ACK_AT);
		second.acknowledge(SECOND_ACK_AT);
		call.acknowledgeIfAllChildren(List.of(first, second), SECOND_ACK_AT);

		call.markDelivered(LATE_DELIVERED_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.ACKNOWLEDGED);
		assertThat(call.getDeliveredAt()).isEqualTo(LATE_DELIVERED_AT);
		assertThat(call.getAcknowledgedAt()).isEqualTo(SECOND_ACK_AT);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
	}

	@Test
	@DisplayName("Call.restore는 callId · acceptedAt · deliveredAt · acknowledgedAt을 포함한 모든 필드를 그대로 되살린다")
	void restoreRebuildsCallWithEveryField() {
		Call call = Call.restore(CALL_ID, SESSION_ID, REVOKED_CALL_ID, CallTriggerType.MANUAL, CallStatus.REVOKED,
				SPOT_VERSION, ACCEPTED_AT, SENT_AT, DELIVERED_AT, SECOND_ACK_AT, REVOKED_AT, REVOKE_REASON);

		assertThat(call.getCallId()).isEqualTo(CALL_ID);
		assertThat(call.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(call.getPrevCallId()).isEqualTo(REVOKED_CALL_ID);
		assertThat(call.getTriggerType()).isEqualTo(CallTriggerType.MANUAL);
		assertThat(call.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(call.getSpotVersion()).isEqualTo(SPOT_VERSION);
		assertThat(call.getAcceptedAt()).isEqualTo(ACCEPTED_AT);
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
		assertThat(call.getDeliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(call.getAcknowledgedAt()).isEqualTo(SECOND_ACK_AT);
		assertThat(call.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(call.getRevokeReason()).isEqualTo(REVOKE_REASON);
	}

	@Test
	@DisplayName("발송 전에 REVOKED가 된 Call에 늦게 온 markSent는 무시되어 status REVOKED · sentAt null · 철회 값이 그대로다")
	void lateMarkSentOnRevokedCallIsIgnored() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.revoke(REVOKE_REASON, REVOKED_AT);

		call.markSent(LATE_SENT_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(call.getSentAt()).isNull();
		assertThat(call.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(call.getRevokeReason()).isEqualTo(REVOKE_REASON);
	}

	@Test
	@DisplayName("REVOKED인 Call에 늦게 온 markDelivered는 무시되어 status REVOKED · deliveredAt null · sentAt · 철회 값이 그대로다")
	void lateMarkDeliveredOnRevokedCallIsIgnored() {
		Call call = Call.accept(SESSION_ID, CallTriggerType.ETA_3MIN, SPOT_VERSION);
		call.markSent(SENT_AT);
		call.revoke(REVOKE_REASON, REVOKED_AT);

		call.markDelivered(LATE_DELIVERED_AT);

		assertThat(call.getStatus()).isEqualTo(CallStatus.REVOKED);
		assertThat(call.getDeliveredAt()).isNull();
		assertThat(call.getSentAt()).isEqualTo(SENT_AT);
		assertThat(call.getRevokedAt()).isEqualTo(REVOKED_AT);
		assertThat(call.getRevokeReason()).isEqualTo(REVOKE_REASON);
	}

	private static SessionChild calledChild(long sessionChildId, long childUserId) {
		return SessionChild.restore(sessionChildId, childUserId, SCHEDULE_ID, ChildPhase.CALLED, 0, null, null, null,
				null, null);
	}
}
