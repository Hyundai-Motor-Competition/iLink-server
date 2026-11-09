package com.example.pickupservice.pickup.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PickupOutcome {

	private long sessionId;
	private Instant completedAt;
	private BoardingMethod boardingMethod;
	private Instant predictedStopAt;
	private Instant actualStopAt;
	private Instant correctedStopAt;
	private CorrectionReason correctionReason;
	private CorrectionStatus correctionStatus;
	private Long correctedBy;
	private String correctionNote;

	public static PickupOutcome complete(long sessionId, Instant completedAt, BoardingMethod boardingMethod,
			Instant predictedStopAt, Instant actualStopAt) {
		return new PickupOutcome(sessionId, completedAt, boardingMethod, predictedStopAt, actualStopAt,
				null, null, null, null, null);
	}

	public static PickupOutcome restore(long sessionId, Instant completedAt, BoardingMethod boardingMethod,
			Instant predictedStopAt, Instant actualStopAt, Instant correctedStopAt, CorrectionReason correctionReason,
			CorrectionStatus correctionStatus, Long correctedBy, String correctionNote) {
		return new PickupOutcome(sessionId, completedAt, boardingMethod, predictedStopAt, actualStopAt,
				correctedStopAt, correctionReason, correctionStatus, correctedBy, correctionNote);
	}

	public void correct(Instant correctedStopAt, CorrectionReason reason, long correctedBy, String note) {
		this.correctedStopAt = correctedStopAt;
		this.correctionReason = reason;
		this.correctionStatus = CorrectionStatus.REQUESTED;
		this.correctedBy = correctedBy;
		this.correctionNote = note;
	}

	public Optional<Long> diffSeconds() {
		if (predictedStopAt == null || actualStopAt == null) {
			return Optional.empty();
		}
		return Optional.of(Duration.between(predictedStopAt, actualStopAt).getSeconds());
	}
}
