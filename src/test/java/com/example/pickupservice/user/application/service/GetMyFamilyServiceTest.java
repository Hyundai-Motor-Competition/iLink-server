package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.application.port.in.FamilyResult;
import com.example.pickupservice.user.application.port.in.GetMyFamilyQuery;
import com.example.pickupservice.user.application.port.in.MemberResult;
import com.example.pickupservice.user.application.port.in.VehicleSummary;
import com.example.pickupservice.user.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.out.LoadMemberDeviceStatusPort;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.LoadVehiclePort;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.JoinCallMode;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetMyFamilyServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 101L;
	private static final long TEACHER_ID = 140L;
	private static final long CHILD_MINJUN_ID = 131L;
	private static final long CHILD_SEOYUN_ID = 132L;
	private static final long VEHICLE_ID = 77L;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	@Mock
	private LoadUserPort loadUserPort;

	@Mock
	private LoadVehiclePort loadVehiclePort;

	@Mock
	private LoadMemberDeviceStatusPort loadMemberDeviceStatusPort;

	@InjectMocks
	private GetMyFamilyService getMyFamilyService;

	@Test
	@DisplayName("기기 없이 등록한 TEACHER의 deviceStatus는 NONE이다")
	void teacherWithoutDeviceHasDeviceStatusNone() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0)));
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432")));
		lenient().when(loadVehiclePort.loadLinkedVehicleOfFamily(FAMILY_ID)).thenReturn(Optional.empty());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.willReturn(Map.of(GUARDIAN_ID, DeviceStatus.CONNECTED));

		FamilyResult result = getMyFamilyService.getMyFamily(new GetMyFamilyQuery(FAMILY_ID));

		MemberResult teacher = result.members().stream()
				.filter(member -> member.userId() == TEACHER_ID)
				.findFirst()
				.orElseThrow();
		assertThat(teacher.role()).isEqualTo(Role.TEACHER);
		assertThat(teacher.deviceStatus()).isEqualTo(DeviceStatus.NONE);
	}

	@Test
	@DisplayName("가족 조회는 멤버별 deviceStatus를 LoadMemberDeviceStatusPort 결과로 채운다")
	void fillsEachMemberDeviceStatusFromLoadMemberDeviceStatusPort() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0)));
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(CHILD_MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null),
				User.restore(CHILD_SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null)));
		lenient().when(loadVehiclePort.loadLinkedVehicleOfFamily(FAMILY_ID)).thenReturn(Optional.empty());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).willReturn(Map.of(
				GUARDIAN_ID, DeviceStatus.CONNECTED,
				CHILD_MINJUN_ID, DeviceStatus.INVITE_PENDING,
				CHILD_SEOYUN_ID, DeviceStatus.INVITE_EXPIRED));

		FamilyResult result = getMyFamilyService.getMyFamily(new GetMyFamilyQuery(FAMILY_ID));

		assertThat(result.members())
				.extracting(MemberResult::userId, MemberResult::deviceStatus)
				.containsExactlyInAnyOrder(
						tuple(GUARDIAN_ID, DeviceStatus.CONNECTED),
						tuple(CHILD_MINJUN_ID, DeviceStatus.INVITE_PENDING),
						tuple(CHILD_SEOYUN_ID, DeviceStatus.INVITE_EXPIRED));
		ArgumentCaptor<List<Long>> requestedUserIds = ArgumentCaptor.captor();
		verify(loadMemberDeviceStatusPort).loadDeviceStatuses(requestedUserIds.capture());
		assertThat(requestedUserIds.getValue())
				.containsExactlyInAnyOrder(GUARDIAN_ID, CHILD_MINJUN_ID, CHILD_SEOYUN_ID);
	}

	@Test
	@DisplayName("가족 조회(FAM-01) members[]는 LoadUserPort가 멤버를 섞인 순서로 돌려줘도 userId 오름차순이다")
	void membersAreSortedByUserIdAscending() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0)));
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(TEACHER_ID, FAMILY_ID, Role.TEACHER, "이선생", "+821098765432"),
				User.restore(CHILD_SEOYUN_ID, FAMILY_ID, Role.CHILD, "서윤", null),
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678"),
				User.restore(CHILD_MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null)));
		lenient().when(loadVehiclePort.loadLinkedVehicleOfFamily(FAMILY_ID)).thenReturn(Optional.empty());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList())).willReturn(Map.of());

		FamilyResult result = getMyFamilyService.getMyFamily(new GetMyFamilyQuery(FAMILY_ID));

		assertThat(result.members()).extracting(MemberResult::userId)
				.containsExactly(GUARDIAN_ID, CHILD_MINJUN_ID, CHILD_SEOYUN_ID, TEACHER_ID);
	}

	@Test
	@DisplayName("가족 조회는 연결 중인 차량이 없으면 vehicle을 null로 돌려준다")
	void returnsNullVehicleWhenFamilyHasNoLinkedVehicle() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0)));
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));
		given(loadVehiclePort.loadLinkedVehicleOfFamily(FAMILY_ID)).willReturn(Optional.empty());
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.willReturn(Map.of(GUARDIAN_ID, DeviceStatus.CONNECTED));

		FamilyResult result = getMyFamilyService.getMyFamily(new GetMyFamilyQuery(FAMILY_ID));

		assertThat(result.vehicle()).isNull();
		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
	}

	@Test
	@DisplayName("가족 조회는 연결 중인 차량이 있으면 vehicleId · model · 가린 번호판과 familyId · joinCallMode를 채운다")
	void fillsVehicleSummaryAndFamilyFieldsWhenFamilyHasLinkedVehicle() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.GUARDIAN_CONFIRM, 7)));
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));
		given(loadVehiclePort.loadLinkedVehicleOfFamily(FAMILY_ID)).willReturn(Optional.of(
				Vehicle.restore(VEHICLE_ID, FAMILY_ID, "KMHK381GFNU000001", "아이오닉 5", "12가 3456", null)));
		given(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.willReturn(Map.of(GUARDIAN_ID, DeviceStatus.CONNECTED));

		FamilyResult result = getMyFamilyService.getMyFamily(new GetMyFamilyQuery(FAMILY_ID));

		assertThat(result.familyId()).isEqualTo(FAMILY_ID);
		assertThat(result.joinCallMode()).isEqualTo(JoinCallMode.GUARDIAN_CONFIRM);
		assertThat(result.vehicle()).isEqualTo(new VehicleSummary(VEHICLE_ID, "아이오닉 5", "12가 ****"));
	}

	@Test
	@DisplayName("가족 조회(FAM-01)는 저장된 Family의 childLocationEnabled가 true면 FamilyResult.childLocationEnabled를 true로 옮긴다")
	void copiesChildLocationEnabledTrueFromStoredFamilyToFamilyResult() {
		given(loadFamilyPort.loadFamily(FAMILY_ID))
				.willReturn(Optional.of(Family.restore(FAMILY_ID, JoinCallMode.AUTO, 0, true)));
		given(loadUserPort.loadUsers(FAMILY_ID)).willReturn(List.of(
				User.restore(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "+821012345678")));
		lenient().when(loadVehiclePort.loadLinkedVehicleOfFamily(FAMILY_ID)).thenReturn(Optional.empty());
		lenient().when(loadMemberDeviceStatusPort.loadDeviceStatuses(anyList()))
				.thenReturn(Map.of(GUARDIAN_ID, DeviceStatus.CONNECTED));

		FamilyResult result = getMyFamilyService.getMyFamily(new GetMyFamilyQuery(FAMILY_ID));

		assertThat(result.childLocationEnabled()).isTrue();
	}
}
