package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.notification.application.port.in.DeliveryReceiptCommand;
import com.example.pickupservice.notification.application.port.in.RecordDeliveryUseCase;
import com.example.pickupservice.notification.application.port.out.CallProgressPort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.SaveNotificationPort;
import com.example.pickupservice.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecordDeliveryService implements RecordDeliveryUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final CallProgressPort callProgressPort;

	@Override
	@Transactional
	public void recordDelivery(DeliveryReceiptCommand command) {
		Notification notification = loadNotificationPort.load(command.notificationId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		requireOwner(command.principal(), notification);

		boolean first = notification.getDeliveredAt() == null;
		boolean markCall = first && "CALL".equals(notification.getType()) && notification.getCallId() != null
				&& !isCallDeliveredInSession(notification);
		notification.markDelivered(command.receivedAt());
		saveNotificationPort.save(notification);

		if (markCall) {
			callProgressPort.markCallDelivered(notification.getCallId(), command.receivedAt());
		}
	}

	private boolean isCallDeliveredInSession(Notification notification) {
		return loadNotificationPort.loadBySession(notification.getSessionId()).stream()
				.anyMatch(row -> row.getDeliveredAt() != null
						&& "CALL".equals(row.getType())
						&& Objects.equals(row.getCallId(), notification.getCallId()));
	}

	private void requireOwner(AuthPrincipal principal, Notification notification) {
		if (principal.type() != PrincipalType.USER) {
			throw BusinessException.of(ErrorCode.NOT_FOUND);
		}
		PushTarget target = loadPushTargetPort.loadPushTargetIncludingRevoked(notification.getDeviceId())
				.orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND));
		if (target.userId() != principal.id()) {
			throw BusinessException.of(ErrorCode.NOT_FOUND);
		}
	}
}
