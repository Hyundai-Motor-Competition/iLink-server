package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.DeviceStatus;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ScheduleOutPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 out 포트 인터페이스가 있다")
	void outPortInterfacesExist() {
		verifyPersistencePorts();
		verifyExternalPorts();
		verifySpotSummaryRecord();
	}

	@Test
	@DisplayName("저장소 out 포트 4개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void persistencePortsHaveDesignSignatures() {
		verifyPersistencePorts();
	}

	@Test
	@DisplayName("지도 API · place · auth 연결 out 포트 3개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void externalPortsHaveDesignSignatures() {
		verifyExternalPorts();
	}

	@Test
	@DisplayName("SpotSummary는 spotId · academyId · name · isDefault record다")
	void spotSummaryHasComponents() {
		verifySpotSummaryRecord();
	}

	private void verifyPersistencePorts() {
		assertPort(LoadAcademyPort.class,
				method("loadAcademy", generic(Optional.class, Academy.class), params(long.class)),
				method("loadAcademyByExternalPlaceId", generic(Optional.class, Academy.class),
						params(long.class, String.class)));
		assertPort(SaveAcademyPort.class,
				method("saveAcademy", Academy.class.getName(), params(Academy.class)));
		assertPort(LoadSchedulePort.class,
				method("loadSchedule", generic(Optional.class, PickupSchedule.class), params(long.class)),
				method("loadSchedulesOfChild", generic(List.class, PickupSchedule.class), params(long.class)),
				method("loadSchedulesOfAcademy", generic(List.class, PickupSchedule.class), params(long.class)),
				method("loadSchedulesOfChildren", generic(List.class, PickupSchedule.class),
						params(List.class), generic(List.class, Long.class)),
				method("loadSchedules", generic(List.class, PickupSchedule.class),
						params(List.class), generic(List.class, Long.class)));
		assertPort(SaveSchedulePort.class,
				method("saveSchedule", PickupSchedule.class.getName(), params(PickupSchedule.class)));
	}

	private void verifyExternalPorts() {
		assertPort(SearchAcademyPort.class,
				method("search", generic(List.class, AcademyCandidate.class), params(String.class)));
		assertPort(LoadPickupSpotPort.class,
				method("loadSpots", generic(List.class, SpotSummary.class),
						params(long.class, List.class), "long", generic(List.class, Long.class)));
		assertPort(LoadMemberDeviceStatusPort.class,
				method("loadDeviceStatuses", generic(Map.class, Long.class, DeviceStatus.class),
						params(List.class), generic(List.class, Long.class)));
	}

	private void verifySpotSummaryRecord() {
		assertThat(SpotSummary.class.isRecord()).as("SpotSummary는 record여야 한다").isTrue();
		RecordComponent[] components = SpotSummary.class.getRecordComponents();
		assertThat(Arrays.stream(components).map(RecordComponent::getName).toList())
				.containsExactly("spotId", "academyId", "name", "isDefault");
		assertThat(Arrays.stream(components).map(c -> c.getGenericType().getTypeName()).toList())
				.containsExactly("long", "long", String.class.getName(), "boolean");
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

	private static String signatureOf(String name, Class<?>[] parameterTypes) {
		return name + Arrays.stream(parameterTypes).map(Class::getTypeName)
				.collect(Collectors.joining(", ", "(", ")"));
	}

	private static void assertPort(Class<?> port, ExpectedMethod... expectedMethods) {
		assertThat(port.isInterface())
				.as("%s는 interface여야 한다", port.getSimpleName())
				.isTrue();

		List<String> abstractMethodNames = Arrays.stream(port.getDeclaredMethods())
				.filter(m -> Modifier.isAbstract(m.getModifiers()))
				.map(m -> signatureOf(m.getName(), m.getParameterTypes()))
				.sorted()
				.toList();
		List<String> expectedNames = Arrays.stream(expectedMethods)
				.map(expected -> signatureOf(expected.name(), expected.parameterTypes()))
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
