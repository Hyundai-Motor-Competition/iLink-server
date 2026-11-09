package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.pickup.domain.ChildPhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "session_child", uniqueConstraints = @UniqueConstraint(
		name = "uk_session_child_session_child_user", columnNames = {"sessionId", "childUserId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionChildEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long sessionChildId;

	@Column(nullable = false)
	private Long sessionId;

	@Column(nullable = false)
	private Long childUserId;

	@Column(nullable = false)
	private Long scheduleId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChildPhase childPhase;

	@Column(nullable = false)
	@ColumnDefault("0")
	private Short extraPrepMinutes;

	private Instant callNotBefore;

	@Column(precision = 9, scale = 6)
	private BigDecimal lastLat;

	@Column(precision = 9, scale = 6)
	private BigDecimal lastLng;

	private Instant lastLocatedAt;

	private Instant acknowledgedAt;

	private Instant boardedAt;

	public SessionChildEntity(Long sessionChildId, Long sessionId, Long childUserId, Long scheduleId,
			ChildPhase childPhase, Short extraPrepMinutes, Instant callNotBefore, BigDecimal lastLat,
			BigDecimal lastLng, Instant lastLocatedAt, Instant acknowledgedAt, Instant boardedAt) {
		this.sessionChildId = sessionChildId;
		this.sessionId = sessionId;
		this.childUserId = childUserId;
		this.scheduleId = scheduleId;
		this.childPhase = childPhase;
		this.extraPrepMinutes = extraPrepMinutes;
		this.callNotBefore = callNotBefore;
		this.lastLat = lastLat;
		this.lastLng = lastLng;
		this.lastLocatedAt = lastLocatedAt;
		this.acknowledgedAt = acknowledgedAt;
		this.boardedAt = boardedAt;
	}

}
