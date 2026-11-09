package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeCommand;
import com.example.pickupservice.pickup.application.port.in.OutcomeView;
import com.example.pickupservice.pickup.application.port.out.LoadOutcomePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.domain.BoardingDoor;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.CorrectionReason;
import com.example.pickupservice.pickup.domain.CorrectionStatus;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class CorrectOutcomeServiceTest {

	private static final LocalDate SESSION_DATE_KST = LocalDate.of(2026, 11, 9);
	// 한국 시간 예상 정차 18:11:00, 실제 정차 18:11:50, 완료 18:12:30
	private static final Instant PREDICTED_STOP_AT = Instant.parse("2026-11-09T09:11:00Z");
	private static final Instant ACTUAL_STOP_AT = Instant.parse("2026-11-09T09:11:50Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-11-09T09:12:30Z");
	// 보호자가 정정하려는 정차 시각 한국 18:11:20
	private static final Instant CORRECTED_STOP_AT = Instant.parse("2026-11-09T09:11:20Z");
	private static final String NOTE = "실제로는 18시 11분 20초쯤 섰어요";

	private static final long SESSION_ID = 501L;
	private static final long VEHICLE_ID = 77L;
	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long MINJUN_ID = 33L;
	private static final long MINJUN_SCHEDULE_ID = 801L;
	private static final long SPOT_ID = 21L;
	private static final String SPOT_NAME = "해솔영어학원 정문 앞";

	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_GUARDIAN_ID = 91L;

	private static final LatLng STOP_POINT = new LatLng(37.501400, 127.039950);
	private static final LatLng MEETING_POINT = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSessionPort loadSessionPort;

	@Mock
	private LoadOutcomePort loadOutcomePort;

	@Mock
	private SaveOutcomePort saveOutcomePort;

	private CorrectOutcomeService correctOutcomeService;

	@BeforeEach
	void setUp() {
		correctOutcomeService = new CorrectOutcomeService(loadSessionPort, loadOutcomePort, saveOutcomePort);
		lenient().when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(completedSession()));
		lenient().when(loadSessionPort.loadForUpdate(SESSION_ID)).thenReturn(Optional.of(completedSession()));
		lenient().when(saveOutcomePort.saveOutcome(any(PickupOutcome.class))).then(returnsFirstArg());
	}

	@Test
	@DisplayName("결과 정정은 actualStopAt 18:11:50을 그대로 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy · correctionNote를 채워 저장한다")
	void correctionKeepsActualStopAndRecordsCorrectionFields() {
		givenOutcome(completedOutcome());

		correctOutcomeService.correct(command(GUARDIAN_ID, Role.GUARDIAN, CORRECTED_STOP_AT, "WRONG_STOP_TIME", NOTE));

		PickupOutcome saved = savedOutcome();
		assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(saved.getActualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(saved.getPredictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
		assertThat(saved.getCompletedAt()).isEqualTo(COMPLETED_AT);
		assertThat(saved.getBoardingMethod()).isEqualTo(BoardingMethod.DOOR);
		assertThat(saved.getCorrectedStopAt()).isEqualTo(CORRECTED_STOP_AT);
		assertThat(saved.getCorrectionReason()).isEqualTo(CorrectionReason.WRONG_STOP_TIME);
		assertThat(saved.getCorrectionStatus()).isEqualTo(CorrectionStatus.REQUESTED);
		assertThat(saved.getCorrectedBy()).isEqualTo(GUARDIAN_ID);
		assertThat(saved.getCorrectionNote()).isEqualTo(NOTE);
	}

	@Test
	@DisplayName("결과 정정은 OUT-02와 같은 모양으로 actualStopAt 기준 diffSeconds 50과 correction REQUESTED · 정정 시각을 돌려준다")
	void correctionReturnsOutcomeViewWithRequestedCorrection() {
		givenOutcome(completedOutcome());

		OutcomeView view = correctOutcomeService.correct(
				command(GUARDIAN_ID, Role.GUARDIAN, CORRECTED_STOP_AT, "WRONG_STOP_TIME", NOTE));

		assertThat(view.sessionId()).isEqualTo(SESSION_ID);
		assertThat(view.completedAt()).isEqualTo(COMPLETED_AT);
		assertThat(view.spotName()).isEqualTo(SPOT_NAME);
		assertThat(view.boardingMethod()).isEqualTo("DOOR");
		assertThat(view.predictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
		assertThat(view.actualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(view.diffSeconds()).isEqualTo(50L);
		assertThat(view.correction()).isNotNull();
		assertThat(view.correction().status()).isEqualTo("REQUESTED");
		assertThat(view.correction().correctedStopAt()).isEqualTo(CORRECTED_STOP_AT);
	}

	@Test
	@DisplayName("결과 정정은 CO_GUARDIAN이 reasonCode OTHER · note 없이 보내면 correctedBy에 그 사용자 id, correctionReason OTHER, correctionNote null을 기록한다")
	void correctionByCoGuardianRecordsCoGuardianAndOtherReason() {
		givenOutcome(completedOutcome());

		correctOutcomeService.correct(command(CO_GUARDIAN_ID, Role.CO_GUARDIAN, CORRECTED_STOP_AT, "OTHER", null));

		PickupOutcome saved = savedOutcome();
		assertThat(saved.getCorrectedBy()).isEqualTo(CO_GUARDIAN_ID);
		assertThat(saved.getCorrectionReason()).isEqualTo(CorrectionReason.OTHER);
		assertThat(saved.getCorrectionStatus()).isEqualTo(CorrectionStatus.REQUESTED);
		assertThat(saved.getCorrectionNote()).isNull();
		assertThat(saved.getActualStopAt()).isEqualTo(ACTUAL_STOP_AT);
	}

	@Test
	@DisplayName("결과 정정은 이미 정정 요청이 있으면 새 요청의 correctedStopAt · correctionReason · correctedBy · correctionNote로 덮어쓴다")
	void correctionOverwritesPreviousCorrectionRequest() {
		Instant previousCorrectedStopAt = Instant.parse("2026-11-09T09:10:40Z");
		givenOutcome(PickupOutcome.restore(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR, PREDICTED_STOP_AT,
				ACTUAL_STOP_AT, previousCorrectedStopAt, CorrectionReason.OTHER, CorrectionStatus.REQUESTED,
				CO_GUARDIAN_ID, "먼저 보낸 정정"));

		OutcomeView view = correctOutcomeService.correct(
				command(GUARDIAN_ID, Role.GUARDIAN, CORRECTED_STOP_AT, "WRONG_STOP_TIME", NOTE));

		PickupOutcome saved = savedOutcome();
		assertThat(saved.getCorrectedStopAt()).isEqualTo(CORRECTED_STOP_AT);
		assertThat(saved.getCorrectionReason()).isEqualTo(CorrectionReason.WRONG_STOP_TIME);
		assertThat(saved.getCorrectionStatus()).isEqualTo(CorrectionStatus.REQUESTED);
		assertThat(saved.getCorrectedBy()).isEqualTo(GUARDIAN_ID);
		assertThat(saved.getCorrectionNote()).isEqualTo(NOTE);
		assertThat(saved.getActualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(view.correction().correctedStopAt()).isEqualTo(CORRECTED_STOP_AT);
	}

	@Test
	@DisplayName("결과 정정은 결과가 아직 없는(완료 전) 세션이면 NOT_FOUND로 거절하고 저장하지 않는다")
	void correctionOfSessionWithoutOutcomeIsNotFound() {
		lenient().when(loadOutcomePort.loadOutcome(SESSION_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> correctOutcomeService.correct(
						command(GUARDIAN_ID, Role.GUARDIAN, CORRECTED_STOP_AT, "WRONG_STOP_TIME", NOTE)));

		assertNotFound(exception);
		verify(saveOutcomePort, never()).saveOutcome(any());
	}

	@Test
	@DisplayName("결과 정정은 다른 가족의 세션이면 결과가 있어도 NOT_FOUND로 거절하고 저장하지 않는다")
	void correctionOfOtherFamilySessionIsNotFound() {
		givenOutcome(completedOutcome());

		AuthPrincipal otherGuardian = new AuthPrincipal(PrincipalType.USER, OTHER_FAMILY_GUARDIAN_ID, OTHER_FAMILY_ID,
				Role.GUARDIAN);
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> correctOutcomeService.correct(new CorrectOutcomeCommand(otherGuardian, SESSION_ID,
						CORRECTED_STOP_AT, "WRONG_STOP_TIME", NOTE)));

		assertNotFound(exception);
		verify(saveOutcomePort, never()).saveOutcome(any());
	}

	// 세션 날짜 2026-11-09(한국) 밖: 한국 11-08 23:59:59, 한국 11-10 00:00:00(24:00), 한국 11-10 09:11:20
	@ParameterizedTest
	@ValueSource(strings = {"2026-11-08T14:59:59Z", "2026-11-09T15:00:00Z", "2026-11-10T00:11:20Z"})
	@DisplayName("결과 정정은 correctedStopAt이 세션 날짜(한국 00:00~24:00) 밖이면 VALIDATION_FAILED(field correctedStopAt)로 거절하고 저장하지 않는다")
	void correctionOutsideSessionDateIsValidationFailed(String correctedStopAt) {
		givenOutcome(completedOutcome());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> correctOutcomeService.correct(
						command(GUARDIAN_ID, Role.GUARDIAN, Instant.parse(correctedStopAt), "WRONG_STOP_TIME", NOTE)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
		assertThat(exception.fields())
				.extracting(FieldError::field)
				.containsExactly("correctedStopAt");
		verify(saveOutcomePort, never()).saveOutcome(any());
	}

	// 세션 날짜 2026-11-09(한국) 안의 양 끝: 한국 11-09 00:00:00, 한국 11-09 23:59:59
	@ParameterizedTest
	@ValueSource(strings = {"2026-11-08T15:00:00Z", "2026-11-09T14:59:59Z"})
	@DisplayName("결과 정정은 correctedStopAt이 세션 날짜의 한국 00:00:00 · 23:59:59이면 받아들여 저장한다")
	void correctionAtSessionDateBoundariesIsAccepted(String correctedStopAt) {
		givenOutcome(completedOutcome());

		correctOutcomeService.correct(
				command(GUARDIAN_ID, Role.GUARDIAN, Instant.parse(correctedStopAt), "WRONG_STOP_TIME", NOTE));

		assertThat(savedOutcome().getCorrectedStopAt()).isEqualTo(Instant.parse(correctedStopAt));
	}

	private void givenOutcome(PickupOutcome outcome) {
		lenient().when(loadOutcomePort.loadOutcome(SESSION_ID)).thenReturn(Optional.of(outcome));
	}

	private PickupOutcome savedOutcome() {
		ArgumentCaptor<PickupOutcome> captor = ArgumentCaptor.forClass(PickupOutcome.class);
		verify(saveOutcomePort).saveOutcome(captor.capture());
		return captor.getValue();
	}

	private static void assertNotFound(BusinessException exception) {
		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
	}

	private static PickupOutcome completedOutcome() {
		return PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR, PREDICTED_STOP_AT,
				ACTUAL_STOP_AT);
	}

	private static PickupSession completedSession() {
		List<SessionChild> children = List.of(SessionChild.restore(7001L, MINJUN_ID, MINJUN_SCHEDULE_ID,
				ChildPhase.MEETING, 0, null, null, null, ACTUAL_STOP_AT.minusSeconds(60), ACTUAL_STOP_AT.plusSeconds(20)));
		return PickupSession.restore(SESSION_ID, VEHICLE_ID, FAMILY_ID, SPOT_ID, SPOT_NAME, STOP_POINT, MEETING_POINT,
				SESSION_DATE_KST, SessionStatus.COMPLETED, VehiclePhase.STOPPED, SpotStatus.CONFIRMED, 15, 0, 1, 0,
				false, PREDICTED_STOP_AT, 0, ACTUAL_STOP_AT.minusSeconds(5), ACTUAL_STOP_AT.plusSeconds(20),
				ACTUAL_STOP_AT.plusSeconds(30), BoardingDoor.REAR_RIGHT, null, null, null, null, null, children);
	}

	private static CorrectOutcomeCommand command(long userId, Role role, Instant correctedStopAt, String reasonCode,
			String note) {
		AuthPrincipal principal = new AuthPrincipal(PrincipalType.USER, userId, FAMILY_ID, role);
		return new CorrectOutcomeCommand(principal, SESSION_ID, correctedStopAt, reasonCode, note);
	}

}
