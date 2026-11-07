package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.common.security.PublicEndpoint;
import com.example.pickupservice.common.security.PublicEndpoints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
public class AuthPublicEndpointsConfig {

	@Bean
	PublicEndpoints authPublicEndpoints() {
		return new PublicEndpoints(List.of(
				new PublicEndpoint(HttpMethod.POST, "/v1/auth/refresh"),
				new PublicEndpoint(HttpMethod.POST, "/v1/vehicle-devices"),
				new PublicEndpoint(HttpMethod.GET, "/v1/vehicle-link-codes/by-code/{code}"),
				new PublicEndpoint(HttpMethod.POST, "/v1/vehicle-links"),
				new PublicEndpoint(HttpMethod.GET, "/v1/invites/{token}"),
				new PublicEndpoint(HttpMethod.POST, "/v1/invites/{token}/acceptance")));
	}

}
