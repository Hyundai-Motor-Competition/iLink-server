package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.Device;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.MemberInvite;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class AuthOutPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 out 포트 인터페이스가 있다")
	void outPortInterfacesExist() {
		verifyPersistencePorts();
		verifyTokenAndModulePorts();
		verifyTokenRecords();
	}

	@Test
	@DisplayName("저장소 out 포트 6개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void persistencePortsHaveDesignSignatures() {
		verifyPersistencePorts();
	}

	@Test
	@DisplayName("토큰 · pickup · 이벤트 out 포트 3개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void tokenAndModulePortsHaveDesignSignatures() {
		verifyTokenAndModulePorts();
	}

	@Test
	@DisplayName("TokenSubject · IssuedTokens는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void tokenRecordsHaveDesignComponents() {
		verifyTokenRecords();
	}

	private void verifyPersistencePorts() {
		assertPort(LoadDevicePort.class,
				method("loadDevice", generic(Optional.class, Device.class), params(long.class)),
				method("loadLiveDevices", generic(List.class, Device.class), params(OwnerType.class, long.class)),
				method("loadByRefreshTokenHash", generic(Optional.class, Device.class), params(String.class)));
		assertPort(SaveDevicePort.class,
				method("saveDevice", Device.class.getName(), params(Device.class)));
		assertPort(LoadLinkCodePort.class,
				method("loadLinkCode", generic(Optional.class, LinkCode.class), params(long.class)),
				method("loadLinkCodeByCode", generic(Optional.class, LinkCode.class), params(String.class)),
				method("loadIssuedCodes", generic(List.class, LinkCode.class), params(long.class)));
		assertPort(SaveLinkCodePort.class,
				method("saveLinkCode", LinkCode.class.getName(), params(LinkCode.class)));
		assertPort(LoadInvitePort.class,
				method("loadInviteByToken", generic(Optional.class, MemberInvite.class), params(String.class)),
				method("loadLatestInvite", generic(Optional.class, MemberInvite.class), params(long.class)));
		assertPort(SaveInvitePort.class,
				method("saveInvite", MemberInvite.class.getName(), params(MemberInvite.class)));
	}

	private void verifyTokenAndModulePorts() {
		assertPort(IssueTokenPort.class,
				method("issueTokens", IssuedTokens.class.getName(), params(TokenSubject.class)),
				method("hashRefreshToken", String.class.getName(), params(String.class)));
		assertPort(CheckActivePickupPort.class,
				method("hasActivePickup", "boolean", params(long.class)));
		assertPort(PublishAuthEventPort.class,
				method("publishDeviceReplaced", "void",
						params(List.class), generic(List.class, Long.class)));
	}

	private void verifyTokenRecords() {
		assertRecord(TokenSubject.class, components(
				"type", PrincipalType.class.getName(),
				"id", "long",
				"familyId", Long.class.getName(),
				"role", Role.class.getName(),
				"deviceId", "long"));
		assertRecord(IssuedTokens.class, components(
				"accessToken", String.class.getName(),
				"accessTokenExpiresAt", Instant.class.getName(),
				"refreshToken", String.class.getName(),
				"refreshTokenHash", String.class.getName()));
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
