package com.example.pickupservice.pickup.application.port.in;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.SessionEvent;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import com.example.pickupservice.user.domain.JoinCallMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PickupInPortsTest {

	private static final String PRINCIPAL = AuthPrincipal.class.getName();
	private static final String STRING = String.class.getName();
	private static final String INSTANT = Instant.class.getName();
	private static final String LONG_OR_NULL = Long.class.getName();
	private static final String INTEGER_OR_NULL = Integer.class.getName();
	private static final String DOUBLE_OR_NULL = Double.class.getName();
	private static final String VIEW = SessionView.class.getName();

	@Test
	@DisplayName("핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다")
	void inPortInterfacesAndCommandResultRecordsExist() {
		verifyWebUseCases();
		verifyModuleUseCases();
		verifyCommandRecords();
		verifyResultRecords();
		verifySessionViewRecords();
	}

	@Test
	@DisplayName("web · sse · scheduler용 유스케이스 26개는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void webUseCasesHaveDesignSignatures() {
		verifyWebUseCases();
	}

	@Test
	@DisplayName("다른 모듈용 ActivePickupQueryUseCase · CallProgressUseCase는 interface이고 설계서 시그니처의 메서드만 갖는다")
	void moduleUseCasesHaveDesignSignatures() {
		verifyModuleUseCases();
	}

	@Test
	@DisplayName("Command · Query는 record이고 첫 컴포넌트가 AuthPrincipal이며 나머지 이름 · 타입 · 순서가 API 명세 요청과 맞다")
	void commandRecordsHaveApiRequestComponents() {
		verifyCommandRecords();
	}

	@Test
	@DisplayName("Result · View는 record이고 컴포넌트 이름 · 타입 · 순서가 API 명세 응답과 맞다")
	void resultRecordsHaveApiResponseComponents() {
		verifyResultRecords();
	}

	@Test
	@DisplayName("SessionView와 하위 record는 세션 뷰 표의 status · spot · eta · call(guard · progress) · children[](location) · route · boarding · incident 구조를 따른다")
	void sessionViewRecordsFollowSessionViewTable() {
		verifySessionViewRecords();
	}

	private void verifyWebUseCases() {
		assertUseCase(FindActiveSessionUseCase.class,
				method("findActive", generic(Optional.class, ActiveSessionResult.class), AuthPrincipal.class));
		assertUseCase(StartPickupUseCase.class,
				method("start", StartPickupResult.class.getName(), StartPickupCommand.class));
		assertUseCase(GetSessionUseCase.class,
				method("getSession", VIEW, SessionQuery.class));
		assertUseCase(ConfirmRouteUseCase.class,
				method("confirmRoute", VIEW, ConfirmRouteCommand.class));
		assertUseCase(ChangeSpotUseCase.class,
				method("changeSpot", VIEW, ChangeSpotCommand.class));
		assertUseCase(HoldSessionUseCase.class,
				method("hold", VIEW, HoldCommand.class),
				method("releaseHold", VIEW, HoldCommand.class));
		assertUseCase(CancelSessionUseCase.class,
				method("cancel", VIEW, CancelSessionCommand.class));
		assertUseCase(ListSessionEventsUseCase.class,
				method("listEvents", SessionEventPage.class.getName(),
						long.class, long.class, int.class, AuthPrincipal.class));
		assertUseCase(GetContactsUseCase.class,
				method("getContacts", generic(List.class, ContactResult.class), SessionQuery.class));
		assertUseCase(CompletePickupUseCase.class,
				method("complete", VIEW, CompletePickupCommand.class));
		assertUseCase(RejectBoardingUseCase.class,
				method("rejectBoarding", VIEW, RejectBoardingCommand.class));
		assertUseCase(OpenIncidentUseCase.class,
				method("openIncident", VIEW, OpenIncidentCommand.class));
		assertUseCase(ReceiveTelemetryUseCase.class,
				method("receive", "void", TelemetryCommand.class));
		assertUseCase(ReceiveVehicleEventUseCase.class,
				method("receive", "void", VehicleEventCommand.class));
		assertUseCase(ExtendPrepTimeUseCase.class,
				method("extend", PrepExtensionResult.class.getName(), ExtendPrepTimeCommand.class));
		assertUseCase(ReportChildLocationUseCase.class,
				method("report", "void", ChildLocationCommand.class));
		assertUseCase(RequestHelpUseCase.class,
				method("request", HelpRequestResult.class.getName(), HelpRequestCommand.class));
		assertUseCase(HoldCallUseCase.class,
				method("holdCall", VIEW, CallHoldCommand.class),
				method("releaseCallHold", VIEW, CallHoldCommand.class));
		assertUseCase(SendCallUseCase.class,
				method("send", CallResult.class.getName(), SendCallCommand.class));
		assertUseCase(RevokeCallUseCase.class,
				method("revoke", VIEW, RevokeCallCommand.class));
		assertUseCase(AcknowledgeCallUseCase.class,
				method("acknowledge", AcknowledgeResult.class.getName(), AcknowledgeCallCommand.class));
		assertUseCase(GetPickupHistoryUseCase.class,
				method("history", generic(List.class, HistoryItem.class), HistoryQuery.class));
		assertUseCase(GetOutcomeUseCase.class,
				method("getOutcome", OutcomeView.class.getName(), SessionQuery.class));
		assertUseCase(CorrectOutcomeUseCase.class,
				method("correct", OutcomeView.class.getName(), CorrectOutcomeCommand.class));
		assertUseCase(UpdateDataSettingsUseCase.class,
				method("updateDataSettings", DataSettingsResult.class.getName(), DataSettingsCommand.class));
		assertUseCase(AutoCompleteBoardingUseCase.class,
				method("completeExpiredBoardings", "int", Instant.class));
	}

	private void verifyModuleUseCases() {
		assertUseCase(ActivePickupQueryUseCase.class,
				method("hasActivePickup", "boolean", long.class),
				method("isInActivePickup", "boolean", long.class),
				method("isSpotInUse", "boolean", long.class),
				method("activeChildLocation", generic(Optional.class, LatLng.class), long.class));
		assertUseCase(CallProgressUseCase.class,
				method("markCallSent", "void", long.class, Instant.class),
				method("markCallDelivered", "void", long.class, Instant.class),
				method("isCallAcknowledged", "boolean", long.class));
	}

	private void verifyCommandRecords() {
		assertRecord(StartPickupCommand.class, components(
				"principal", PRINCIPAL,
				"scheduleIds", generic(List.class, Long.class),
				"date", LocalDate.class.getName()));
		assertRecord(SessionQuery.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long"));
		assertRecord(ConfirmRouteCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"spotId", "long",
				"navRouteId", STRING,
				"stateVersion", "int"));
		assertRecord(ChangeSpotCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"spotId", "long",
				"stateVersion", "int",
				"spotVersion", "int"));
		assertRecord(HoldCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"stateVersion", "int",
				"reason", STRING));
		assertRecord(CancelSessionCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"stateVersion", "int",
				"reason", STRING));
		assertRecord(CompletePickupCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"mode", STRING,
				"stateVersion", "int",
				"notBoardedChildUserIds", generic(List.class, Long.class)));
		assertRecord(RejectBoardingCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"childUserId", LONG_OR_NULL,
				"stateVersion", "int"));
		assertRecord(OpenIncidentCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"type", STRING,
				"childUserId", "long",
				"stateVersion", "int"));
		assertRecord(TelemetryCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"observedAt", INSTANT,
				"navEtaSeconds", "int",
				"remainingMeters", "int",
				"location", LatLng.class.getName(),
				"headingDeg", INTEGER_OR_NULL,
				"speedKmh", DOUBLE_OR_NULL,
				"gear", STRING));
		assertRecord(VehicleEventCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"eventId", STRING,
				"type", STRING,
				"occurredAt", INSTANT,
				"location", LatLng.class.getName(),
				"door", STRING));
		assertRecord(ExtendPrepTimeCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"minutes", "int"));
		assertRecord(ChildLocationCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"at", INSTANT,
				"lat", "double",
				"lng", "double",
				"accuracyM", DOUBLE_OR_NULL));
		assertRecord(HelpRequestCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"type", STRING));
		assertRecord(CallHoldCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"stateVersion", "int"));
		assertRecord(SendCallCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"stateVersion", "int"));
		assertRecord(RevokeCallCommand.class, components(
				"principal", PRINCIPAL,
				"callId", "long",
				"stateVersion", "int",
				"reason", STRING));
		assertRecord(AcknowledgeCallCommand.class, components(
				"principal", PRINCIPAL,
				"callId", "long"));
		assertRecord(HistoryQuery.class, components(
				"principal", PRINCIPAL,
				"childId", "long",
				"month", YearMonth.class.getName()));
		assertRecord(CorrectOutcomeCommand.class, components(
				"principal", PRINCIPAL,
				"sessionId", "long",
				"correctedStopAt", INSTANT,
				"reasonCode", STRING,
				"note", STRING));
		assertRecord(DataSettingsCommand.class, components(
				"principal", PRINCIPAL,
				"learningConsent", "boolean"));
	}

	private void verifyResultRecords() {
		assertRecord(ActiveSessionResult.class, components(
				"sessionId", "long",
				"status", SessionStatus.class.getName(),
				"stateVersion", "int"));
		assertRecord(StartPickupResult.class, components(
				"created", "boolean",
				"session", VIEW));
		assertRecord(SessionEventPage.class, components(
				"items", generic(List.class, SessionEvent.class),
				"hasMore", "boolean"));
		assertRecord(ContactResult.class, components(
				"name", STRING,
				"role", STRING,
				"phone", STRING));
		assertRecord(PrepExtensionResult.class, components(
				"extraPrepMinutes", "int",
				"callNotBefore", INSTANT));
		assertRecord(HelpRequestResult.class, components(
				"nextAllowedAt", INSTANT));
		assertRecord(CallResult.class, components(
				"callId", "long",
				"status", CallStatus.class.getName()));
		assertRecord(AcknowledgeResult.class, components(
				"callId", "long",
				"status", CallStatus.class.getName(),
				"acknowledgedAt", INSTANT));
		assertRecord(HistoryItem.class, components(
				"date", LocalDate.class.getName(),
				"sessionId", "long",
				"status", SessionStatus.class.getName(),
				"completedAt", INSTANT));
		assertRecord(OutcomeView.class, components(
				"sessionId", "long",
				"completedAt", INSTANT,
				"spotName", STRING,
				"boardingMethod", STRING,
				"predictedStopAt", INSTANT,
				"actualStopAt", INSTANT,
				"diffSeconds", LONG_OR_NULL,
				"correction", CorrectionView.class.getName()));
		assertRecord(CorrectionView.class, components(
				"status", STRING,
				"correctedStopAt", INSTANT));
		assertRecord(DataSettingsResult.class, components(
				"learningConsent", "boolean",
				"retentionDays", "int"));
	}

	private void verifySessionViewRecords() {
		assertRecord(SessionView.class, components(
				"sessionId", "long",
				"stateVersion", "int",
				"status", SessionStatusView.class.getName(),
				"spot", SessionSpotView.class.getName(),
				"eta", SessionEtaView.class.getName(),
				"call", SessionCallView.class.getName(),
				"children", generic(List.class, SessionChildView.class),
				"route", SessionRouteView.class.getName(),
				"boarding", SessionBoardingView.class.getName(),
				"incident", SessionIncidentView.class.getName()));
		assertRecord(SessionStatusView.class, components(
				"session", SessionStatus.class.getName(),
				"vehicle", VehiclePhase.class.getName(),
				"call", STRING,
				"spot", SpotStatus.class.getName()));
		assertRecord(SessionSpotView.class, components(
				"spotId", "long",
				"name", STRING,
				"spotVersion", "int",
				"locked", "boolean"));
		assertRecord(SessionEtaView.class, components(
				"stopEtaAt", INSTANT,
				"remainingSeconds", INTEGER_OR_NULL,
				"updatedAt", INSTANT,
				"stale", "boolean"));
		assertRecord(SessionCallView.class, components(
				"mode", JoinCallMode.class.getName(),
				"callId", LONG_OR_NULL,
				"held", "boolean",
				"scheduledAt", INSTANT,
				"guard", CallGuardView.class.getName(),
				"progress", CallProgressView.class.getName()));
		assertRecord(CallGuardView.class, components(
				"met", "boolean",
				"unmet", generic(List.class, String.class)));
		assertRecord(CallProgressView.class, components(
				"acceptedAt", INSTANT,
				"sentAt", INSTANT,
				"deliveredAt", INSTANT,
				"acknowledgedAt", INSTANT));
		assertRecord(SessionChildView.class, components(
				"childUserId", "long",
				"name", STRING,
				"phase", ChildPhase.class.getName(),
				"extraPrepMinutes", "int",
				"callNotBefore", INSTANT,
				"acknowledgedAt", INSTANT,
				"boardedAt", INSTANT,
				"location", ChildLocationView.class.getName()));
		assertRecord(ChildLocationView.class, components(
				"lat", "double",
				"lng", "double",
				"at", INSTANT));
		assertRecord(SessionRouteView.class, components(
				"routeVersion", "int",
				"remainingMeters", INTEGER_OR_NULL));
		assertRecord(SessionBoardingView.class, components(
				"detectedAt", INSTANT,
				"cancelableUntil", INSTANT,
				"door", STRING));
		assertRecord(SessionIncidentView.class, components(
				"type", STRING,
				"childUserId", "long",
				"openedAt", INSTANT));
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes,
								  String[] genericParameterTypes) {
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
