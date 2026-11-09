package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.application.port.in.SessionEventPage;
import com.example.pickupservice.pickup.domain.Call;
import com.example.pickupservice.pickup.domain.GuardianDataSetting;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PickupOutPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 out 포트 인터페이스가 있다")
	void outPortInterfacesExist() {
		verifyPersistencePorts();
		verifyExternalPorts();
		verifyOutRecords();
	}

	@Test
	@DisplayName("세션 · 호출 · 이벤트 · 결과 · 기록 out 포트 10개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void persistencePortsHaveDesignSignatures() {
		verifyPersistencePorts();
	}

	@Test
	@DisplayName("notification · schedule · place · user · auth 연결 out 포트 5개는 interface이고 설계서 시그니처의 메서드만 갖고 그 모듈 in 포트 타입을 쓴다")
	void externalPortsHaveDesignSignatures() {
		verifyExternalPorts();
	}

	@Test
	@DisplayName("GuardianDataSetting용 out 포트 LoadDataSettingPort · SaveDataSettingPort가 있다")
	void dataSettingPortsExist() {
		verifyDataSettingPorts();
	}

	@Test
	@DisplayName("SessionEventMessage · NotificationRequest는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void outRecordsHaveComponents() {
		verifyOutRecords();
	}

	private void verifyPersistencePorts() {
		String optionalSession = generic(Optional.class, PickupSession.class);
		assertPort(LoadSessionPort.class,
				method("load", optionalSession, params(long.class)),
				method("loadForUpdate", optionalSession, params(long.class)),
				method("loadActiveByVehicle", optionalSession, params(long.class)),
				method("loadActiveByFamily", optionalSession, params(long.class)),
				method("loadActiveByChild", optionalSession, params(long.class)),
				method("loadActiveBySpot", optionalSession, params(long.class)),
				method("loadBoardingExpired", generic(List.class, PickupSession.class), params(Instant.class)));
		assertPort(SaveSessionPort.class,
				method("save", PickupSession.class.getName(), params(PickupSession.class)));
		assertPort(LoadCallPort.class,
				method("loadCall", generic(Optional.class, Call.class), params(long.class)),
				method("loadLiveCall", generic(Optional.class, Call.class), params(long.class)),
				method("loadLatestCall", generic(Optional.class, Call.class), params(long.class)));
		assertPort(SaveCallPort.class,
				method("saveCall", Call.class.getName(), params(Call.class)));
		assertPort(SessionEventPort.class,
				method("append", SessionEvent.class.getName(),
						params(long.class, String.class, int.class, Instant.class, Map.class),
						"long", String.class.getName(), "int", Instant.class.getName(),
						generic(Map.class, String.class, Object.class)),
				method("loadAfter", SessionEventPage.class.getName(), params(long.class, long.class, int.class)),
				method("loadLastHelpRequest", generic(Optional.class, SessionEvent.class),
						params(long.class, long.class)),
				method("loadFirst", generic(Optional.class, SessionEvent.class), params(long.class, String.class)));
		assertPort(PublishSessionEventPort.class,
				method("publish", "void", params(SessionEventMessage.class)));
		assertPort(ProcessedVehicleEventPort.class,
				method("markProcessed", "boolean", params(long.class, String.class)));
		assertPort(LoadOutcomePort.class,
				method("loadOutcome", generic(Optional.class, PickupOutcome.class), params(long.class)));
		assertPort(SaveOutcomePort.class,
				method("saveOutcome", PickupOutcome.class.getName(), params(PickupOutcome.class)));
		assertPort(LoadHistoryPort.class,
				method("loadHistory", generic(List.class, HistoryItem.class),
						params(long.class, LocalDate.class, LocalDate.class)));
	}

	private void verifyExternalPorts() {
		assertPort(RecordNotificationPort.class,
				method("recordNotifications", generic(List.class, Long.class), params(NotificationRequest.class)));
		assertPort(LoadSchedulePort.class,
				method("loadSchedules", generic(List.class, ScheduleInfo.class),
						params(List.class), generic(List.class, Long.class)));
		assertPort(LoadSpotPort.class,
				method("loadDefaultSpot", generic(Optional.class, SpotInfo.class), params(long.class, long.class)),
				method("loadSpot", generic(Optional.class, SpotInfo.class), params(long.class)));
		assertPort(LoadFamilyPort.class,
				method("loadFamily", FamilyInfo.class.getName(), params(long.class)),
				method("loadMembers", generic(List.class, MemberInfo.class), params(long.class)),
				method("loadMember", generic(Optional.class, MemberInfo.class), params(long.class)));
		assertPort(LoadChildReadinessPort.class,
				method("loadDeviceStatuses", generic(Map.class, Long.class, MemberDeviceStatus.class),
						params(List.class), generic(List.class, Long.class)),
				method("hasLocationConsent", "boolean", params(long.class)));
	}

	private void verifyDataSettingPorts() {
		assertPort(LoadDataSettingPort.class,
				method("loadDataSetting", generic(Optional.class, GuardianDataSetting.class), params(long.class)));
		assertPort(SaveDataSettingPort.class,
				method("saveDataSetting", GuardianDataSetting.class.getName(), params(GuardianDataSetting.class)));
	}

	private void verifyOutRecords() {
		assertRecord(SessionEventMessage.class, components(
				"sessionId", "long",
				"eventId", Long.class.getName(),
				"seq", Long.class.getName(),
				"type", String.class.getName(),
				"stateVersion", "int",
				"occurredAt", Instant.class.getName(),
				"payload", generic(Map.class, String.class, Object.class),
				"receiverUserIds", generic(List.class, Long.class)));
		assertRecord(NotificationRequest.class, components(
				"sessionId", "long",
				"callId", Long.class.getName(),
				"type", String.class.getName(),
				"receiverUserIds", generic(List.class, Long.class)));
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
