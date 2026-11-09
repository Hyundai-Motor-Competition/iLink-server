package com.example.pickupservice.schedule.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.schedule.domain.ScheduleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "pickup_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupScheduleEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long scheduleId;

	@Column(nullable = false)
	private Long childUserId;

	@Column(nullable = false)
	private Long academyId;

	@Column(nullable = false, length = 30)
	private String weekdays;

	@Column(nullable = false)
	private LocalTime classEndTime;

	@Column(nullable = false)
	private Short targetPickup;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ScheduleStatus status;

	public PickupScheduleEntity(Long scheduleId, Long childUserId, Long academyId, String weekdays,
			LocalTime classEndTime, Short targetPickup, ScheduleStatus status) {
		this.scheduleId = scheduleId;
		this.childUserId = childUserId;
		this.academyId = academyId;
		this.weekdays = weekdays;
		this.classEndTime = classEndTime;
		this.targetPickup = targetPickup;
		this.status = status;
	}

}
