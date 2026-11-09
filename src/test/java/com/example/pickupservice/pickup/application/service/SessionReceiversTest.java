package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionReceiversTest {

	private static final Instant NOW = Instant.parse("2026-11-09T09:00:00Z");
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long DELEGATE_ID = 35L;
	private static final long TEACHER_ID = 36L;
	private static final long MINJUN_ID = 33L;
	private static final long SEOYUN_ID = 34L;
	private static final long SIBLING_NOT_IN_SESSION_ID = 37L;

	@Test
	@DisplayName("받는 사람 보호자는 가족의 GUARDIAN · CO_GUARDIAN · DELEGATE이고 TEACHER · CHILD는 빠진다")
	void guardiansAreGuardianCoGuardianAndDelegateWithoutTeacherAndChild() {
		List<Long> receivers = SessionReceivers.guardians(members());

		assertThat(receivers).containsExactlyInAnyOrder(GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID);
	}

	@Test
	@DisplayName("보호자와 세션 자녀에게 보낼 때 받는 사람은 GUARDIAN · CO_GUARDIAN · DELEGATE와 세션 자녀이고 TEACHER와 세션에 없는 자녀는 빠진다")
	void guardiansAndSessionChildrenExcludeTeacherAndChildrenOutsideSession() {
		List<Long> receivers = SessionReceivers.guardiansAndSessionChildren(session(), members());

		assertThat(receivers).containsExactlyInAnyOrder(GUARDIAN_ID, CO_GUARDIAN_ID, DELEGATE_ID, MINJUN_ID, SEOYUN_ID);
	}

	private static List<MemberInfo> members() {
		return List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1111-2222"),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이공동", "010-3333-4444"),
				new MemberInfo(DELEGATE_ID, FAMILY_ID, Role.DELEGATE, "박대리", "010-5555-6666"),
				new MemberInfo(TEACHER_ID, FAMILY_ID, Role.TEACHER, "최선생", "010-7777-8888"),
				new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				new MemberInfo(SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null),
				new MemberInfo(SIBLING_NOT_IN_SESSION_ID, FAMILY_ID, Role.CHILD, "하준", null));
	}

	private static PickupSession session() {
		List<SessionChild> children = List.of(
				SessionChild.restore(7001L, MINJUN_ID, 801L, ChildPhase.WAITING, 0, null, null, null, null, null),
				SessionChild.restore(7002L, SEOYUN_ID, 802L, ChildPhase.WAITING, 0, null, null, null, null, null));
		return PickupSession.restore(501L, 77L, FAMILY_ID, 21L, "정문 앞", new LatLng(37.501400, 127.039950),
				new LatLng(37.501301, 127.039901), LocalDate.of(2026, 11, 9), SessionStatus.ACTIVE,
				VehiclePhase.APPROACHING, SpotStatus.CONFIRMED, 3, 0, 1, 0, false, NOW.plusSeconds(600), 2400,
				NOW.minusSeconds(5), null, null, null, null, null, null, null, null, children);
	}

}
