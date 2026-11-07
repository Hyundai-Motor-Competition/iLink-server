package com.example.pickupservice.common.logging;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@ControllerAdvice
public class PollStateCaptureAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.hasMethodAnnotation(PollingLog.class);
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		String stateField = Objects.requireNonNull(returnType.getMethodAnnotation(PollingLog.class)).stateField();
		Optional<String> state = stateOf(body, stateField);
		if (state.isPresent() && request instanceof ServletServerHttpRequest servletRequest) {
			servletRequest.getServletRequest().setAttribute(RequestLogAttributes.POLL_STATE, state.get());
		}
		return body;
	}

	private static Optional<String> stateOf(Object body, String stateField) {
		if (body == null || !body.getClass().isRecord()) {
			return Optional.empty();
		}
		return Arrays.stream(body.getClass().getRecordComponents())
				.filter(component -> component.getName().equals(stateField))
				.findFirst()
				.map(component -> readValue(component, body))
				.map(value -> value instanceof Enum<?> constant ? constant.name() : value.toString());
	}

	private static Object readValue(RecordComponent component, Object body) {
		Method accessor = component.getAccessor();
		ReflectionUtils.makeAccessible(accessor);
		return ReflectionUtils.invokeMethod(accessor, body);
	}

}
