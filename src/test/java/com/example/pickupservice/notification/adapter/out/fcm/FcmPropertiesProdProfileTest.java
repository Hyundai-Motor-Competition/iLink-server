package com.example.pickupservice.notification.adapter.out.fcm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class FcmPropertiesProdProfileTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(BindingConfig.class);

	@Test
	@DisplayName("prod 프로필에서 FCM project id는 환경변수 FCM_PROJECT_ID에서 읽는다")
	void prodProfileReadsProjectIdFromEnvironmentVariable() {
		runner.withPropertyValues("spring.profiles.active=prod", "FCM_PROJECT_ID=pickup-service-prod")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(FcmProperties.class).projectId())
							.isEqualTo("pickup-service-prod");
				});
	}

	@Test
	@DisplayName("prod 프로필에서 자격 증명 경로는 환경변수 FCM_CREDENTIALS_PATH에서 읽는다")
	void prodProfileReadsCredentialsPathFromEnvironmentVariable() {
		runner.withPropertyValues("spring.profiles.active=prod", "FCM_CREDENTIALS_PATH=/secrets/fcm-prod.json")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(FcmProperties.class).credentialsPath())
							.isEqualTo("/secrets/fcm-prod.json");
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(FcmProperties.class)
	static class BindingConfig {
	}
}
