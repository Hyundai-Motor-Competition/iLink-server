package com.example.pickupservice.auth.application.service;

import com.example.pickupservice.auth.application.port.in.LinkCodeInfo;
import com.example.pickupservice.auth.application.port.out.LoadLinkCodePort;
import com.example.pickupservice.auth.domain.LinkCode;
import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.user.application.port.in.LoadVehicleUseCase;
import com.example.pickupservice.user.application.port.in.VehicleInfo;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckLinkCodeServiceTest {

	private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	private static final long CODE_ID = 5012L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final String CODE = "K7P2QX9M";
	private static final String UNKNOWN_CODE = "ZZZZ9999";
	private static final String CLIENT_KEY = "203.0.113.10";
	private static final String OTHER_CLIENT_KEY = "203.0.113.20";
	private static final String MODEL = "아이오닉 5";
	private static final String PLATE_NUMBER = "12가 3456";
	private static final String PLATE_NUMBER_MASKED = "12가 ****";

	@Mock
	private LoadLinkCodePort loadLinkCodePort;

	@Mock
	private LoadVehicleUseCase loadVehicleUseCase;

	@Mock
	private Clock clock;

	private CheckLinkCodeService checkLinkCodeService;

	@BeforeEach
	void setUp() {
		lenient().when(clock.instant()).thenReturn(NOW);
		lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		lenient().when(loadLinkCodePort.loadLinkCodeByCode(UNKNOWN_CODE)).thenReturn(Optional.empty());
		checkLinkCodeService = new CheckLinkCodeService(loadLinkCodePort, loadVehicleUseCase,
				new LinkCodeAttemptLimiter(clock), clock);
	}

	@Test
	@DisplayName("연결 코드 확인은 가족에 연결된 차량의 코드면 RELINK, 아니면 NEW를 돌려준다")
	void checkLinkCodeReturnsRelinkForCodeOfVehicleLinkedToFamily() {
		givenCode(issuedCode());
		givenVehicle(new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", MODEL, PLATE_NUMBER, true));

		LinkCodeInfo info = checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY);

		assertThat(info).isEqualTo(new LinkCodeInfo(LinkMode.RELINK, MODEL, PLATE_NUMBER_MASKED,
				NOW.plusSeconds(180)));
	}

	@Test
	@DisplayName("연결 코드 확인은 가족이 없는 연결 대기 차량의 코드면 NEW를 돌려준다")
	void checkLinkCodeReturnsNewForCodeOfVehicleWithoutFamily() {
		givenCode(issuedCode());
		givenVehicle(new VehicleInfo(VEHICLE_ID, null, "KMHJ381ABNU123456", MODEL, PLATE_NUMBER, true));

		LinkCodeInfo info = checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY);

		assertThat(info).isEqualTo(new LinkCodeInfo(LinkMode.NEW, MODEL, PLATE_NUMBER_MASKED,
				NOW.plusSeconds(180)));
	}

	@Test
	@DisplayName("연결 코드 확인은 familyId가 남아 있어도 연결 해제된(unlinkedAt이 찬) 차량의 코드면 NEW를 돌려준다")
	void checkLinkCodeReturnsNewForCodeOfUnlinkedVehicleEvenWithFamilyId() {
		givenCode(issuedCode());
		givenVehicle(new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", MODEL, PLATE_NUMBER, false));

		LinkCodeInfo info = checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY);

		assertThat(info.status()).isEqualTo(LinkMode.NEW);
	}

	@Test
	@DisplayName("연결 코드 확인은 없는 코드면 LINK_CODE_INVALID로 거절한다")
	void checkLinkCodeRejectsUnknownCode() {
		assertLinkCodeInvalid(() -> checkLinkCodeService.checkLinkCode(UNKNOWN_CODE, CLIENT_KEY));

		verify(loadVehicleUseCase, never()).findVehicle(anyLong());
	}

	@Test
	@DisplayName("연결 코드 확인은 사용된 코드면 LINK_CODE_EXPIRED로 거절한다")
	void checkLinkCodeRejectsUsedCode() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.USED, NOW.plusSeconds(180),
				NOW.minusSeconds(10), LinkMode.NEW, null));

		assertLinkCodeExpired(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));

		verify(loadVehicleUseCase, never()).findVehicle(anyLong());
	}

	@Test
	@DisplayName("연결 코드 확인은 status가 EXPIRED인 코드면 LINK_CODE_EXPIRED로 거절한다")
	void checkLinkCodeRejectsExpiredStatusCode() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.EXPIRED, NOW.plusSeconds(180),
				null, null, null));

		assertLinkCodeExpired(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));
	}

	@Test
	@DisplayName("연결 코드 확인은 expiresAt이 지난 ISSUED 코드면 LINK_CODE_EXPIRED로 거절한다")
	void checkLinkCodeRejectsIssuedCodePastExpiresAt() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.ISSUED, NOW.minusSeconds(1),
				null, null, null));

		assertLinkCodeExpired(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));
	}

	@Test
	@DisplayName("연결 코드 확인이 같은 기기에서 5번 실패하면 1분 동안 LINK_CODE_ATTEMPT_LIMIT으로 거절한다")
	void checkLinkCodeRejectsSixthAttemptAfterFiveFailuresFromSameClient() {
		failFiveTimes(CLIENT_KEY);
		lenient().when(loadLinkCodePort.loadLinkCodeByCode(CODE)).thenReturn(Optional.of(issuedCode()));

		assertAttemptLimit(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));

		when(clock.instant()).thenReturn(NOW.plusSeconds(59));
		assertAttemptLimit(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));

		verify(loadLinkCodePort, never()).loadLinkCodeByCode(CODE);
		verify(loadLinkCodePort, times(5)).loadLinkCodeByCode(anyString());
	}

	@Test
	@DisplayName("연결 코드 확인은 4번 실패한 기기의 5번째 시도는 막지 않는다")
	void checkLinkCodeAllowsFifthAttemptAfterFourFailures() {
		for (int i = 0; i < 4; i++) {
			assertLinkCodeInvalid(() -> checkLinkCodeService.checkLinkCode(UNKNOWN_CODE, CLIENT_KEY));
		}
		givenCode(issuedCode());
		givenVehicle(new VehicleInfo(VEHICLE_ID, null, "KMHJ381ABNU123456", MODEL, PLATE_NUMBER, true));

		LinkCodeInfo info = checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY);

		assertThat(info.status()).isEqualTo(LinkMode.NEW);
	}

	@Test
	@DisplayName("연결 코드 확인은 LINK_CODE_EXPIRED 실패도 5번 실패에 센다")
	void checkLinkCodeCountsExpiredCodeFailuresTowardAttemptLimit() {
		givenCode(LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.USED, NOW.plusSeconds(180),
				NOW.minusSeconds(10), LinkMode.NEW, null));
		for (int i = 0; i < 5; i++) {
			assertLinkCodeExpired(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));
		}

		assertAttemptLimit(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));
		verify(loadLinkCodePort, times(5)).loadLinkCodeByCode(CODE);
	}

	@Test
	@DisplayName("연결 코드 확인은 5번 실패한 기기라도 1분이 지나면 다시 확인한다")
	void checkLinkCodeAllowsAttemptAgainAfterOneMinute() {
		failFiveTimes(CLIENT_KEY);
		givenCode(issuedCode(NOW.plusSeconds(300)));
		givenVehicle(new VehicleInfo(VEHICLE_ID, FAMILY_ID, "KMHJ381ABNU123456", MODEL, PLATE_NUMBER, true));
		when(clock.instant()).thenReturn(NOW.plusSeconds(60));

		LinkCodeInfo info = checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY);

		assertThat(info.status()).isEqualTo(LinkMode.RELINK);
	}

	@Test
	@DisplayName("연결 코드 확인은 한 기기가 5번 실패해도 다른 기기는 막지 않는다")
	void checkLinkCodeDoesNotLimitOtherClientWhenOneClientFailedFiveTimes() {
		failFiveTimes(CLIENT_KEY);
		givenCode(issuedCode());
		givenVehicle(new VehicleInfo(VEHICLE_ID, null, "KMHJ381ABNU123456", MODEL, PLATE_NUMBER, true));

		LinkCodeInfo info = checkLinkCodeService.checkLinkCode(CODE, OTHER_CLIENT_KEY);

		assertThat(info.status()).isEqualTo(LinkMode.NEW);
		assertAttemptLimit(() -> checkLinkCodeService.checkLinkCode(CODE, CLIENT_KEY));
	}

	private void failFiveTimes(String clientKey) {
		for (int i = 0; i < 5; i++) {
			assertLinkCodeInvalid(() -> checkLinkCodeService.checkLinkCode(UNKNOWN_CODE, clientKey));
		}
	}

	private void givenCode(LinkCode code) {
		when(loadLinkCodePort.loadLinkCodeByCode(CODE)).thenReturn(Optional.of(code));
	}

	private void givenVehicle(VehicleInfo vehicle) {
		when(loadVehicleUseCase.findVehicle(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
	}

	private static LinkCode issuedCode() {
		return issuedCode(NOW.plusSeconds(180));
	}

	private static LinkCode issuedCode(Instant expiresAt) {
		return LinkCode.restore(CODE_ID, VEHICLE_ID, CODE, LinkCodeStatus.ISSUED, expiresAt, null, null, null);
	}

	private static void assertLinkCodeInvalid(ThrowingCallable call) {
		assertError(call, HttpStatus.NOT_FOUND, "LINK_CODE_INVALID");
	}

	private static void assertLinkCodeExpired(ThrowingCallable call) {
		assertError(call, HttpStatus.GONE, "LINK_CODE_EXPIRED");
	}

	private static void assertAttemptLimit(ThrowingCallable call) {
		assertError(call, HttpStatus.TOO_MANY_REQUESTS, "LINK_CODE_ATTEMPT_LIMIT");
	}

	private static void assertError(ThrowingCallable call, HttpStatus status, String code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.status()).isEqualTo(status);
			assertThat(e.code()).isEqualTo(code);
		});
	}
}
