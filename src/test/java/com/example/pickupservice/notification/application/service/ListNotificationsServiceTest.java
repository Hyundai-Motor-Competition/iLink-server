package com.example.pickupservice.notification.application.service;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.notification.application.port.in.NotificationListQuery;
import com.example.pickupservice.notification.application.port.in.NotificationView;
import com.example.pickupservice.notification.application.port.out.LoadMemberNamePort;
import com.example.pickupservice.notification.application.port.out.LoadNotificationPort;
import com.example.pickupservice.notification.application.port.out.LoadPushTargetPort;
import com.example.pickupservice.notification.application.port.out.LoadSessionFamilyPort;
import com.example.pickupservice.notification.domain.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListNotificationsServiceTest {

	private static final long FAMILY_ID = 1L;
	private static final long OTHER_FAMILY_ID = 2L;
	private static final long SESSION_ID = 501L;
	private static final long CALL_ID = 3001L;
	private static final long CHILD_DEVICE_ID = 902L;
	private static final long GUARDIAN_DEVICE_ID = 903L;
	private static final long CHILD_USER_ID = 21L;
	private static final long GUARDIAN_USER_ID = 11L;
	private static final Instant FIRST_SENT_AT = Instant.parse("2026-11-09T06:00:00Z");
	private static final Instant SECOND_SENT_AT = Instant.parse("2026-11-09T06:05:00Z");
	private static final Instant DELIVERED_AT = Instant.parse("2026-11-09T06:00:05Z");

	@Mock
	private LoadNotificationPort loadNotificationPort;
	@Mock
	private LoadPushTargetPort loadPushTargetPort;
	@Mock
	private LoadMemberNamePort loadMemberNamePort;

	@Mock
	private LoadSessionFamilyPort loadSessionFamilyPort;

	private ListNotificationsService service;

	@BeforeEach
	void setUp() {
		service = new ListNotificationsService(loadNotificationPort, loadPushTargetPort, loadMemberNamePort,
				loadSessionFamilyPort);
		lenient().when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.of(FAMILY_ID));
	}

	@Test
	@DisplayName("알림 이력은 요청자 가족의 세션이 아니면 NOT_FOUND로 거절하고 알림을 읽지 않는다")
	void listRejectsOtherFamilySessionWithNotFound() {
		when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.of(OTHER_FAMILY_ID));

		assertThatThrownBy(() -> service.list(new NotificationListQuery(principal(), SESSION_ID)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND.name()));

		verify(loadNotificationPort, never()).loadBySession(anyLong());
	}

	@Test
	@DisplayName("알림 이력은 없는 세션이면 NOT_FOUND로 거절하고 알림을 읽지 않는다")
	void listRejectsMissingSessionWithNotFound() {
		when(loadSessionFamilyPort.loadFamilyId(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.list(new NotificationListQuery(principal(), SESSION_ID)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_FOUND.name()));

		verify(loadNotificationPort, never()).loadBySession(anyLong());
	}

	@Test
	@DisplayName("알림 이력은 받는 사람 이름과 attempt를 채워 돌려준다")
	void listFillsReceiverNameAndAttempt() {
		Notification first = Notification.restore(7001L, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID, FIRST_SENT_AT,
				DELIVERED_AT, null);
		Notification guardianRow = Notification.restore(7002L, SESSION_ID, null, "HELP", GUARDIAN_DEVICE_ID,
				FIRST_SENT_AT, null, null);
		Notification resent = Notification.restore(7003L, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID, SECOND_SENT_AT,
				null, null);
		when(loadNotificationPort.loadBySession(SESSION_ID)).thenReturn(List.of(first, guardianRow, resent));
		when(loadPushTargetPort.loadPushTargetIncludingRevoked(CHILD_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "t1")));
		when(loadPushTargetPort.loadPushTargetIncludingRevoked(GUARDIAN_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(GUARDIAN_USER_ID, GUARDIAN_DEVICE_ID, Platform.IOS, "t2")));
		when(loadMemberNamePort.loadNames(anyList()))
				.thenReturn(Map.of(CHILD_USER_ID, "지우", GUARDIAN_USER_ID, "엄마"));

		List<NotificationView> views = service.list(new NotificationListQuery(principal(), SESSION_ID));

		assertThat(views).hasSize(3);
		NotificationView view1 = views.get(0);
		assertThat(view1.notificationId()).isEqualTo(7001L);
		assertThat(view1.type()).isEqualTo("CALL");
		assertThat(view1.receiverName()).isEqualTo("지우");
		assertThat(view1.title()).isEqualTo("이제 나와요");
		assertThat(view1.sentAt()).isEqualTo(FIRST_SENT_AT);
		assertThat(view1.deliveredAt()).isEqualTo(DELIVERED_AT);
		assertThat(view1.openedAt()).isNull();
		assertThat(view1.attempt()).isEqualTo(1);

		NotificationView view2 = views.get(1);
		assertThat(view2.notificationId()).isEqualTo(7002L);
		assertThat(view2.receiverName()).isEqualTo("엄마");
		assertThat(view2.title()).isEqualTo("도움 요청");
		assertThat(view2.deliveredAt()).isNull();
		assertThat(view2.attempt()).isEqualTo(1);

		NotificationView view3 = views.get(2);
		assertThat(view3.notificationId()).isEqualTo(7003L);
		assertThat(view3.receiverName()).isEqualTo("지우");
		assertThat(view3.sentAt()).isEqualTo(SECOND_SENT_AT);
		assertThat(view3.attempt()).isEqualTo(2);
	}

	@Test
	@DisplayName("알림 이력의 받는 사람 이름은 폐기된 기기도 찾는 폐기 포함 조회로 채우고 살아 있는 기기만 보는 조회는 쓰지 않는다")
	void listFillsReceiverNameOfRevokedDeviceWithRevokedInclusiveLookupOnly() {
		Notification row = Notification.restore(7001L, SESSION_ID, CALL_ID, "CALL", CHILD_DEVICE_ID, FIRST_SENT_AT,
				DELIVERED_AT, null);
		when(loadNotificationPort.loadBySession(SESSION_ID)).thenReturn(List.of(row));
		when(loadPushTargetPort.loadPushTargetIncludingRevoked(CHILD_DEVICE_ID))
				.thenReturn(Optional.of(new PushTarget(CHILD_USER_ID, CHILD_DEVICE_ID, Platform.ANDROID, "t1")));
		when(loadMemberNamePort.loadNames(anyList())).thenReturn(Map.of(CHILD_USER_ID, "지우"));

		List<NotificationView> views = service.list(new NotificationListQuery(principal(), SESSION_ID));

		assertThat(views).hasSize(1);
		assertThat(views.get(0).receiverName()).isEqualTo("지우");
		verify(loadPushTargetPort, never()).loadPushTarget(anyLong());
	}

	private static AuthPrincipal principal() {
		return new AuthPrincipal(PrincipalType.USER, GUARDIAN_USER_ID, FAMILY_ID, Role.GUARDIAN);
	}
}
