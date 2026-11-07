package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.security.PrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

	Optional<IdempotencyKeyEntity> findByPrincipalTypeAndPrincipalIdAndIdempotencyKey(
			PrincipalType principalType, Long principalId, String idempotencyKey);

	@Modifying
	@Query("delete from IdempotencyKeyEntity k where k.createdAt < :threshold")
	int deleteByCreatedAtBefore(@Param("threshold") Instant threshold);

}
