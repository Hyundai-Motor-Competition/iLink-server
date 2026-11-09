package com.example.pickupservice.notification.adapter.out.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {

	@Bean
	@ConditionalOnExpression("!'${pickup.fcm.credentials-path:}'.isEmpty()")
	public FirebaseMessaging firebaseMessaging(FcmProperties properties) throws IOException {
		try (InputStream credentials = new FileInputStream(properties.credentialsPath())) {
			FirebaseOptions options = FirebaseOptions.builder()
					.setProjectId(properties.projectId())
					.setCredentials(GoogleCredentials.fromStream(credentials))
					.build();
			return FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options));
		}
	}
}
