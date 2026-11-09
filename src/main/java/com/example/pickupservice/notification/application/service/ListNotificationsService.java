package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.notification.application.port.in.ListNotificationsUseCase;
import com.example.pickupservice.notification.application.port.in.NotificationListQuery;
import com.example.pickupservice.notification.application.port.in.NotificationView;
import com.example.pickupservice.notification.application.port.out.LoadMemberNamePort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListNotificationsService implements ListNotificationsUseCase {

	private final LoadNotificationPort loadNotificationPort;
	private final LoadPushTargetPort loadPushTargetPort;
	private final LoadMemberNamePort loadMemberNamePort;
	private final com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort loadSessionFamilyPort;

	@Override
	@Transactional(readOnly = true)
	public List<NotificationView> list(NotificationListQuery query) {
		Optional<Long> familyId = loadSessionFamilyPort.loadFamilyId(query.sessionId());
		if (familyId.isEmpty() || !familyId.get().equals(query.principal().familyId())) {
			throw BusinessException.of(ErrorCode.NOT_FOUND);
		}
		List<Notification> rows = loadNotificationPort.loadBySession(query.sessionId());
		Map<Long, Long> userIdByDevice = new HashMap<>();
		for (Notification row : rows) {
			if (!userIdByDevice.containsKey(row.getDeviceId())) {
				Optional<PushTarget> target = loadPushTargetPort.loadPushTarget(row.getDeviceId());
				target.ifPresent(t -> userIdByDevice.put(row.getDeviceId(), t.userId()));
			}
		}
		List<Long> userIds = userIdByDevice.values().stream().distinct().toList();
		Map<Long, String> names = loadMemberNamePort.loadNames(userIds);

		List<NotificationView> views = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			Notification row = rows.get(i);
			int attempt = 1;
			for (int j = 0; j < i; j++) {
				if (sameGroup(rows.get(j), row)) {
					attempt++;
				}
			}
			Long userId = userIdByDevice.get(row.getDeviceId());
			String receiverName = userId == null ? null : names.get(userId);
			views.add(new NotificationView(row.getNotificationId(), row.getType(), receiverName,
					NotificationTexts.title(row.getType()), row.getSentAt(), row.getDeliveredAt(), row.getOpenedAt(),
					attempt));
		}
		return views;
	}

	private static boolean sameGroup(Notification a, Notification b) {
		return Objects.equals(a.getCallId(), b.getCallId()) && a.getType().equals(b.getType())
				&& a.getDeviceId() == b.getDeviceId();
	}
}
