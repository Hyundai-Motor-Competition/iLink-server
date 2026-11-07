package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.schedule.application.port.in.VehicleChild;
import com.example.pickupservice.schedule.application.port.in.VehicleSettingsSummary;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static com.example.pickupservice.schedule.fixture.JoinCallModeFixture.joinCallModeOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetVehicleSettingsSummaryServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long GUARDIAN_ID = 100L;
	private static final long CO_GUARDIAN_ID = 110L;
	private static final long DELEGATE_ID = 120L;
	private static final long MINJUN_ID = 131L;
	private static final long SEOYEON_ID = 132L;

	@Mock
	private LoadMemberUseCase loadMemberUseCase;

	@Mock
	private LoadFamilyUseCase loadFamilyUseCase;

	@InjectMocks
	private GetVehicleSettingsSummaryService service;

	// #36

	@Test
	@DisplayName("차량용 설정 요약은 GUARDIAN 이름의 가운데 글자를 가려서 돌려준다")
	void summaryMasksGuardianName() {
		givenMembers(
				member(CO_GUARDIAN_ID, Role.CO_GUARDIAN, "이공동"),
				member(GUARDIAN_ID, Role.GUARDIAN, "김보호"),
				member(MINJUN_ID, Role.CHILD, "민준"));
		givenFamily(JoinCallMode.AUTO);

		VehicleSettingsSummary summary = service.getVehicleSettingsSummary(FAMILY_ID);

		assertThat(summary.guardianNameMasked()).isEqualTo("김*호");
	}

	@Test
	@DisplayName("차량용 설정 요약의 children은 CHILD 회원만 userId와 이름으로 회원 순서대로 담는다")
	void summaryContainsOnlyChildMembers() {
		givenMembers(
				member(GUARDIAN_ID, Role.GUARDIAN, "김보호"),
				member(MINJUN_ID, Role.CHILD, "민준"),
				member(CO_GUARDIAN_ID, Role.CO_GUARDIAN, "이공동"),
				member(DELEGATE_ID, Role.DELEGATE, "박대리"),
				member(SEOYEON_ID, Role.CHILD, "서연"));
		givenFamily(JoinCallMode.AUTO);

		VehicleSettingsSummary summary = service.getVehicleSettingsSummary(FAMILY_ID);

		assertThat(summary.children()).containsExactly(
				new VehicleChild(MINJUN_ID, "민준"),
				new VehicleChild(SEOYEON_ID, "서연"));
	}

	@ParameterizedTest
	@EnumSource(JoinCallMode.class)
	@DisplayName("차량용 설정 요약은 가족의 joinCallMode를 같은 이름의 schedule JoinCallMode로 바꿔 담는다")
	void summaryConvertsJoinCallModeOfFamilyToScheduleEnum(JoinCallMode familyMode) {
		givenMembers(
				member(GUARDIAN_ID, Role.GUARDIAN, "김보호"),
				member(MINJUN_ID, Role.CHILD, "민준"));
		givenFamily(familyMode);

		VehicleSettingsSummary summary = service.getVehicleSettingsSummary(FAMILY_ID);

		Object joinCallMode = summary.joinCallMode();
		assertThat(joinCallMode).isInstanceOf(com.example.pickupservice.schedule.domain.JoinCallMode.class);
		assertThat(((Enum<?>) joinCallMode).name()).isEqualTo(familyMode.name());
	}

	@Test
	@DisplayName("차량용 설정 요약은 설계서 예시와 같은 값을 돌려준다")
	void summaryMatchesDesignExample() {
		givenMembers(
				member(GUARDIAN_ID, Role.GUARDIAN, "김보호"),
				member(MINJUN_ID, Role.CHILD, "민준"));
		givenFamily(JoinCallMode.AUTO);

		VehicleSettingsSummary summary = service.getVehicleSettingsSummary(FAMILY_ID);

		assertThat(summary).isEqualTo(new VehicleSettingsSummary("김*호",
				List.of(new VehicleChild(MINJUN_ID, "민준")), joinCallModeOf(VehicleSettingsSummary.class, "AUTO")));
	}

	private void givenMembers(MemberInfo... members) {
		given(loadMemberUseCase.findMembers(FAMILY_ID)).willReturn(List.of(members));
	}

	private void givenFamily(JoinCallMode joinCallMode) {
		given(loadFamilyUseCase.findFamily(FAMILY_ID)).willReturn(new FamilyInfo(FAMILY_ID, joinCallMode, 1));
	}

	private static MemberInfo member(long userId, Role role, String name) {
		return new MemberInfo(userId, FAMILY_ID, role, name, "010-0000-0000");
	}
}
