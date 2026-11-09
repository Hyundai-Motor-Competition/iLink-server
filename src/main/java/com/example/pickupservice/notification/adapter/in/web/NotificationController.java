package com.example.pickupservice.notification.adapter.in.web;

import com.example.pickupservice.common.idempotency.IdempotencyKeyRequired;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.notification.adapter.in.web.doc.NotificationApiDoc;
import com.example.pickupservice.notification.application.port.in.ListNotificationsUseCase;
import com.example.pickupservice.notification.application.port.in.NotificationListQuery;
import com.example.pickupservice.notification.application.port.in.RecordDeliveryUseCase;
import com.example.pickupservice.notification.application.port.in.ResendCommand;
import com.example.pickupservice.notification.application.port.in.ResendNotificationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApiDoc {

	private final RecordDeliveryUseCase recordDeliveryUseCase;
	private final ListNotificationsUseCase listNotificationsUseCase;
	private final ResendNotificationUseCase resendNotificationUseCase;

	@Override
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PostMapping("/v1/notifications/{notificationId}/delivery-receipt")
	public void recordDelivery(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long notificationId,
			@Valid @RequestBody DeliveryReceiptRequest request) {
		recordDeliveryUseCase.recordDelivery(request.toCommand(principal, notificationId));
	}

	@Override
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE')")
	@GetMapping("/v1/pickup-sessions/{sessionId}/notifications")
	public NotificationsResponse list(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long sessionId) {
		return NotificationsResponse.from(
				listNotificationsUseCase.list(new NotificationListQuery(principal, sessionId)));
	}

	@Override
	@ResponseStatus(HttpStatus.ACCEPTED)
	@PreAuthorize("hasAnyRole('GUARDIAN', 'CO_GUARDIAN', 'DELEGATE')")
	@IdempotencyKeyRequired
	@PostMapping("/v1/notifications/{notificationId}/resend")
	public ResendResponse resend(
			@AuthenticationPrincipal AuthPrincipal principal,
			@PathVariable long notificationId) {
		return new ResendResponse(resendNotificationUseCase.resend(new ResendCommand(principal, notificationId)));
	}

}
