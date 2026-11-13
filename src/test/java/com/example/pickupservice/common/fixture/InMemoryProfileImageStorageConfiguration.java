package com.example.pickupservice.common.fixture;

import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class InMemoryProfileImageStorageConfiguration {

	@Bean
	@Primary
	InMemoryProfileImageStorage inMemoryProfileImageStorage() {
		return new InMemoryProfileImageStorage();
	}
}
