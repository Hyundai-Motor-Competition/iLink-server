package com.example.pickupservice.notification.application.port.in;

import com.example.pickupservice.common.security.AuthPrincipal;
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

class NotificationInPortsTest {

	private static final String PRINCIPAL = AuthPrincipal.class.getName();
	private static final String INSTANT = Instant.class.getName();
	private static final String STRING = String.class.getName();

	@Test
	@DisplayName("핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다")
	void inPortInterfacesAndCommandResultRecordsExist() {
		verifyWebAndListenerUseCases();
		verifyModuleUseCases();
		verifyRecords();
	}

	@Test
	@DisplayName("web · 스케줄러 · 리스너용 유스케이스 6개는 interface이고 설계서 시그니처의 메서드 하나씩만 갖는다")
	void webSchedulerListenerUseCasesHaveDesignSignatures() {
		verifyWebAndListenerUseCases();
	}

	@Test
	@DisplayName("다른 모듈용 RecordNotificationUseCase는 interface이고 record(NotificationRequest)만 갖는다")
	void recordNotificationUseCaseHasDesignSignature() {
		verifyModuleUseCases();
	}

	@Test
	@DisplayName("Command · Query · View · Request · Message는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void recordsHaveDesignComponents() {
		verifyRecords();
	}

	private void verifyWebAndListenerUseCases() {
		assertUseCase(RecordDeliveryUseCase.class,
				method("recordDelivery", "void", DeliveryReceiptCommand.class));
		assertUseCase(ListNotificationsUseCase.class,
				method("list", "java.util.List<" + NotificationView.class.getName() + ">",
						NotificationListQuery.class));
		assertUseCase(ResendNotificationUseCase.class,
				method("resend", "long", ResendCommand.class));
		assertUseCase(ResendUndeliveredUseCase.class,
				method("resendUndelivered", "int", Instant.class));
		assertUseCase(DispatchSessionEventUseCase.class,
				method("dispatch", "void", SessionEventMessage.class));
		assertUseCase(NotifyDeviceReplacedUseCase.class,
				method("notifyDeviceReplaced", "void", List.class));
	}

	private void verifyModuleUseCases() {
		assertUseCase(RecordNotificationUseCase.class,
				method("record", "java.util.List<java.lang.Long>", NotificationRequest.class));
	}

	private void verifyRecords() {
		assertRecord(DeliveryReceiptCommand.class, components(
				"principal", PRINCIPAL,
				"notificationId", "long",
				"receivedAt", INSTANT));
		assertRecord(NotificationListQuery.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long"));
		assertRecord(ResendCommand.class, components(
				"principal", PRINCIPAL,
				"notificationId", "long"));
		assertRecord(NotificationView.class, components(
				"notificationId", "long",
				"type", STRING,
				"receiverName", STRING,
				"title", STRING,
				"sentAt", INSTANT,
				"deliveredAt", INSTANT,
				"openedAt", INSTANT,
				"attempt", "int"));
		assertRecord(NotificationRequest.class, components(
				"sessionId", "long",
				"callId", Long.class.getName(),
				"type", STRING,
				"receiverUserIds", "java.util.List<java.lang.Long>"));
		assertRecord(SessionEventMessage.class, components(
				"sessionId", "long",
				"eventId", Long.class.getName(),
				"seq", Long.class.getName(),
				"type", STRING,
				"stateVersion", "int",
				"occurredAt", INSTANT,
				"payload", "java.util.Map<java.lang.String, java.lang.Object>",
				"receiverUserIds", "java.util.List<java.lang.Long>"));
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes) {
	}

	private static ExpectedMethod method(String name, String genericReturnType, Class<?>... parameterTypes) {
		return new ExpectedMethod(name, genericReturnType, parameterTypes);
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
