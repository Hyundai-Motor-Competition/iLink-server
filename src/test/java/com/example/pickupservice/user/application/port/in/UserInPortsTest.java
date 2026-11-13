package com.example.pickupservice.user.application.port.in;

import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class UserInPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다")
	void inPortInterfacesAndCommandResultRecordsExist() {
		verifyWebUseCases();
		verifyModuleUseCases();
		verifyWebRecords();
		verifyModuleRecords();
		verifyDeviceStatusValues();
	}

	@Test
	@DisplayName("web용 유스케이스 6개는 interface이고 설계서 시그니처의 메서드 하나씩만 갖는다")
	void webUseCasesHaveDesignSignatures() {
		verifyWebUseCases();
	}

	@Test
	@DisplayName("다른 모듈용 유스케이스 6개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void moduleUseCasesHaveDesignSignatures() {
		verifyModuleUseCases();
	}

	@Test
	@DisplayName("web용 Query · Command · Result는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void webRecordsHaveDesignComponents() {
		verifyWebRecords();
	}

	@Test
	@DisplayName("다른 모듈용 Command · Result · Info는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void moduleRecordsHaveDesignComponents() {
		verifyModuleRecords();
	}

	@Test
	@DisplayName("DeviceStatus는 CONNECTED · INVITE_PENDING · INVITE_EXPIRED · NONE 네 값을 갖는다")
	void deviceStatusHasFourValues() {
		verifyDeviceStatusValues();
	}

	private void verifyWebUseCases() {
		assertUseCase(GetMyFamilyUseCase.class,
				method("getMyFamily", FamilyResult.class.getName(), GetMyFamilyQuery.class));
		assertUseCase(RegisterMemberUseCase.class,
				method("registerMember", "long", RegisterMemberCommand.class));
		assertUseCase(UpdateMemberUseCase.class,
				method("updateMember", MemberResult.class.getName(), UpdateMemberCommand.class));
		assertUseCase(RemoveMemberUseCase.class,
				method("removeMember", "void", RemoveMemberCommand.class));
		assertUseCase(UnlinkVehicleUseCase.class,
				method("unlinkVehicle", "void", UnlinkVehicleCommand.class));
		assertUseCase(ChangeCallModeUseCase.class,
				method("changeCallMode", CallModeResult.class.getName(), ChangeCallModeCommand.class));
	}

	private void verifyModuleUseCases() {
		assertUseCase(CreateGuardianUseCase.class,
				method("createGuardian", GuardianAccount.class.getName(), CreateGuardianCommand.class));
		assertUseCase(ChangeProfileImageUseCase.class,
				method("changeProfileImage", "void", ChangeProfileImageCommand.class));
		assertUseCase(RegisterVehicleUseCase.class,
				method("registerVehicle", "long", String.class, String.class));
		assertUseCase(LoadVehicleUseCase.class,
				method("findVehicle", generic(Optional.class, VehicleInfo.class), long.class),
				method("findLinkedVehicleByVin", generic(Optional.class, VehicleInfo.class), String.class));
		assertUseCase(LoadMemberUseCase.class,
				method("findMember", generic(Optional.class, MemberInfo.class), long.class),
				method("findMembers", generic(List.class, MemberInfo.class), long.class));
		assertUseCase(LoadFamilyUseCase.class,
				method("findFamily", FamilyInfo.class.getName(), long.class));
	}

	private void verifyWebRecords() {
		assertRecord(GetMyFamilyQuery.class, components(
				"familyId", "long"));
		assertRecord(FamilyResult.class, components(
				"familyId", "long",
				"joinCallMode", JoinCallMode.class.getName(),
				"vehicle", VehicleSummary.class.getName(),
				"members", generic(List.class, MemberResult.class),
				"childLocationEnabled", "boolean"));
		assertRecord(VehicleSummary.class, components(
				"vehicleId", "long",
				"model", String.class.getName(),
				"plateNumberMasked", String.class.getName()));
		assertRecord(MemberResult.class, components(
				"userId", "long",
				"role", Role.class.getName(),
				"name", String.class.getName(),
				"phone", String.class.getName(),
				"deviceStatus", DeviceStatus.class.getName(),
				"profileImageUrl", String.class.getName()));
		assertRecord(RegisterMemberCommand.class, components(
				"familyId", "long",
				"requesterRole", Role.class.getName(),
				"role", Role.class.getName(),
				"name", String.class.getName(),
				"phone", String.class.getName(),
				"profileImage", ProfileImage.class.getName()));
		assertRecord(UpdateMemberCommand.class, components(
				"familyId", "long",
				"requesterId", "long",
				"requesterRole", Role.class.getName(),
				"userId", "long",
				"name", String.class.getName(),
				"phone", String.class.getName(),
				"role", Role.class.getName()));
		assertRecord(RemoveMemberCommand.class, components(
				"familyId", "long",
				"requesterId", "long",
				"userId", "long"));
		assertRecord(UnlinkVehicleCommand.class, components(
				"familyId", "long",
				"vehicleId", "long"));
		assertRecord(ChangeCallModeCommand.class, components(
				"familyId", "long",
				"joinCallMode", JoinCallMode.class.getName()));
		assertRecord(CallModeResult.class, components(
				"joinCallMode", JoinCallMode.class.getName(),
				"settingsVersion", "int"));
	}

	private void verifyModuleRecords() {
		assertRecord(CreateGuardianCommand.class, components(
				"vehicleId", "long",
				"phone", String.class.getName(),
				"name", String.class.getName(),
				"profileImage", ProfileImage.class.getName()));
		assertRecord(ChangeProfileImageCommand.class, components(
				"userId", "long",
				"profileImage", ProfileImage.class.getName()));
		assertRecord(GuardianAccount.class, components(
				"familyId", "long",
				"userId", "long"));
		assertRecord(VehicleInfo.class, components(
				"vehicleId", "long",
				"familyId", Long.class.getName(),
				"vin", String.class.getName(),
				"model", String.class.getName(),
				"plateNumber", String.class.getName(),
				"linked", "boolean"));
		assertRecord(MemberInfo.class, components(
				"userId", "long",
				"familyId", "long",
				"role", Role.class.getName(),
				"name", String.class.getName(),
				"phone", String.class.getName(),
				"profileImageUrl", String.class.getName()));
		assertRecord(FamilyInfo.class, components(
				"familyId", "long",
				"joinCallMode", JoinCallMode.class.getName(),
				"settingsVersion", "int",
				"childLocationEnabled", "boolean"));
	}

	private void verifyDeviceStatusValues() {
		assertThat(DeviceStatus.values())
				.extracting(DeviceStatus::name)
				.containsExactly("CONNECTED", "INVITE_PENDING", "INVITE_EXPIRED", "NONE");
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes) {
	}

	private static ExpectedMethod method(String name, String genericReturnType, Class<?>... parameterTypes) {
		return new ExpectedMethod(name, genericReturnType, parameterTypes);
	}

	private static String generic(Class<?> raw, Class<?> argument) {
		return raw.getName() + "<" + argument.getName() + ">";
	}

	private static void assertUseCase(Class<?> useCase, ExpectedMethod... expectedMethods) {
		assertThat(useCase.isInterface())
				.as("%s는 interface여야 한다", useCase.getSimpleName())
				.isTrue();

		List<String> abstractMethodNames = Arrays.stream(useCase.getDeclaredMethods())
				.filter(m -> Modifier.isAbstract(m.getModifiers()))
				.map(Method::getName)
				.sorted()
				.toList();
		List<String> expectedNames = Arrays.stream(expectedMethods)
				.map(ExpectedMethod::name)
				.sorted()
				.toList();
		assertThat(abstractMethodNames)
				.as("%s의 추상 메서드 목록", useCase.getSimpleName())
				.isEqualTo(expectedNames);

		for (ExpectedMethod expected : expectedMethods) {
			Method actual;
			try {
				actual = useCase.getMethod(expected.name(), expected.parameterTypes());
			} catch (NoSuchMethodException e) {
				fail("%s에 %s%s 메서드가 없다", useCase.getSimpleName(), expected.name(),
						Arrays.toString(expected.parameterTypes()));
				return;
			}
			assertThat(actual.getGenericReturnType().getTypeName())
					.as("%s.%s 반환 타입", useCase.getSimpleName(), expected.name())
					.isEqualTo(expected.genericReturnType());
		}
	}

	private static Map<String, String> components(String... nameTypePairs) {
		Map<String, String> result = new LinkedHashMap<>();
		for (int i = 0; i < nameTypePairs.length; i += 2) {
			result.put(nameTypePairs[i], nameTypePairs[i + 1]);
		}
		return result;
	}

	private static void assertRecord(Class<?> type, Map<String, String> expectedComponents) {
		assertThat(type.isRecord())
				.as("%s는 record여야 한다", type.getSimpleName())
				.isTrue();

		RecordComponent[] actual = type.getRecordComponents();
		assertThat(Arrays.stream(actual).map(RecordComponent::getName).toList())
				.as("%s 컴포넌트 이름 · 순서", type.getSimpleName())
				.containsExactlyElementsOf(expectedComponents.keySet());
		assertThat(Arrays.stream(actual).map(c -> c.getGenericType().getTypeName()).toList())
				.as("%s 컴포넌트 타입 · 순서", type.getSimpleName())
				.containsExactlyElementsOf(expectedComponents.values());
	}
}
