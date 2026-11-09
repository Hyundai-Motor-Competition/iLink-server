package com.example.pickupservice.notification.adapter.in.web.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.notification.adapter.in.web.DeliveryReceiptRequest;
import com.example.pickupservice.notification.adapter.in.web.NotificationsResponse;
import com.example.pickupservice.notification.adapter.in.web.ResendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "알림", description = "알림 수신 확인 · 이력 조회")
public interface NotificationApiDoc {

	@Operation(summary = "알림 수신 확인 (NOTI-01)",
			description = "받은 기기가 알림 수신 시각을 보고한다. 여러 번 보내도 첫 수신 시각을 유지한다")
	@ApiResponse(responseCode = "204", description = "본문 없음")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (알림이 없거나 다른 기기의 것)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (receivedAt 없음)")
	void recordDelivery(@Parameter(hidden = true) AuthPrincipal principal,
			long notificationId, DeliveryReceiptRequest request);

	@Operation(summary = "알림 이력 조회 (NOTI-02)",
			description = "GUARDIAN · CO_GUARDIAN · DELEGATE. 세션의 알림을 발송 순서대로 돌려준다")
	@ApiResponse(responseCode = "200", description = "items[] (notificationId · type · receiverName · title · "
			+ "sentAt · deliveredAt · openedAt · attempt)")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 가족의 것)")
	NotificationsResponse list(@Parameter(hidden = true) AuthPrincipal principal, long sessionId);

	@Operation(summary = "알림 다시 보내기 (NOTI-03)",
			description = "GUARDIAN · CO_GUARDIAN · DELEGATE. 같은 알림을 새 행으로 다시 보내고 새 notificationId를 돌려준다. "
					+ "마지막 발송 뒤 1분 안이면 거절한다. Idempotency-Key 헤더가 필요하다")
	@ApiResponse(responseCode = "202", description = "새 notificationId")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (TEACHER · CHILD 토큰)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (알림이 없거나 다른 가족의 것)")
	@ApiResponse(responseCode = "409", description = "SESSION_ALREADY_FINISHED (이미 끝난 세션) · "
			+ "CALL_ALREADY_REVOKED (이미 철회된 호출)")
	@ApiResponse(responseCode = "422", description = "VALIDATION_FAILED (Idempotency-Key 없음)")
	@ApiResponse(responseCode = "429", description = "RESEND_TOO_SOON (1분 안에 다시 보냄, retryAfterSeconds 포함)")
	ResendResponse resend(@Parameter(hidden = true) AuthPrincipal principal, long notificationId);

}
