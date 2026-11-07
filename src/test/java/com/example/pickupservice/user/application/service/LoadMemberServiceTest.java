package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoadMemberServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 101L;
	private static final long CHILD_ID = 131L;

	@Mock
	private LoadUserPort loadUserPort;

	@InjectMocks
	private LoadMemberService loadMemberService;

	@Test
	@DisplayName("user의 LoadMemberUseCase는 findMember · findMembers로 MemberInfo를 돌려주고 LoadFamilyUseCase는 findFamily로 FamilyInfo를 돌려준다")
	void findMemberReturnsMemberInfoFromLoadedUser() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));

		Optional<MemberInfo> result = loadMemberService.findMember(GUARDIAN_ID);

		assertThat(result).contains(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"));
	}

	@Test
	@DisplayName("findMember는 멤버가 없으면 빈 Optional을 돌려준다")
	void findMemberReturnsEmptyWhenUserMissing() {
		given(loadUserPort.loadUser(GUARDIAN_ID)).willReturn(Optional.empty());

		assertThat(loadMemberService.findMember(GUARDIAN_ID)).isEmpty();
	}

	@Test
	@DisplayName("findMembers는 그 가족의 멤버 전부를 MemberInfo로 돌려준다")
	void findMembersReturnsAllFamilyMembersAsMemberInfo() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null)));

		List<MemberInfo> result = loadMemberService.findMembers(FAMILY_ID);

		assertThat(result).containsExactly(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				new MemberInfo(CHILD_ID, FAMILY_ID, Role.CHILD, "민준", null));
	}

	@Test
	@DisplayName("findMembers는 멤버가 없는 가족이면 빈 목록을 돌려준다")
	void findMembersReturnsEmptyListWhenNoMembers() {
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of());

		assertThat(loadMemberService.findMembers(FAMILY_ID)).isEmpty();
	}
}
