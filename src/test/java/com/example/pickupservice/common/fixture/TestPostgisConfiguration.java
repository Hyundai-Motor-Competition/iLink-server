package com.example.pickupservice.common.fixture;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 테스트 DB pickup_test(PostgreSQL 17 + PostGIS 3) 연결과 테스트 공통 설정.
 * 서버는 TestPostgis가 고른다: Windows는 내장 서버, 그 밖의 OS는 Testcontainers(pickup.test.db로 강제).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestPostgisConfiguration {

	private static final String DATABASE_NAME = "pickup_test";
	private static final int TEST_POOL_MAX_SIZE = 3;
	private static final int TEST_POOL_MIN_IDLE = 1;
	private static final String JWT_SECRET_PROPERTY = "pickup.jwt.secret";
	private static final int JWT_SECRET_BYTES = 32;
	private static final String PHONE_KEY_PROPERTY = "pickup.crypto.phone-key";
	private static final int PHONE_KEY_BYTES = 32;
	private static final String IDEMPOTENCY_CLEANUP_INTERVAL_PROPERTY = "pickup.idempotency.cleanup-interval";
	private static final String SCHEDULER_IDLE_INTERVAL = "P1D";

	@Bean
	JdbcConnectionDetails testPostgisConnectionDetails() {
		TestPostgis.Database database = TestPostgis.database(DATABASE_NAME);
		return new JdbcConnectionDetails() {

			@Override
			public String getUsername() {
				return database.username();
			}

			@Override
			public String getPassword() {
				return database.password();
			}

			@Override
			public String getJdbcUrl() {
				return database.jdbcUrl();
			}

		};
	}

	@Bean
	DynamicPropertyRegistrar testPoolSizeRegistrar() {
		return registry -> {
			registry.add("spring.datasource.hikari.maximum-pool-size", () -> TEST_POOL_MAX_SIZE);
			registry.add("spring.datasource.hikari.minimum-idle", () -> TEST_POOL_MIN_IDLE);
		};
	}

	@Bean
	DynamicPropertyRegistrar randomJwtSecretRegistrar() {
		String secret = randomBase64(JWT_SECRET_BYTES);
		return registry -> registry.add(JWT_SECRET_PROPERTY, () -> secret);
	}

	@Bean
	DynamicPropertyRegistrar randomPhoneKeyRegistrar() {
		String key = randomBase64(PHONE_KEY_BYTES);
		return registry -> registry.add(PHONE_KEY_PROPERTY, () -> key);
	}

	@Bean
	DynamicPropertyRegistrar idleSchedulerRegistrar() {
		return registry -> registry.add(IDEMPOTENCY_CLEANUP_INTERVAL_PROPERTY, () -> SCHEDULER_IDLE_INTERVAL);
	}

	private static String randomBase64(int bytes) {
		byte[] value = new byte[bytes];
		new SecureRandom().nextBytes(value);
		return Base64.getEncoder().encodeToString(value);
	}

}
