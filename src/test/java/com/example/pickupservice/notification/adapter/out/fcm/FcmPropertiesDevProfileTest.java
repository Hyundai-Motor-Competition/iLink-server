package com.example.pickupservice.notification.adapter.out.fcm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class FcmPropertiesDevProfileTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(BindingConfig.class);

	@Test
	@DisplayName("dev 프로필로 바인딩하면 테스트용 Firebase 프로젝트 id가 들어온다")
	void devProfileBindsTestFirebaseProjectId() {
		runner.withPropertyValues("spring.profiles.active=dev").run(context -> {
			assertThat(context).hasNotFailed();
			FcmProperties properties = context.getBean(FcmProperties.class);
			assertThat(properties.projectId()).isEqualTo("pickup-service-dev");
		});
	}

	@Test
	@DisplayName("dev 프로필에서 자격 증명 경로는 환경변수가 없으면 비어 있다")
	void devProfileCredentialsPathIsEmptyWithoutEnvironmentVariable() {
		runner.withPropertyValues("spring.profiles.active=dev", "FCM_CREDENTIALS_PATH=").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(FcmProperties.class).credentialsPath()).isEmpty();
		});
	}

	@Test
	@DisplayName("자격 증명 경로는 환경변수 FCM_CREDENTIALS_PATH에서 읽는다")
	void credentialsPathIsReadFromEnvironmentVariable() {
		runner.withPropertyValues("spring.profiles.active=dev", "FCM_CREDENTIALS_PATH=/secrets/fcm-dev.json")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBean(FcmProperties.class).credentialsPath())
							.isEqualTo("/secrets/fcm-dev.json");
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(FcmProperties.class)
	static class BindingConfig {
	}
}
