package com.example.pickupservice.common.web;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ValidationConfig {

	private static final String ALLOW_PARAMETER_CONSTRAINT_OVERRIDE = "hibernate.validator.allow_parameter_constraint_override";

	@Bean
	public ValidationConfigurationCustomizer allowParameterConstraintOverride() {
		return configuration -> configuration.addProperty(ALLOW_PARAMETER_CONSTRAINT_OVERRIDE, "true");
	}

}
