package com.example.pickupservice.notification.application.port.out;

import com.example.pickupservice.auth.application.port.in.PushTarget;
import com.example.pickupservice.notification.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class NotificationOutPortsTest {

	private static final String NOTIFICATION = Notification.class.getName();

	@Test
	@DisplayName("핵심 로직 「시그니처」의 out 포트 인터페이스가 있다")
	void outPortInterfacesExist() {
		verifyPersistencePorts();
		verifyExternalPorts();
		verifyPushMessage();
	}

	@Test
	@DisplayName("LoadNotificationPort · SaveNotificationPort는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void persistencePortsHaveDesignSignatures() {
		verifyPersistencePorts();
	}

	@Test
	@DisplayName("SendPushPort · LoadPushTargetPort · LoadSessionFamilyPort · LoadMemberNamePort · CallProgressPort · LoadResendGuardPort는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void externalPortsHaveDesignSignatures() {
		verifyExternalPorts();
	}

	@Test
	@DisplayName("PushMessage는 record이고 받는 기기(PushTarget) · 제목 · 본문 · data 컴포넌트를 갖는다")
	void pushMessageHasDesignComponents() {
		verifyPushMessage();
	}

	private void verifyPersistencePorts() {
		assertPort(LoadNotificationPort.class,
				method("load", "java.util.Optional<" + NOTIFICATION + ">", long.class),
				method("loadBySession", "java.util.List<" + NOTIFICATION + ">", long.class),
				method("loadPending", "java.util.List<" + NOTIFICATION + ">",
						long.class, String.class, Long.class),
				method("loadResendTargets", "java.util.List<" + NOTIFICATION + ">", Instant.class),
				method("countAttempts", "int", Notification.class));
		assertPort(SaveNotificationPort.class,
				method("save", NOTIFICATION, Notification.class));
	}

	private void verifyExternalPorts() {
		assertPort(SendPushPort.class,
				method("send", "boolean", PushMessage.class));
		assertPort(LoadPushTargetPort.class,
				method("loadPushTargets", "java.util.List<" + PushTarget.class.getName() + ">", List.class),
				method("loadPushTarget", "java.util.Optional<" + PushTarget.class.getName() + ">", long.class),
				method("loadPushTargetIncludingRevoked", "java.util.Optional<" + PushTarget.class.getName() + ">", long.class));
		assertPort(LoadSessionFamilyPort.class,
				method("loadFamilyId", "java.util.Optional<java.lang.Long>", long.class));
		assertPort(LoadMemberNamePort.class,
				method("loadNames", "java.util.Map<java.lang.Long, java.lang.String>", List.class));
		assertPort(CallProgressPort.class,
				method("markCallSent", "void", long.class, Instant.class),
				method("markCallDelivered", "void", long.class, Instant.class),
				method("isCallAcknowledged", "boolean", long.class));
		assertPort(LoadResendGuardPort.class,
				method("isSessionEnded", "boolean", long.class),
				method("isCallRevoked", "boolean", long.class));
	}

	private void verifyPushMessage() {
		assertRecord(PushMessage.class, components(
				"target", PushTarget.class.getName(),
				"title", String.class.getName(),
				"body", String.class.getName(),
				"data", "java.util.Map<java.lang.String, java.lang.String>"));
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes) {
	}

	private static ExpectedMethod method(String name, String genericReturnType, Class<?>... parameterTypes) {
		return new ExpectedMethod(name, genericReturnType, parameterTypes);
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
