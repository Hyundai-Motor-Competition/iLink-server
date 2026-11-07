package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.ConsentType;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.MemberDeviceStatus;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.security.Role;
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

class AuthInPortsTest {

	@Test
	@DisplayName("핵심 로직 「시그니처」의 in 포트 인터페이스와 Command · Result record가 있다")
	void inPortInterfacesAndCommandResultRecordsExist() {
		verifyWebUseCases();
		verifyModuleUseCases();
		verifyWebRecords();
		verifyModuleRecords();
		verifyLinkStatusValues();
	}

	@Test
	@DisplayName("web용 유스케이스 11개는 interface이고 설계서 시그니처의 메서드 하나씩만 갖는다")
	void webUseCasesHaveDesignSignatures() {
		verifyWebUseCases();
	}

	@Test
	@DisplayName("다른 모듈용 유스케이스 3개는 interface이고 설계서 시그니처의 메서드 하나씩만 갖는다")
	void moduleUseCasesHaveDesignSignatures() {
		verifyModuleUseCases();
	}

	@Test
	@DisplayName("web용 Command · Result · Info는 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void webRecordsHaveDesignComponents() {
		verifyWebRecords();
	}

	@Test
	@DisplayName("다른 모듈용 PushTarget은 record이고 컴포넌트 이름 · 타입 · 순서가 맞다")
	void moduleRecordsHaveDesignComponents() {
		verifyModuleRecords();
	}

	@Test
	@DisplayName("LinkStatus는 WAITING · LINKED · RELINKED · EXPIRED 네 값을 갖는다")
	void linkStatusHasFourValues() {
		verifyLinkStatusValues();
	}

	private void verifyWebUseCases() {
		assertUseCase(RefreshTokenUseCase.class,
				method("refresh", TokenResult.class.getName(), String.class));
		assertUseCase(RegisterVehicleDeviceUseCase.class,
				method("registerVehicleDevice", VehicleDeviceResult.class.getName(), RegisterVehicleDeviceCommand.class));
		assertUseCase(UpdatePushTokenUseCase.class,
				method("updatePushToken", "void", UpdatePushTokenCommand.class));
		assertUseCase(LogoutVehicleUseCase.class,
				method("logoutVehicle", "void", long.class));
		assertUseCase(IssueLinkCodeUseCase.class,
				method("issueLinkCode", LinkCodeResult.class.getName(), IssueLinkCodeCommand.class));
		assertUseCase(GetLinkStatusUseCase.class,
				method("getLinkStatus", LinkStatusResult.class.getName(), long.class, long.class));
		assertUseCase(CheckLinkCodeUseCase.class,
				method("checkLinkCode", LinkCodeInfo.class.getName(), String.class, String.class));
		assertUseCase(ApproveLinkUseCase.class,
				method("approveLink", VehicleLinkResult.class.getName(), ApproveLinkCommand.class));
		assertUseCase(IssueInviteUseCase.class,
				method("issueInvite", InviteResult.class.getName(), IssueInviteCommand.class));
		assertUseCase(GetInviteUseCase.class,
				method("getInvite", InviteInfo.class.getName(), String.class));
		assertUseCase(AcceptInviteUseCase.class,
				method("acceptInvite", AcceptInviteResult.class.getName(), String.class, AcceptInviteCommand.class));
	}

	private void verifyModuleUseCases() {
		assertUseCase(GetMemberDeviceStatusUseCase.class,
				method("getDeviceStatuses",
						Map.class.getName() + "<" + Long.class.getName() + ", " + MemberDeviceStatus.class.getName() + ">",
						List.class));
		assertUseCase(RevokeMemberDevicesUseCase.class,
				method("revokeDevicesOf", "void", long.class));
		assertUseCase(LoadPushTargetUseCase.class,
				method("findPushTargets", generic(List.class, PushTarget.class), List.class));
		assertGenericParameter(GetMemberDeviceStatusUseCase.class, "getDeviceStatuses",
				generic(List.class, Long.class), List.class);
		assertGenericParameter(LoadPushTargetUseCase.class, "findPushTargets",
				generic(List.class, Long.class), List.class);
	}

