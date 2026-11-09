package com.example.pickupservice.pickup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PickupOutcomeTest {

	private static final long SESSION_ID = 7L;
	private static final long GUARDIAN_USER_ID = 11L;
	// 한국 시각 18:11:00 · 18:11:50 · 18:12:20 · 18:13:05
	private static final Instant PREDICTED_STOP_AT = Instant.parse("2026-11-09T09:11:00Z");
	private static final Instant ACTUAL_STOP_AT = Instant.parse("2026-11-09T09:11:50Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-11-09T09:12:20Z");
	private static final Instant CORRECTED_STOP_AT = Instant.parse("2026-11-09T09:13:05Z");
	private static final String CORRECTION_NOTE = "실제로는 학원 앞에 조금 늦게 섰어요";

	@Test
	@DisplayName("완료 때 만든 PickupOutcome은 completedAt · boardingMethod · predictedStopAt · actualStopAt을 갖고 정정 값은 비어 있다")
	void completeCreatesOutcomeWithStopTimesAndNoCorrection() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT);

		assertThat(outcome.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(outcome.getCompletedAt()).isEqualTo(COMPLETED_AT);
		assertThat(outcome.getBoardingMethod()).isEqualTo(BoardingMethod.DOOR);
		assertThat(outcome.getPredictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
		assertThat(outcome.getActualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(outcome.getCorrectedStopAt()).isNull();
		assertThat(outcome.getCorrectionReason()).isNull();
		assertThat(outcome.getCorrectionStatus()).isNull();
		assertThat(outcome.getCorrectedBy()).isNull();
		assertThat(outcome.getCorrectionNote()).isNull();
	}

	@Test
	@DisplayName("PickupOutcome.correct는 actualStopAt을 그대로 두고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy · correctionNote를 채운다")
	void correctKeepsActualStopAtAndFillsCorrectionFields() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT);

		outcome.correct(CORRECTED_STOP_AT, CorrectionReason.WRONG_STOP_TIME, GUARDIAN_USER_ID, CORRECTION_NOTE);

		assertThat(outcome.getActualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(outcome.getCorrectedStopAt()).isEqualTo(CORRECTED_STOP_AT);
		assertThat(outcome.getCorrectionReason()).isEqualTo(CorrectionReason.WRONG_STOP_TIME);
		assertThat(outcome.getCorrectionStatus()).isEqualTo(CorrectionStatus.REQUESTED);
		assertThat(outcome.getCorrectedBy()).isEqualTo(GUARDIAN_USER_ID);
		assertThat(outcome.getCorrectionNote()).isEqualTo(CORRECTION_NOTE);
	}

	@Test
	@DisplayName("diffSeconds는 예상 18:11:00 · 실제 18:11:50이면 50이다")
	void diffSecondsIsActualMinusPredictedInSeconds() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT);

		assertThat(outcome.diffSeconds()).contains(50L);
	}

	@Test
	@DisplayName("diffSeconds는 실제 정차가 예상보다 이르면 음수다(예상 18:11:50 · 실제 18:11:00 → -50)")
	void diffSecondsIsNegativeWhenActualIsEarlierThanPredicted() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.IMMEDIATE,
				ACTUAL_STOP_AT, PREDICTED_STOP_AT);

		assertThat(outcome.diffSeconds()).contains(-50L);
	}

	@Test
	@DisplayName("diffSeconds는 정정한 뒤에도 correctedStopAt이 아니라 원본 actualStopAt으로 계산한다")
	void diffSecondsUsesOriginalActualStopAtAfterCorrection() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT);

		outcome.correct(CORRECTED_STOP_AT, CorrectionReason.OTHER, GUARDIAN_USER_ID, null);

		assertThat(outcome.diffSeconds()).contains(50L);
	}

	@Test
	@DisplayName("diffSeconds는 predictedStopAt이 없으면 비어 있다")
	void diffSecondsIsEmptyWhenPredictedStopAtIsMissing() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.MANUAL,
				null, ACTUAL_STOP_AT);

		assertThat(outcome.diffSeconds()).isEmpty();
	}

	@Test
	@DisplayName("diffSeconds는 actualStopAt이 없으면 비어 있다")
	void diffSecondsIsEmptyWhenActualStopAtIsMissing() {
		PickupOutcome outcome = PickupOutcome.complete(SESSION_ID, COMPLETED_AT, BoardingMethod.MANUAL,
				PREDICTED_STOP_AT, null);

		assertThat(outcome.diffSeconds()).isEmpty();
	}

	@Test
	@DisplayName("PickupOutcome.restore는 결과 · 정정 필드 10개를 그대로 되살린다")
	void restoreKeepsAllFields() {
		PickupOutcome outcome = PickupOutcome.restore(SESSION_ID, COMPLETED_AT, BoardingMethod.DOOR,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT, CORRECTED_STOP_AT, CorrectionReason.WRONG_STOP_TIME,
				CorrectionStatus.REQUESTED, GUARDIAN_USER_ID, CORRECTION_NOTE);

		assertThat(outcome.getSessionId()).isEqualTo(SESSION_ID);
		assertThat(outcome.getCompletedAt()).isEqualTo(COMPLETED_AT);
		assertThat(outcome.getBoardingMethod()).isEqualTo(BoardingMethod.DOOR);
		assertThat(outcome.getPredictedStopAt()).isEqualTo(PREDICTED_STOP_AT);
		assertThat(outcome.getActualStopAt()).isEqualTo(ACTUAL_STOP_AT);
		assertThat(outcome.getCorrectedStopAt()).isEqualTo(CORRECTED_STOP_AT);
		assertThat(outcome.getCorrectionReason()).isEqualTo(CorrectionReason.WRONG_STOP_TIME);
		assertThat(outcome.getCorrectionStatus()).isEqualTo(CorrectionStatus.REQUESTED);
		assertThat(outcome.getCorrectedBy()).isEqualTo(GUARDIAN_USER_ID);
		assertThat(outcome.getCorrectionNote()).isEqualTo(CORRECTION_NOTE);
	}
}
