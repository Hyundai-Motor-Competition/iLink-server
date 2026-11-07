package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.common.security.PrincipalType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKeyEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	private PrincipalType principalType;

	private Long principalId;

	@Column(nullable = false, length = IdempotencyKeyHeader.MAX_LENGTH)
	private String idempotencyKey;

	@Column(nullable = false, columnDefinition = "char(64)")
	private String requestHash;

	private Integer responseStatus;

	@JdbcTypeCode(SqlTypes.JSON)
	private String responseBody;

	IdempotencyKeyEntity(PrincipalType principalType, Long principalId, String idempotencyKey,
			String requestHash, Integer responseStatus, String responseBody) {
		this.principalType = principalType;
		this.principalId = principalId;
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.responseStatus = responseStatus;
		this.responseBody = responseBody;
	}

	IdempotencyKeyEntity(IdempotentRequest request) {
		this(request.principalType(), request.principalId(), request.key(), request.requestHash(), null, null);
	}

	void complete(int status, String body) {
		this.responseStatus = status;
		this.responseBody = body;
	}

	boolean isReplayableFor(String otherRequestHash) {
		return responseStatus != null && requestHash.equals(otherRequestHash);
	}

}
