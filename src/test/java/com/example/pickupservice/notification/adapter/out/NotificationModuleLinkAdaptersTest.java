package com.example.pickupservice.notification.adapter.out;

import com.example.pickupservice.auth.application.port.in.LoadPushTargetUseCase;
import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.notification.adapter.out.auth.NotificationPushTargetAdapter;
import com.example.pickupservice.notification.adapter.out.pickup.NotificationCallProgressAdapter;
import com.example.pickupservice.notification.adapter.out.pickup.NotificationSessionFamilyAdapter;
import com.example.pickupservice.notification.adapter.out.user.NotificationMemberNameAdapter;
import com.example.pickupservice.pickup.application.port.in.CallProgressUseCase;
import com.example.pickupservice.pickup.application.port.in.FindSessionFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationModuleLinkAdaptersTest {

	private static final long GUARDIAN_ID = 11L;
	private static final long CHILD_ID = 21L;
	private static final long MISSING_USER_ID = 99L;
	private static final long CHILD_DEVICE_ID = 902L;
	private static final long CALL_ID = 3001L;
	private static final long SESSION_ID = 501L;
	private static final long FAMILY_ID = 1L;
	private static final Instant AT = Instant.parse("2026-11-09T06:00:05Z");

	@Mock
	private LoadPushTargetUseCase loadPushTargetUseCase;
	@Mock
	private LoadMemberUseCase loadMemberUseCase;
	@Mock
	private CallProgressUseCase callProgressUseCase;
	@Mock
	private FindSessionFamilyUseCase findSessionFamilyUseCase;

	@Test
	@DisplayName("LoadPushTargetPort.loadPushTargets는 auth LoadPushTargetUseCase.findPushTargets에 같은 userIds를 넘기고 결과를 그대로 돌려준다")
	void loadPushTargetsDelegatesToAuthFindPushTargets() {
		List<Long> userIds = List.of(GUARDIAN_ID, CHILD_ID);
		List<PushTarget> targets = List.of(
				new PushTarget(GUARDIAN_ID, 901L, Platform.ANDROID, "guardian-token"),
				new PushTarget(CHILD_ID, CHILD_DEVICE_ID, Platform.ANDROID, "child-token"));
		when(loadPushTargetUseCase.findPushTargets(userIds)).thenReturn(targets);
		NotificationPushTargetAdapter adapter = new NotificationPushTargetAdapter(loadPushTargetUseCase);

		List<PushTarget> result = adapter.loadPushTargets(userIds);

		assertThat(result).isEqualTo(targets);
		verify(loadPushTargetUseCase).findPushTargets(userIds);
	}

	@Test
	@DisplayName("LoadPushTargetPort.loadPushTargets는 auth가 빈 목록을 주면 빈 목록을 돌려준다")
	void loadPushTargetsReturnsEmptyListWhenAuthReturnsEmpty() {
		when(loadPushTargetUseCase.findPushTargets(List.of(MISSING_USER_ID))).thenReturn(List.of());
		NotificationPushTargetAdapter adapter = new NotificationPushTargetAdapter(loadPushTargetUseCase);

		assertThat(adapter.loadPushTargets(List.of(MISSING_USER_ID))).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("LoadPushTargetPort.loadPushTarget은 auth LoadPushTargetUseCase.findPushTargetByDeviceId에 위임해 결과를 그대로 돌려준다")
	void loadPushTargetDelegatesToAuthFindPushTargetByDeviceId() {
		PushTarget target = new PushTarget(CHILD_ID, CHILD_DEVICE_ID, Platform.ANDROID, "child-token");
		when(loadPushTargetUseCase.findPushTargetByDeviceId(CHILD_DEVICE_ID)).thenReturn(Optional.of(target));
		NotificationPushTargetAdapter adapter = new NotificationPushTargetAdapter(loadPushTargetUseCase);

		assertThat(adapter.loadPushTarget(CHILD_DEVICE_ID)).contains(target);
		verify(loadPushTargetUseCase).findPushTargetByDeviceId(CHILD_DEVICE_ID);
	}

	@Test
	@DisplayName("LoadPushTargetPort.loadPushTarget은 auth가 빈 값을 주면 빈 값을 돌려준다")
	void loadPushTargetReturnsEmptyWhenAuthReturnsEmpty() {
		when(loadPushTargetUseCase.findPushTargetByDeviceId(CHILD_DEVICE_ID)).thenReturn(Optional.empty());
		NotificationPushTargetAdapter adapter = new NotificationPushTargetAdapter(loadPushTargetUseCase);

		assertThat(adapter.loadPushTarget(CHILD_DEVICE_ID)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("LoadMemberNamePort.loadNames는 user LoadMemberUseCase.findMember로 사용자마다 이름을 찾아 userId별 Map으로 돌려준다")
	void loadNamesDelegatesToUserFindMemberPerUser() {
		when(loadMemberUseCase.findMember(GUARDIAN_ID)).thenReturn(Optional.of(member(GUARDIAN_ID, Role.GUARDIAN, "김보호")));
		when(loadMemberUseCase.findMember(CHILD_ID)).thenReturn(Optional.of(member(CHILD_ID, Role.CHILD, "민준")));
		NotificationMemberNameAdapter adapter = new NotificationMemberNameAdapter(loadMemberUseCase);

		Map<Long, String> names = adapter.loadNames(List.of(GUARDIAN_ID, CHILD_ID));

		assertThat(names).containsExactlyInAnyOrderEntriesOf(Map.of(GUARDIAN_ID, "김보호", CHILD_ID, "민준"));
	}

	@Test
	@DisplayName("LoadMemberNamePort.loadNames는 user에 없는 사용자는 Map에서 뺀다")
	void loadNamesOmitsUsersNotFoundInUser() {
		when(loadMemberUseCase.findMember(CHILD_ID)).thenReturn(Optional.of(member(CHILD_ID, Role.CHILD, "민준")));
		when(loadMemberUseCase.findMember(MISSING_USER_ID)).thenReturn(Optional.empty());
		NotificationMemberNameAdapter adapter = new NotificationMemberNameAdapter(loadMemberUseCase);

		Map<Long, String> names = adapter.loadNames(List.of(CHILD_ID, MISSING_USER_ID));

		assertThat(names).containsExactlyInAnyOrderEntriesOf(Map.of(CHILD_ID, "민준"));
	}

	@Test
	@DisplayName("LoadMemberNamePort.loadNames에 빈 목록을 주면 user를 부르지 않고 빈 Map을 돌려준다")
	void loadNamesReturnsEmptyMapWithoutCallingUserForEmptyList() {
		NotificationMemberNameAdapter adapter = new NotificationMemberNameAdapter(loadMemberUseCase);

		assertThat(adapter.loadNames(List.of())).isNotNull().isEmpty();
		verifyNoMoreInteractions(loadMemberUseCase);
	}

	@Test
	@DisplayName("CallProgressPort.markCallSent는 pickup CallProgressUseCase.markCallSent에 callId와 시각을 그대로 넘긴다")
	void markCallSentDelegatesToPickup() {
		NotificationCallProgressAdapter adapter = new NotificationCallProgressAdapter(callProgressUseCase);

		adapter.markCallSent(CALL_ID, AT);

		verify(callProgressUseCase).markCallSent(CALL_ID, AT);
		verifyNoMoreInteractions(callProgressUseCase);
	}

	@Test
	@DisplayName("CallProgressPort.markCallDelivered는 pickup CallProgressUseCase.markCallDelivered에 callId와 시각을 그대로 넘긴다")
	void markCallDeliveredDelegatesToPickup() {
		NotificationCallProgressAdapter adapter = new NotificationCallProgressAdapter(callProgressUseCase);

		adapter.markCallDelivered(CALL_ID, AT);

		verify(callProgressUseCase).markCallDelivered(CALL_ID, AT);
		verifyNoMoreInteractions(callProgressUseCase);
	}

	@Test
	@DisplayName("CallProgressPort.isCallAcknowledged는 pickup CallProgressUseCase.isCallAcknowledged 결과(true · false)를 그대로 돌려준다")
	void isCallAcknowledgedReturnsPickupAnswerAsIs() {
		when(callProgressUseCase.isCallAcknowledged(CALL_ID)).thenReturn(true);
		when(callProgressUseCase.isCallAcknowledged(CALL_ID + 1)).thenReturn(false);
		NotificationCallProgressAdapter adapter = new NotificationCallProgressAdapter(callProgressUseCase);

		assertThat(adapter.isCallAcknowledged(CALL_ID)).isTrue();
		assertThat(adapter.isCallAcknowledged(CALL_ID + 1)).isFalse();
	}

	@Test
	@DisplayName("LoadSessionFamilyPort.loadFamilyId는 pickup FindSessionFamilyUseCase.findFamilyId 결과(있음 · 없음)를 그대로 돌려준다")
	void loadFamilyIdDelegatesToPickupFindSessionFamily() {
		when(findSessionFamilyUseCase.findFamilyId(SESSION_ID)).thenReturn(Optional.of(FAMILY_ID));
		when(findSessionFamilyUseCase.findFamilyId(SESSION_ID + 1)).thenReturn(Optional.empty());
		NotificationSessionFamilyAdapter adapter = new NotificationSessionFamilyAdapter(findSessionFamilyUseCase);

		assertThat(adapter.loadFamilyId(SESSION_ID)).contains(FAMILY_ID);
		assertThat(adapter.loadFamilyId(SESSION_ID + 1)).isNotNull().isEmpty();
	}

	private static MemberInfo member(long userId, Role role, String name) {
		return new MemberInfo(userId, 1L, role, name, "010-0000-0000");
	}
}
