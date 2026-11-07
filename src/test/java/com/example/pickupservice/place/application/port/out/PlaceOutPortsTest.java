package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.domain.PickupSpot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PlaceOutPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 out 포트 인터페이스가 있다")
	void outPortInterfacesExist() {
		verifyPersistencePorts();
		verifyExternalPorts();
		verifyAcademyInfoRecord();
	}

	@Test
	@DisplayName("저장소 out 포트 LoadSpotPort · SaveSpotPort · DistancePort는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void persistencePortsHaveDesignSignatures() {
		verifyPersistencePorts();
	}

	@Test
	@DisplayName("schedule · pickup 연결 out 포트 LoadAcademyPort · CheckActivePickupPort는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void externalPortsHaveDesignSignatures() {
		verifyExternalPorts();
	}

	@Test
	@DisplayName("place 쪽 AcademyInfo는 academyId · name · location record다")
	void academyInfoHasComponents() {
		verifyAcademyInfoRecord();
	}

	private void verifyPersistencePorts() {
		assertPort(LoadSpotPort.class,
				method("loadSpot", generic(Optional.class, PickupSpot.class), params(long.class)),
				method("loadSpots", generic(List.class, PickupSpot.class), params(long.class, Long.class)),
				method("loadSpotsWithin", generic(List.class, PickupSpot.class),
						params(long.class, Long.class, LatLng.class, int.class)),
				method("loadDefaultSpot", generic(Optional.class, PickupSpot.class), params(long.class, long.class)));
		assertPort(SaveSpotPort.class,
				method("saveSpot", PickupSpot.class.getName(), params(PickupSpot.class)),
				method("deleteSpot", "void", params(long.class)));
		assertPort(DistancePort.class,
				method("isWithinMeters", "boolean", params(LatLng.class, LatLng.class, int.class)));
	}

	private void verifyExternalPorts() {
		assertPort(LoadAcademyPort.class,
				method("loadAcademy", generic(Optional.class, AcademyInfo.class), params(long.class)));
		assertPort(CheckActivePickupPort.class,
				method("isUsedByActiveSession", "boolean", params(long.class)),
				method("loadActiveChildLocation", generic(Optional.class, LatLng.class), params(long.class)));
	}

	private void verifyAcademyInfoRecord() {
		assertThat(AcademyInfo.class.isRecord()).as("AcademyInfo는 record여야 한다").isTrue();
		RecordComponent[] components = AcademyInfo.class.getRecordComponents();
		assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
				.containsExactly("academyId", "name", "location");
		assertThat(Arrays.stream(components).map(c -> c.getGenericType().getTypeName()).toList())
				.containsExactly("long", String.class.getName(), LatLng.class.getName());
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes,
								  String[] genericParameterTypes) {
	}

	private static Class<?>[] params(Class<?>... types) {
		return types;
	}

	private static ExpectedMethod method(String name, String genericReturnType, Class<?>[] parameterTypes,
										 String... genericParameterTypes) {
		String[] generics = genericParameterTypes.length == 0
				? Arrays.stream(parameterTypes).map(Class::getTypeName).toArray(String[]::new)
				: genericParameterTypes;
		return new ExpectedMethod(name, genericReturnType, parameterTypes, generics);
	}

	private static String generic(Class<?> raw, Class<?>... arguments) {
		return raw.getName() + "<" + Arrays.stream(arguments).map(Class::getName)
				.collect(Collectors.joining(", ")) + ">";
	}

	private static void assertPort(Class<?> port, ExpectedMethod... expectedMethods) {
		assertThat(port.isInterface())
				.as("%s는 interface여야 한다", port.getSimpleName())
				.isTrue();

		List<String> abstractMethodNames = Arrays.stream(port.getDeclaredMethods())
				.filter(m -> Modifier.isAbstract(m.getModifiers()))
				.map(Method::getName)
				.sorted()
				.toList();
		List<String> expectedNames = Arrays.stream(expectedMethods)
				.map(ExpectedMethod::name)
				.sorted()
				.toList();
		assertThat(abstractMethodNames)
				.as("%s의 추상 메서드 목록", port.getSimpleName())
				.isEqualTo(expectedNames);

		for (ExpectedMethod expected : expectedMethods) {
			Method actual;
			try {
				actual = port.getMethod(expected.name(), expected.parameterTypes());
			} catch (NoSuchMethodException e) {
				fail("%s에 %s%s 메서드가 없다", port.getSimpleName(), expected.name(),
						Arrays.toString(expected.parameterTypes()));
				return;
			}
			assertThat(actual.getGenericReturnType().getTypeName())
					.as("%s.%s 반환 타입", port.getSimpleName(), expected.name())
					.isEqualTo(expected.genericReturnType());
			assertThat(Arrays.stream(actual.getGenericParameterTypes()).map(Type::getTypeName).toList())
					.as("%s.%s 파라미터 타입", port.getSimpleName(), expected.name())
					.containsExactly(expected.genericParameterTypes());
		}
	}
}
