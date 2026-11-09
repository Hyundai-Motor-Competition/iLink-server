package com.example.pickupservice.pickup.adapter.out.notification;

import com.example.pickupservice.notification.application.port.in.RecordNotificationUseCase;
import com.example.pickupservice.pickup.application.port.out.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupNotificationAdapterTest {

	@Mock
	private RecordNotificationUseCase recordNotificationUseCase;

	@Test
	@DisplayName("recordNotifications는 pickup 요청의 sessionId · callId · type · receiverUserIds를 같은 값으로 notification NotificationRequest에 옮겨 RecordNotificationUseCase.record를 부르고 그 결과 notificationId 목록을 그대로 돌려준다")
	void recordNotificationsDelegatesWithSameValuesAndReturnsNotificationIds() {
		PickupNotificationAdapter adapter = new PickupNotificationAdapter(recordNotificationUseCase);
		when(recordNotificationUseCase.record(any())).thenReturn(List.of(7001L, 7002L));

		List<Long> result = adapter.recordNotifications(new NotificationRequest(501L, 3001L, "CALL", List.of(31L, 32L)));

		assertThat(result).containsExactly(7001L, 7002L);
		ArgumentCaptor<com.example.pickupservice.notification.application.port.in.NotificationRequest> captor =
				ArgumentCaptor.forClass(com.example.pickupservice.notification.application.port.in.NotificationRequest.class);
		verify(recordNotificationUseCase).record(captor.capture());
		assertThat(captor.getValue().sessionId()).isEqualTo(501L);
		assertThat(captor.getValue().callId()).isEqualTo(3001L);
		assertThat(captor.getValue().type()).isEqualTo("CALL");
		assertThat(captor.getValue().receiverUserIds()).containsExactly(31L, 32L);
	}

	@Test
	@DisplayName("callId가 null인 요청(세션 단위 알림)은 callId를 null로 그대로 옮기고 받는 사람이 없어 유스케이스가 빈 목록을 주면 빈 목록을 돌려준다")
	void recordNotificationsKeepsNullCallIdAndReturnsEmptyListFromUseCase() {
		PickupNotificationAdapter adapter = new PickupNotificationAdapter(recordNotificationUseCase);
		when(recordNotificationUseCase.record(any())).thenReturn(List.of());

		List<Long> result = adapter.recordNotifications(new NotificationRequest(502L, null, "HELP", List.of()));

		assertThat(result).isNotNull().isEmpty();
		ArgumentCaptor<com.example.pickupservice.notification.application.port.in.NotificationRequest> captor =
				ArgumentCaptor.forClass(com.example.pickupservice.notification.application.port.in.NotificationRequest.class);
		verify(recordNotificationUseCase).record(captor.capture());
		assertThat(captor.getValue().sessionId()).isEqualTo(502L);
		assertThat(captor.getValue().callId()).isNull();
		assertThat(captor.getValue().type()).isEqualTo("HELP");
		assertThat(captor.getValue().receiverUserIds()).isEmpty();
	}
}
