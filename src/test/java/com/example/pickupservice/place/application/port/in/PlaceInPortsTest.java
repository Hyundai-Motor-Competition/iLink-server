package com.example.pickupservice.place.application.port.in;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PlaceInPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다")
	void inPortInterfacesAndCommandResultRecordsExist() {
		verifyWebUseCases();
		verifyModuleUseCases();
		verifyWebRecords();
		verifyModuleRecords();
	}

	@Test
	@DisplayName("web용 유스케이스 4개는 interface이고 설계서 시그니처의 메서드 하나씩만 갖는다")
	void webUseCasesHaveDesignSignatures() {
		verifyWebUseCases();
	}

	@Test
	@DisplayName("다른 모듈용 LoadPickupSpotUseCase는 interface이고 findDefaultSpot · findSpot · findSpots만 갖는다")
	void moduleUseCaseHasDesignSignatures() {
		verifyModuleUseCases();
	}

	@Test
	@DisplayName("web용 Query · Command · Result · View는 record이고 컴포넌트 이름 · 타입 · 순서가 API 명세와 맞다")
	void webRecordsHaveDesignComponents() {
		verifyWebRecords();
	}

	@Test
	@DisplayName("다른 모듈용 SpotInfo · SpotSummary는 record이고 SpotSummary는 schedule의 SpotSummary와 같은 모양이다")
	void moduleRecordsHaveDesignComponents() {
		verifyModuleRecords();
	}

	private void verifyWebUseCases() {
		assertUseCase(ListPickupSpotsUseCase.class,
				method("listSpots", SpotGroups.class.getName(), ListSpotsQuery.class));
		assertUseCase(AddPickupSpotUseCase.class,
				method("addSpot", SpotCreated.class.getName(), AddSpotCommand.class));
		assertUseCase(UpdatePickupSpotUseCase.class,
				method("updateSpot", SpotView.class.getName(), UpdateSpotCommand.class));
		assertUseCase(DeletePickupSpotUseCase.class,
				method("deleteSpot", "void", DeleteSpotCommand.class));
	}

	private void verifyModuleUseCases() {
		assertUseCase(LoadPickupSpotUseCase.class,
				method("findDefaultSpot", generic(Optional.class, SpotInfo.class), long.class, long.class),
				method("findSpot", generic(Optional.class, SpotInfo.class), long.class),
				method("findSpots", generic(List.class, SpotSummary.class), long.class, List.class)
						.withGenericParameters("long", generic(List.class, Long.class)));
	}

	private void verifyWebRecords() {
		assertRecord(ListSpotsQuery.class, components(
				"familyId", "long",
				"academyId", Long.class.getName(),
				"near", LatLng.class.getName(),
				"radiusM", Integer.class.getName()));
		assertRecord(SpotGroups.class, components(
				"academies", generic(List.class, AcademySpots.class)));
		assertRecord(AcademySpots.class, components(
				"academyId", "long",
				"academyName", String.class.getName(),
				"spots", generic(List.class, SpotItem.class)));
		assertRecord(SpotItem.class, components(
				"spotId", "long",
				"name", String.class.getName(),
				"stopPoint", LatLng.class.getName(),
				"meetingPoint", LatLng.class.getName(),
				"memo", String.class.getName(),
				"isDefault", "boolean",
				"walkMinutes", Integer.class.getName()));
		assertRecord(AddSpotCommand.class, components(
				"familyId", "long",
				"createdBy", "long",
				"academyId", "long",
				"name", String.class.getName(),
				"stopPoint", LatLng.class.getName(),
				"meetingPoint", LatLng.class.getName(),
				"memo", String.class.getName(),
				"isDefault", "boolean"));
		assertRecord(SpotCreated.class, components(
				"spotId", "long",
				"isDefault", "boolean"));
		assertRecord(UpdateSpotCommand.class, components(
				"familyId", "long",
				"spotId", "long",
				"name", String.class.getName(),
				"stopPoint", LatLng.class.getName(),
				"meetingPoint", LatLng.class.getName(),
				"memo", String.class.getName(),
				"isDefault", Boolean.class.getName()));
		assertRecord(SpotView.class, components(
				"spotId", "long",
				"name", String.class.getName(),
				"stopPoint", LatLng.class.getName(),
				"meetingPoint", LatLng.class.getName(),
				"memo", String.class.getName(),
				"isDefault", "boolean"));
		assertRecord(DeleteSpotCommand.class, components(
				"familyId", "long",
				"spotId", "long"));
	}

	private void verifyModuleRecords() {
		assertRecord(SpotInfo.class, components(
				"spotId", "long",
				"familyId", "long",
				"academyId", "long",
				"name", String.class.getName(),
				"stopPoint", LatLng.class.getName(),
				"meetingPoint", LatLng.class.getName()));
		Map<String, String> spotSummaryShape = components(
				"spotId", "long",
				"academyId", "long",
				"name", String.class.getName(),
				"isDefault", "boolean");
		assertRecord(SpotSummary.class, spotSummaryShape);
		assertRecord(com.example.pickupservice.schedule.application.port.out.SpotSummary.class, spotSummaryShape);
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
