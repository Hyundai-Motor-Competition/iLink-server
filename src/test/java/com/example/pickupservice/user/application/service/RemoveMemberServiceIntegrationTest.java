package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.GetMyFamilyQuery;
import com.example.pickupservice.user.application.port.in.GetMyFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.in.RegisterMemberCommand;
import com.example.pickupservice.user.application.port.in.RegisterMemberUseCase;
import com.example.pickupservice.user.application.port.in.RemoveMemberCommand;
import com.example.pickupservice.user.application.port.in.RemoveMemberUseCase;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
@Import(UserSeed.class)
class RemoveMemberServiceIntegrationTest extends IntegrationTestSupport {

	private final RemoveMemberUseCase removeMemberUseCase;
	private final RegisterMemberUseCase registerMemberUseCase;
	private final GetMyFamilyUseCase getMyFamilyUseCase;
	private final LoadMemberUseCase loadMemberUseCase;
	private final UserSeed userSeed;

	private UserSeed.Result seeded;

	@BeforeEach
	void setUp() {
		seeded = userSeed.seed();
	}

	@AfterEach
	void tearDown() {
		userSeed.clear(seeded);
	}

	@Test
	@DisplayName("자녀 외 멤버 5명 중 1명을 해제하면 한도에 자리가 생겨 새 자녀 외 멤버 등록이 성공한다")
	void registeringNonChildMemberSucceedsAfterOneOfFiveIsRemoved() {
		long teacherId = register(Role.TEACHER, "박선생", "+821033334444");
		register(Role.CO_GUARDIAN, "이공동", "+821011112222");
		register(Role.DELEGATE, "최대리", "+821055556666");
		register(Role.DELEGATE, "정대리", "+821077778888");
		assertThatThrownBy(() -> register(Role.TEACHER, "한선생", "+821099990000"))
				.as("해제 전에는 자녀 외 5명이라 한도 초과")
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.code()).isEqualTo("MEMBER_LIMIT_EXCEEDED"));

		removeMemberUseCase.removeMember(new RemoveMemberCommand(seeded.familyId(), seeded.guardianId(), teacherId));

		long newMemberId = register(Role.TEACHER, "한선생", "+821099990000");
		assertThat(getMyFamilyUseCase.getMyFamily(new GetMyFamilyQuery(seeded.familyId())).members())
				.as("새 멤버가 가족 조회에 나온다")
				.extracting(MemberResult::userId)
				.contains(newMemberId);
	}

	@Test
	@DisplayName("해제된 멤버는 가족 조회(FAM-01) members에 나오지 않는다")
	void removedMemberIsNotInMyFamilyMembers() {
		long delegateId = register(Role.DELEGATE, "최대리", "+821055556666");

		removeMemberUseCase.removeMember(new RemoveMemberCommand(seeded.familyId(), seeded.guardianId(), delegateId));
		removeMemberUseCase.removeMember(
				new RemoveMemberCommand(seeded.familyId(), seeded.guardianId(), seeded.seoyunId()));

		assertThat(getMyFamilyUseCase.getMyFamily(new GetMyFamilyQuery(seeded.familyId())).members())
				.extracting(MemberResult::userId)
				.containsExactly(seeded.guardianId(), seeded.minjunId());
	}

	@Test
	@DisplayName("해제된 멤버는 다른 모듈용 findMembers에서 빠지고 findMember(userId)로는 이름 · role이 그대로 읽힌다")
	void removedMemberIsExcludedFromFindMembersButReadableByFindMember() {
		long delegateId = register(Role.DELEGATE, "최대리", "+821055556666");

		removeMemberUseCase.removeMember(new RemoveMemberCommand(seeded.familyId(), seeded.guardianId(), delegateId));

		assertThat(loadMemberUseCase.findMembers(seeded.familyId()))
				.extracting(MemberInfo::userId)
				.containsExactly(seeded.guardianId(), seeded.minjunId(), seeded.seoyunId());
		assertThat(loadMemberUseCase.findMember(delegateId)).get()
				.satisfies(member -> {
					assertThat(member.name()).isEqualTo("최대리");
					assertThat(member.role()).isEqualTo(Role.DELEGATE);
					assertThat(member.familyId()).isEqualTo(seeded.familyId());
				});
	}

	private long register(Role role, String name, String phone) {
		return registerMemberUseCase.registerMember(
				new RegisterMemberCommand(seeded.familyId(), Role.GUARDIAN, role, name, phone));
	}

}
