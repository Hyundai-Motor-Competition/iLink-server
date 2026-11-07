package com.example.pickupservice.common.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	private static final String BEARER_SCHEME_NAME = "bearerAuth";

	@Bean
	OpenAPI pickupOpenApi() {
		return new OpenAPI()
				.info(new Info().title("Pickup Service API").version("v1"))
				.components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME, bearerJwtScheme()))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
	}

	private static SecurityScheme bearerJwtScheme() {
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");
	}

}
