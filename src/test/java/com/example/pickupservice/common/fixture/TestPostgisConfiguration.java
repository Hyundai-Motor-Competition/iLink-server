package com.example.pickupservice.common.fixture;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@TestConfiguration(proxyBeanMethods = false)
public class TestPostgisConfiguration {

	private static final String DATABASE_NAME = "pickup_test";
	private static final int TEST_POOL_MAX_SIZE = 3;
	private static final int TEST_POOL_MIN_IDLE = 1;
	private static final String JWT_SECRET_PROPERTY = "pickup.jwt.secret";
	private static final int JWT_SECRET_BYTES = 32;
	private static final String PHONE_KEY_PROPERTY = "pickup.crypto.phone-key";
	private static final int PHONE_KEY_BYTES = 32;
	static final List<String> SCHEDULER_INTERVAL_PROPERTIES = List.of(
			"pickup.idempotency.cleanup-interval",
			"pickup.session.auto-complete-interval",
			"pickup.call.signal-lost-check-interval",
			"pickup.sse.ping-interval",
			"pickup.notification.resend-interval");
	static final String SCHEDULER_IDLE_INTERVAL = "P1D";

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
		return registry -> SCHEDULER_INTERVAL_PROPERTIES
				.forEach(property -> registry.add(property, () -> SCHEDULER_IDLE_INTERVAL));
	}

	private static String randomBase64(int bytes) {
		byte[] value = new byte[bytes];
		new SecureRandom().nextBytes(value);
		return Base64.getEncoder().encodeToString(value);
	}

}
