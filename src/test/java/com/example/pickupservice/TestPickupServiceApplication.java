package com.example.pickupservice;

import com.example.pickupservice.common.fixture.PostgisTestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestPickupServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PickupServiceApplication::main)
				.with(PostgisTestcontainersConfiguration.class)
				.withAdditionalProfiles("dev")
				.run(args);
	}

}
