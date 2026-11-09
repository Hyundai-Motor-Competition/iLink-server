package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_vehicle_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedVehicleEventEntity extends BaseTimeEntity {

	@Id
	@Column(length = 36)
	private String eventId;

	@Column(nullable = false)
	private Long sessionId;

	public ProcessedVehicleEventEntity(String eventId, Long sessionId) {
		this.eventId = eventId;
		this.sessionId = sessionId;
	}

}
