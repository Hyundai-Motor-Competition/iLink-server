package com.example.pickupservice.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final JsonAccessDeniedHandler accessDeniedHandler;
	private final AuthPrincipalJwtConverter authPrincipalJwtConverter;
	private final ObjectProvider<PublicEndpoints> publicEndpoints;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		RequestMatcher publicEndpointMatcher = publicEndpointMatcher();
		authorizeRequests(http, publicEndpointMatcher)
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenResolver(bearerTokenResolver(publicEndpointMatcher))
						.jwt(jwt -> jwt.jwtAuthenticationConverter(authPrincipalJwtConverter))
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	private static HttpSecurity authorizeRequests(HttpSecurity http, RequestMatcher publicEndpointMatcher) throws Exception {
		return http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
				.requestMatchers(publicEndpointMatcher).permitAll()
				.anyRequest().authenticated());
	}

	private RequestMatcher publicEndpointMatcher() {
		List<RequestMatcher> matchers = publicEndpoints.orderedStream()
				.flatMap(registered -> registered.endpoints().stream())
				.map(SecurityConfig::toMatcher)
				.toList();
		return matchers.isEmpty() ? request -> false : new OrRequestMatcher(matchers);
	}

	private static RequestMatcher toMatcher(PublicEndpoint endpoint) {
		return PathPatternRequestMatcher.withDefaults().matcher(endpoint.method(), endpoint.pattern());
	}

	private static BearerTokenResolver bearerTokenResolver(RequestMatcher publicEndpointMatcher) {
		DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
		return request -> publicEndpointMatcher.matches(request) ? null : defaultResolver.resolve(request);
	}

}
