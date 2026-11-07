package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.DeviceStatus;
import com.example.pickupservice.user.domain.Family;
import com.example.pickupservice.user.domain.User;
import com.example.pickupservice.user.domain.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class UserOutPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 out 포트 인터페이스가 있다")
	void outPortInterfacesExist() {
		verifyPersistencePorts();
		verifyModulePorts();
	}

	@Test
	@DisplayName("저장소 out 포트 6개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void persistencePortsHaveDesignSignatures() {
		verifyPersistencePorts();
	}

	@Test
	@DisplayName("auth · pickup 연결 out 포트 3개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void modulePortsHaveDesignSignatures() {
		verifyModulePorts();
	}

	private void verifyPersistencePorts() {
		assertPort(LoadFamilyPort.class,
				method("loadFamily", generic(Optional.class, Family.class), params(long.class)));
		assertPort(SaveFamilyPort.class,
				method("saveFamily", Family.class.getName(), params(Family.class)));
		assertPort(LoadUserPort.class,
				method("loadUser", generic(Optional.class, User.class), params(long.class)),
				method("loadUsers", generic(List.class, User.class), params(long.class)),
				method("countNonChildMembers", "long", params(long.class)));
		assertPort(SaveUserPort.class,
				method("saveUser", User.class.getName(), params(User.class)),
				method("deleteUser", "void", params(long.class)));
		assertPort(LoadVehiclePort.class,
				method("loadVehicle", generic(Optional.class, Vehicle.class), params(long.class)),
				method("loadLinkedVehicleByVin", generic(Optional.class, Vehicle.class), params(String.class)),
				method("loadLinkedVehicleOfFamily", generic(Optional.class, Vehicle.class), params(long.class)));
		assertPort(SaveVehiclePort.class,
				method("saveVehicle", Vehicle.class.getName(), params(Vehicle.class)));
	}

	private void verifyModulePorts() {
		assertPort(LoadMemberDeviceStatusPort.class,
				method("loadDeviceStatuses", generic(Map.class, Long.class, DeviceStatus.class),
						params(List.class), generic(List.class, Long.class)));
		assertPort(RevokeMemberDevicesPort.class,
				method("revokeDevicesOf", "void", params(long.class)));
		assertPort(CheckActivePickupPort.class,
				method("isInActivePickup", "boolean", params(long.class)),
				method("hasActivePickup", "boolean", params(long.class)));
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
