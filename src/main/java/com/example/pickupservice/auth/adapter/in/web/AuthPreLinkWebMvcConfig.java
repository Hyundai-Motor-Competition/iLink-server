package com.example.pickupservice.auth.adapter.in.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthPreLinkWebMvcConfig implements WebMvcConfigurer {

	private static final String API_PATH_PATTERN = "/v1/**";

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new PreLinkTokenInterceptor())
				.addPathPatterns(API_PATH_PATTERN)
				.order(Ordered.LOWEST_PRECEDENCE);
	}
}
