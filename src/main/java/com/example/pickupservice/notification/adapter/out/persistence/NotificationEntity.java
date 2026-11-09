package com.example.pickupservice.notification.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long notificationId;

	@Column(nullable = false)
	private Long sessionId;

	private Long callId;

	@Column(nullable = false, length = 30)
	private String type;

	@Column(nullable = false)
	private Long deviceId;

	private Instant sentAt;

	private Instant deliveredAt;

	private Instant openedAt;

	public NotificationEntity(Long notificationId, Long sessionId, Long callId, String type, Long deviceId,
			Instant sentAt, Instant deliveredAt, Instant openedAt) {
		this.notificationId = notificationId;
		this.sessionId = sessionId;
		this.callId = callId;
		this.type = type;
		this.deviceId = deviceId;
		this.sentAt = sentAt;
		this.deliveredAt = deliveredAt;
		this.openedAt = openedAt;
	}

}
