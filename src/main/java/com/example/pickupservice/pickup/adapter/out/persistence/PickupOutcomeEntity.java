package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pickup_outcome")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupOutcomeEntity extends BaseTimeEntity {

	@Id
	private Long sessionId;

	@Column(nullable = false)
	private Instant completedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BoardingMethod boardingMethod;

	private Instant predictedStopAt;

	private Instant actualStopAt;

	private Instant correctedStopAt;

	@Column(length = 30)
	private String correctionReason;

	@Column(length = 20)
	private String correctionStatus;

	private Long correctedBy;

	@Column(length = 200)
	private String correctionNote;

	public PickupOutcomeEntity(Long sessionId, Instant completedAt, BoardingMethod boardingMethod,
			Instant predictedStopAt, Instant actualStopAt, Instant correctedStopAt, String correctionReason,
			String correctionStatus, Long correctedBy, String correctionNote) {
		this.sessionId = sessionId;
		this.completedAt = completedAt;
		this.boardingMethod = boardingMethod;
		this.predictedStopAt = predictedStopAt;
		this.actualStopAt = actualStopAt;
		this.correctedStopAt = correctedStopAt;
		this.correctionReason = correctionReason;
		this.correctionStatus = correctionStatus;
		this.correctedBy = correctedBy;
		this.correctionNote = correctionNote;
	}

}
