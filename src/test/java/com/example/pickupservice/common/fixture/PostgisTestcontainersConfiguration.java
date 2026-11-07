package com.example.pickupservice.common.fixture;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.SecureRandom;
import java.util.Base64;

@TestConfiguration(proxyBeanMethods = false)
public class PostgisTestcontainersConfiguration {

	private static final DockerImageName POSTGIS_IMAGE =
			DockerImageName.parse("postgis/postgis:17-3.7").asCompatibleSubstituteFor("postgres");
	private static final int MAX_CONNECTIONS = 200;
	private static final PostgreSQLContainer POSTGIS = new PostgreSQLContainer(POSTGIS_IMAGE)
			.withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=" + MAX_CONNECTIONS);
	private static final int TEST_POOL_MAX_SIZE = 3;
	private static final int TEST_POOL_MIN_IDLE = 1;
	private static final String JWT_SECRET_PROPERTY = "pickup.jwt.secret";
	private static final int JWT_SECRET_BYTES = 32;
	private static final String PHONE_KEY_PROPERTY = "pickup.crypto.phone-key";
	private static final int PHONE_KEY_BYTES = 32;
	private static final String IDEMPOTENCY_CLEANUP_INTERVAL_PROPERTY = "pickup.idempotency.cleanup-interval";
	private static final String SCHEDULER_IDLE_INTERVAL = "P1D";

	static {
		POSTGIS.start();
	}

	@Bean
	DynamicPropertyRegistrar postgisDataSourceRegistrar() {
		return registry -> {
			registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
			registry.add("spring.datasource.username", POSTGIS::getUsername);
			registry.add("spring.datasource.password", POSTGIS::getPassword);
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