	private void verifyWebRecords() {
		assertRecord(TokenResult.class, components(
				"accessToken", String.class.getName(),
				"refreshToken", String.class.getName(),
				"accessTokenExpiresAt", Instant.class.getName()));
		assertRecord(RegisterVehicleDeviceCommand.class, components(
				"vin", String.class.getName(),
				"model", String.class.getName()));
		assertRecord(VehicleDeviceResult.class, components(
				"deviceId", "long",
				"vehicleId", "long",
				"deviceToken", String.class.getName()));
		assertRecord(UpdatePushTokenCommand.class, components(
				"deviceId", "long",
				"platform", Platform.class.getName(),
				"pushToken", String.class.getName()));
		assertRecord(IssueLinkCodeCommand.class, components(
				"deviceId", "long",
				"vehicleId", "long",
				"familyId", Long.class.getName()));
		assertRecord(LinkCodeResult.class, components(
				"codeId", "long",
				"code", String.class.getName(),
				"qrPayload", String.class.getName(),
				"expiresAt", Instant.class.getName(),
				"reissuesLeft", "int"));
		assertRecord(LinkStatusResult.class, components(
				"status", LinkStatus.class.getName(),
				"vehicleId", Long.class.getName(),
				"familyId", Long.class.getName(),
				"accessToken", String.class.getName(),
				"refreshToken", String.class.getName(),
				"pollAfterSeconds", Integer.class.getName()));
		assertRecord(LinkCodeInfo.class, components(
				"status", LinkMode.class.getName(),
				"vehicleModel", String.class.getName(),
				"plateNumberMasked", String.class.getName(),
				"expiresAt", Instant.class.getName()));
		assertRecord(ConsentAgreement.class, components(
				"type", ConsentType.class.getName(),
				"agreed", "boolean"));
		assertRecord(ApproveLinkCommand.class, components(
				"code", String.class.getName(),
				"mode", LinkMode.class.getName(),
				"consents", generic(List.class, ConsentAgreement.class),
				"platform", Platform.class.getName(),
				"pushToken", String.class.getName(),
				"name", String.class.getName()));
		assertRecord(VehicleLinkResult.class, components(
				"vehicleId", "long",
				"familyId", "long",
				"userId", "long",
				"accessToken", String.class.getName(),
				"refreshToken", String.class.getName()));
		assertRecord(IssueInviteCommand.class, components(
				"familyId", "long",
				"userId", "long",
				"replaceDevice", "boolean"));
		assertRecord(InviteResult.class, components(
				"inviteUrl", String.class.getName(),
				"expiresAt", Instant.class.getName()));
		assertRecord(InviteInfo.class, components(
				"inviterName", String.class.getName(),
				"memberName", String.class.getName(),
				"role", Role.class.getName(),
				"requiredConsents", generic(List.class, ConsentType.class),
				"expiresAt", Instant.class.getName()));
		assertRecord(AcceptInviteCommand.class, components(
				"consents", generic(List.class, ConsentAgreement.class),
				"platform", Platform.class.getName(),
				"pushToken", String.class.getName()));
		assertRecord(AcceptInviteResult.class, components(
				"userId", "long",
				"familyId", "long",
				"role", Role.class.getName(),
				"accessToken", String.class.getName(),
				"refreshToken", String.class.getName()));
	}

	private void verifyModuleRecords() {
		assertRecord(PushTarget.class, components(
				"userId", "long",
				"deviceId", "long",
				"platform", Platform.class.getName(),
				"pushToken", String.class.getName()));
	}

	private void verifyLinkStatusValues() {
		assertThat(LinkStatus.class.isEnum())
				.as("LinkStatus는 enum이어야 한다")
				.isTrue();
		assertThat(LinkStatus.values())
				.extracting(LinkStatus::name)
				.containsExactly("WAITING", "LINKED", "RELINKED", "EXPIRED");
	}

	private record ExpectedMethod(String name, String genericReturnType, Class<?>[] parameterTypes) {
	}

	private static ExpectedMethod method(String name, String genericReturnType, Class<?>... parameterTypes) {
		return new ExpectedMethod(name, genericReturnType, parameterTypes);
	}

	private static String generic(Class<?> raw, Class<?> argument) {
		return raw.getName() + "<" + argument.getName() + ">";
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

	private static void assertGenericParameter(Class<?> useCase, String methodName, String expectedGenericType,
			Class<?> rawParameterType) {
		Method actual;
		try {
			actual = useCase.getMethod(methodName, rawParameterType);
		} catch (NoSuchMethodException e) {
			fail("%s에 %s(%s) 메서드가 없다", useCase.getSimpleName(), methodName, rawParameterType.getSimpleName());
			return;
		}
		assertThat(actual.getGenericParameterTypes()[0].getTypeName())
				.as("%s.%s 파라미터 타입", useCase.getSimpleName(), methodName)
				.isEqualTo(expectedGenericType);
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
