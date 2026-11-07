package com.example.pickupservice.common.error;

import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Stream;

final class ValidationFieldErrors {

	private static final List<Class<? extends Annotation>> REQUEST_VALUE_ANNOTATIONS =
			List.of(RequestParam.class, RequestHeader.class, PathVariable.class);

	private ValidationFieldErrors() {
	}

	static List<FieldError> from(Errors errors) {
		return objectFieldErrors(errors).toList();
	}

	static List<FieldError> from(HandlerMethodValidationException exception) {
		return exception.getParameterValidationResults().stream()
				.flatMap(ValidationFieldErrors::parameterFieldErrors)
				.toList();
	}

	private static Stream<FieldError> parameterFieldErrors(ParameterValidationResult result) {
		if (result instanceof ParameterErrors errors) {
			return objectFieldErrors(errors);
		}
		String field = requestValueName(result.getMethodParameter());
		return result.getResolvableErrors().stream()
				.map(error -> new FieldError(field, error.getDefaultMessage()));
	}

	private static Stream<FieldError> objectFieldErrors(Errors errors) {
		return errors.getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()));
	}

	private static String requestValueName(MethodParameter parameter) {
		MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameterAnnotations());
		return REQUEST_VALUE_ANNOTATIONS.stream()
				.map(annotations::get)
				.filter(MergedAnnotation::isPresent)
				.map(annotation -> annotation.getString("name"))
				.filter(StringUtils::hasText)
				.findFirst()
				.orElseGet(parameter::getParameterName);
	}

}
