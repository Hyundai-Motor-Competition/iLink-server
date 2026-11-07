package com.example.pickupservice.common.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.Optional;

@Component
class IdempotencyKeyRequiredMatcher {

	private final RequestMappingHandlerMapping handlerMapping;

	IdempotencyKeyRequiredMatcher(
			@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
		this.handlerMapping = handlerMapping;
	}

	boolean matches(HttpServletRequest request) {
		RequestPath previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
		ServletRequestPathUtils.parseAndCache(request);
		try {
			return handlerMethodOf(request)
					.map(handlerMethod -> handlerMethod.hasMethodAnnotation(IdempotencyKeyRequired.class))
					.orElse(false);
		} finally {
			ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
		}
	}

	private Optional<HandlerMethod> handlerMethodOf(HttpServletRequest request) {
		try {
			HandlerExecutionChain chain = handlerMapping.getHandler(request);
			return Optional.ofNullable(chain)
					.map(HandlerExecutionChain::getHandler)
					.filter(HandlerMethod.class::isInstance)
					.map(HandlerMethod.class::cast);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
