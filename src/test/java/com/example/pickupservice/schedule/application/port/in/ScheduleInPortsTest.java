package com.example.pickupservice.schedule.application.port.in;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.MissingSetupItem;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import com.example.pickupservice.schedule.domain.JoinCallMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ScheduleInPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다")
	void inPortInterfacesAndCommandResultRecordsExist() {
		verifyWebUseCases();
		verifyModuleUseCases();
		verifyWebRecords();
		verifyModuleRecords();
		verifyEnumValues();
	}

	@Test
	@DisplayName("web용 유스케이스 8개는 interface이고 설계서 시그니처의 메서드 하나씩만 갖는다")
	void webUseCasesHaveDesignSignatures() {
		verifyWebUseCases();
	}

	@Test
	@DisplayName("다른 모듈용 유스케이스 2개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void moduleUseCasesHaveDesignSignatures() {
		verifyModuleUseCases();
	}

	@Test
	@DisplayName("web용 Query · Command · Result · View는 record이고 컴포넌트 이름 · 타입 · 순서가 API 명세와 맞다")
	void webRecordsHaveDesignComponents() {
		verifyWebRecords();
	}

	@Test
	@DisplayName("다른 모듈용 ScheduleInfo · AcademyInfo는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void moduleRecordsHaveDesignComponents() {
		verifyModuleRecords();
	}

	@Test
	@DisplayName("DeviceStatus는 네 값, MissingSetupItem은 CHILD_DEVICE · SCHEDULE · PICKUP_SPOT 세 값을 갖는다")
	void enumsHaveDesignValues() {
		verifyEnumValues();
	}

	private void verifyWebUseCases() {
		assertUseCase(SearchAcademyUseCase.class,
				method("searchAcademies", generic(List.class, AcademyCandidate.class), String.class));
		assertUseCase(CreateScheduleUseCase.class,
				method("createSchedule", ScheduleResult.class.getName(), CreateScheduleCommand.class));
		assertUseCase(UpdateScheduleUseCase.class,
				method("updateSchedule", ScheduleResult.class.getName(), UpdateScheduleCommand.class));
		assertUseCase(ListSchedulesUseCase.class,
				method("listSchedules", generic(List.class, ScheduleView.class), ChildQuery.class));
		assertUseCase(GetSetupSummaryUseCase.class,
				method("getSetupSummary", SetupSummary.class.getName(), ChildQuery.class));
		assertUseCase(CompleteSetupUseCase.class,
				method("completeSetup", SetupCompletionResult.class.getName(), CompleteSetupCommand.class));
		assertUseCase(GetWeeklyPickupsUseCase.class,
				method("getWeeklyPickups", WeeklyPickups.class.getName(), long.class, LocalDate.class));
		assertUseCase(GetVehicleSettingsSummaryUseCase.class,
				method("getVehicleSettingsSummary", VehicleSettingsSummary.class.getName(), long.class));
	}

	private void verifyModuleUseCases() {
		assertUseCase(LoadScheduleUseCase.class,
				method("findSchedules", generic(List.class, ScheduleInfo.class), List.class)
						.withGenericParameters(generic(List.class, Long.class)));
		assertUseCase(LoadAcademyUseCase.class,
				method("findAcademy", generic(Optional.class, AcademyInfo.class), long.class));
	}

	private void verifyWebRecords() {
		assertRecord(AcademyCandidate.class, components(
				"externalPlaceId", String.class.getName(),
				"name", String.class.getName(),
				"address", String.class.getName(),
				"location", LatLng.class.getName()));
		assertRecord(AcademyInput.class, components(
				"externalPlaceId", String.class.getName(),
				"name", String.class.getName(),
				"address", String.class.getName(),
				"location", LatLng.class.getName(),
				"entrance", LatLng.class.getName(),
				"phone", String.class.getName()));
		assertRecord(CreateScheduleCommand.class, components(
				"familyId", "long",
				"childId", "long",
				"academy", AcademyInput.class.getName(),
				"weekdays", generic(Set.class, DayOfWeek.class),
				"classEndTime", LocalTime.class.getName(),
				"targetPickup", "int"));
		assertRecord(UpdateScheduleCommand.class, components(
				"familyId", "long",
				"scheduleId", "long",
				"academy", AcademyInput.class.getName(),
				"weekdays", generic(Set.class, DayOfWeek.class),
				"classEndTime", LocalTime.class.getName(),
				"targetPickup", "int"));
		assertRecord(ScheduleResult.class, components(
				"scheduleId", "long",
				"academyId", "long",
				"status", ScheduleStatus.class.getName()));
		assertRecord(ChildQuery.class, components(
				"familyId", "long",
				"childId", "long"));
		assertRecord(ScheduleView.class, components(
				"scheduleId", "long",
				"academy", ScheduleAcademy.class.getName(),
				"weekdays", generic(List.class, DayOfWeek.class),
				"classEndTime", LocalTime.class.getName(),
				"targetPickup", "int",
				"status", ScheduleStatus.class.getName()));
		assertRecord(ScheduleAcademy.class, components(
				"academyId", "long",
				"name", String.class.getName()));
		assertRecord(SetupSummary.class, components(
				"child", SetupChild.class.getName(),
				"schedules", generic(List.class, SetupSchedule.class),
				"pickupSpots", generic(List.class, SetupSpot.class),
				"joinCallMode", JoinCallMode.class.getName(),
				"missing", generic(List.class, MissingSetupItem.class),
				"settingsVersion", "int"));
		assertRecord(SetupChild.class, components(
				"userId", "long",
				"name", String.class.getName(),
				"deviceStatus", DeviceStatus.class.getName()));
		assertRecord(SetupSchedule.class, components(
				"scheduleId", "long",
				"academyName", String.class.getName(),
				"status", ScheduleStatus.class.getName()));
		assertRecord(SetupSpot.class, components(
				"spotId", "long",
				"name", String.class.getName(),
				"isDefault", "boolean"));
		assertRecord(CompleteSetupCommand.class, components(
				"familyId", "long",
				"childId", "long",
				"settingsVersion", "int"));
		assertRecord(SetupCompletionResult.class, components(
				"syncedToVehicle", "boolean"));
		assertRecord(WeeklyPickups.class, components(
				"weekStart", LocalDate.class.getName(),
				"days", generic(List.class, DailyPickups.class),
				"readiness", PickupReadiness.class.getName()));
		assertRecord(DailyPickups.class, components(
				"date", LocalDate.class.getName(),
				"pickups", generic(List.class, WeeklyPickup.class)));
		assertRecord(WeeklyPickup.class, components(
				"scheduleId", "long",
				"childName", String.class.getName(),
				"profileImageUrl", String.class.getName(),
				"academyName", String.class.getName(),
				"classEndTime", LocalTime.class.getName(),
				"targetPickupTime", LocalTime.class.getName()));
		assertRecord(PickupReadiness.class, components(
				"startable", "boolean",
				"missing", generic(List.class, MissingSetupItem.class)));
		assertRecord(VehicleSettingsSummary.class, components(
				"guardianNameMasked", String.class.getName(),
				"children", generic(List.class, VehicleChild.class),
				"joinCallMode", JoinCallMode.class.getName()));
		assertRecord(VehicleChild.class, components(
				"userId", "long",
				"name", String.class.getName()));
	}

	private void verifyModuleRecords() {
		assertRecord(ScheduleInfo.class, components(
				"scheduleId", "long",
				"childUserId", "long",
				"academy", AcademyInfo.class.getName(),
				"weekdays", generic(Set.class, DayOfWeek.class),
				"classEndTime", LocalTime.class.getName(),
				"targetPickup", "int",
				"status", ScheduleStatus.class.getName()));
		assertRecord(AcademyInfo.class, components(
				"academyId", "long",
				"name", String.class.getName(),
				"address", String.class.getName(),
				"location", LatLng.class.getName(),
				"entrance", LatLng.class.getName(),
				"phone", String.class.getName()));
	}

	private void verifyEnumValues() {
		assertThat(DeviceStatus.values())
				.extracting(DeviceStatus::name)
				.containsExactly("CONNECTED", "INVITE_PENDING", "INVITE_EXPIRED", "NONE");
		assertThat(MissingSetupItem.values())
				.extracting(MissingSetupItem::name)
				.containsExactly("CHILD_DEVICE", "SCHEDULE", "PICKUP_SPOT");
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes,
								  String[] genericParameterTypes) {

		ExpectedMethod withGenericParameters(String... generics) {
			return new ExpectedMethod(name, genericReturnType, parameterTypes, generics);
		}
	}

	private static ExpectedMethod method(String name, String genericReturnType, Class<?>... parameterTypes) {
		String[] generics = Arrays.stream(parameterTypes).map(Class::getTypeName).toArray(String[]::new);
		return new ExpectedMethod(name, genericReturnType, parameterTypes, generics);
	}

	private static String generic(Class<?> raw, Class<?>... arguments) {
		return raw.getName() + "<" + Arrays.stream(arguments).map(Class::getName)
				.collect(Collectors.joining(", ")) + ">";
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
			assertThat(Arrays.stream(actual.getGenericParameterTypes()).map(Type::getTypeName).toList())
					.as("%s.%s 파라미터 타입", useCase.getSimpleName(), expected.name())
					.containsExactly(expected.genericParameterTypes());
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
