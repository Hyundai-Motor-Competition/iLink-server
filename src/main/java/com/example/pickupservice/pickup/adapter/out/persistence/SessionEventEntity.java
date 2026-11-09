package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "session_event", uniqueConstraints = @UniqueConstraint(
		name = "uk_session_event_session_seq", columnNames = {"sessionId", "seq"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionEventEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventId;

	@Column(nullable = false)
	private Long sessionId;

	@Column(nullable = false)
	private Long seq;

	@Column(nullable = false, length = 40)
	private String type;

	@Column(nullable = false)
	private int stateVersion;

	@Column(nullable = false)
	private Instant occurredAt;

	@JdbcTypeCode(SqlTypes.JSON)
	private String payload;

	public SessionEventEntity(Long eventId, Long sessionId, Long seq, String type, int stateVersion,
			Instant occurredAt, String payload) {
		this.eventId = eventId;
		this.sessionId = sessionId;
		this.seq = seq;
		this.type = type;
		this.stateVersion = stateVersion;
		this.occurredAt = occurredAt;
		this.payload = payload;
	}

}
