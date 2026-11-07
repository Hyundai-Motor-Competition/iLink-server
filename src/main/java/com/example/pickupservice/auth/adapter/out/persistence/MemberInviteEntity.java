package com.example.pickupservice.auth.adapter.out.persistence;

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
@Table(name = "member_invite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberInviteEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long inviteId;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, unique = true, length = 64)
	private String token;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant usedAt;

	public MemberInviteEntity(Long inviteId, Long userId, String token, Instant expiresAt, Instant usedAt) {
		this.inviteId = inviteId;
		this.userId = userId;
		this.token = token;
		this.expiresAt = expiresAt;
		this.usedAt = usedAt;
	}

}
