package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.PrincipalType;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class IdempotencyKeyCleanerTest extends IntegrationTestSupport {

	private static final Instant NOW = Instant.parse("2026-01-15T03:00:00Z");
	private static final Instant SAVED_25_HOURS_AGO = NOW.minus(Duration.ofHours(25));
	private static final Instant SAVED_23_HOURS_AGO = NOW.minus(Duration.ofHours(23));
	private static final String EXPIRED_COMPLETED_KEY = "c1e5a0d2-3b4f-4a6e-8c7d-9e0f1a2b3c01";
	private static final String FRESH_COMPLETED_KEY = "c1e5a0d2-3b4f-4a6e-8c7d-9e0f1a2b3c02";
	private static final String EXPIRED_PROCESSING_KEY = "c1e5a0d2-3b4f-4a6e-8c7d-9e0f1a2b3c03";
	private static final String FRESH_PROCESSING_KEY = "c1e5a0d2-3b4f-4a6e-8c7d-9e0f1a2b3c04";
	private static final List<String> TEST_KEYS =
			List.of(EXPIRED_COMPLETED_KEY, FRESH_COMPLETED_KEY, EXPIRED_PROCESSING_KEY, FRESH_PROCESSING_KEY);
	private static final long USER_ID = 101L;
	private static final String REQUEST_HASH = "a".repeat(64);
	private static final int CREATED = 201;
	private static final String RESPONSE_BODY = """
			{"spotId":41,"name":"정문 앞","stopPoint":{"lat":37.501234,"lng":127.039876},"memo":null,"isDefault":true}""";

	private final IdempotencyKeyCleaner idempotencyKeyCleaner;
	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("delete from idempotency_key where idempotency_key in (?, ?, ?, ?)", TEST_KEYS.toArray());
	}

	@Test
	@DisplayName("만료 키 삭제 작업은 현재 시각보다 25시간 전에 저장한 완료 행(응답 상태 201)은 지우고 23시간 전에 저장한 완료 행은 남기며 지운 행 수 1을 돌려준다")
	void deletesCompletedRowSavedMoreThan24HoursAgoAndKeepsYoungerRow() {
		saveCompletedRow(EXPIRED_COMPLETED_KEY, SAVED_25_HOURS_AGO);
		saveCompletedRow(FRESH_COMPLETED_KEY, SAVED_23_HOURS_AGO);

		int deletedCount = idempotencyKeyCleaner.deleteExpired();

		assertThat(deletedCount).isEqualTo(1);
		assertThat(remainingTestKeys()).containsExactly(FRESH_COMPLETED_KEY);
	}

	@Test
	@DisplayName("만료 키 삭제 작업은 응답이 채워지지 않은 처리 중 행(response_status null)도 25시간 전에 저장했으면 지우고 23시간 전에 저장한 처리 중 행은 남기며 지운 행 수 1을 돌려준다")
	void deletesProcessingRowSavedMoreThan24HoursAgoAndKeepsYoungerProcessingRow() {
		saveProcessingRow(EXPIRED_PROCESSING_KEY, SAVED_25_HOURS_AGO);
		saveProcessingRow(FRESH_PROCESSING_KEY, SAVED_23_HOURS_AGO);

		int deletedCount = idempotencyKeyCleaner.deleteExpired();

		assertThat(deletedCount).isEqualTo(1);
		assertThat(remainingTestKeys()).containsExactly(FRESH_PROCESSING_KEY);
		assertThat(responseStatusOf(FRESH_PROCESSING_KEY)).isNull();
	}

	private void saveCompletedRow(String key, Instant createdAt) {
		idempotencyKeyRepository.save(
				new IdempotencyKeyEntity(PrincipalType.USER, USER_ID, key, REQUEST_HASH, CREATED, RESPONSE_BODY));
		moveCreatedAt(key, createdAt);
	}

	private void saveProcessingRow(String key, Instant createdAt) {
		idempotencyKeyRepository.save(
				new IdempotencyKeyEntity(PrincipalType.USER, USER_ID, key, REQUEST_HASH, null, null));
		moveCreatedAt(key, createdAt);
	}

	private void moveCreatedAt(String key, Instant createdAt) {
		jdbcTemplate.update("update idempotency_key set created_at = ? where idempotency_key = ?",
				createdAt.atOffset(ZoneOffset.UTC), key);
	}

	private List<String> remainingTestKeys() {
		return jdbcTemplate.queryForList(
				"select idempotency_key from idempotency_key where idempotency_key in (?, ?, ?, ?)",
				String.class, TEST_KEYS.toArray());
	}

	private Integer responseStatusOf(String key) {
		return jdbcTemplate.queryForObject(
				"select response_status from idempotency_key where idempotency_key = ?", Integer.class, key);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

	}

}
