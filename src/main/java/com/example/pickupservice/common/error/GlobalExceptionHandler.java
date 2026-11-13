package com.example.pickupservice.common.error;

import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.DisconnectedClientHelper;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final List<ErrorCode> CLIENT_ERROR_CODES = List.of(ErrorCode.INVALID_REQUEST, ErrorCode.NOT_ACCEPTABLE);
	private static final String CLIENT_ERROR_MESSAGE = "요청을 처리할 수 없습니다";
	private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();

	@ExceptionHandler({
			HttpMessageNotReadableException.class,
			MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ErrorResponse> handleInvalidRequest() {
		return toResponse(ErrorCode.INVALID_REQUEST, null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		return toResponse(ErrorCode.VALIDATION_FAILED, ValidationFieldErrors.from(exception.getBindingResult()));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
		return toResponse(ErrorCode.VALIDATION_FAILED, ValidationFieldErrors.from(exception));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound() {
		return toResponse(ErrorCode.NOT_FOUND, null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported() {
		return toResponse(ErrorCode.METHOD_NOT_ALLOWED, null);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported() {
		return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
		return toJsonResponse(exception.status(), ErrorResponse.from(exception));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!TRUST_RESOLVER.isAuthenticated(authentication)) {
			throw exception;
		}
		return toResponse(ErrorCode.FORBIDDEN, null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public void rethrowSecurityException(AuthenticationException exception) {
		throw exception;
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
		if (DisconnectedClientHelper.isClientDisconnectedException(exception)) {
			log.debug("Client disconnected", exception);
			return null;
		}
		if (exception instanceof org.springframework.web.ErrorResponse clientError
				&& clientError.getStatusCode().is4xxClientError()) {
			return toClientErrorResponse(clientError);
		}
		log.error("Unhandled exception", exception);
		return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, null);
	}

	private ResponseEntity<ErrorResponse> toClientErrorResponse(org.springframework.web.ErrorResponse clientError) {
		HttpStatusCode status = clientError.getStatusCode();
		Optional<ErrorCode> errorCode = CLIENT_ERROR_CODES.stream()
				.filter(candidate -> candidate.getStatus().value() == status.value())
				.findFirst();
		String code = errorCode.map(ErrorCode::name).orElseGet(() -> statusName(status));
		String defaultMessage = errorCode.map(ErrorCode::getMessage).orElse(CLIENT_ERROR_MESSAGE);
		return toJsonResponse(status, ErrorResponse.of(code, reasonOr(clientError, defaultMessage), null));
	}

	private String statusName(HttpStatusCode status) {
		return status instanceof HttpStatus httpStatus ? httpStatus.name() : String.valueOf(status.value());
	}

	private String reasonOr(org.springframework.web.ErrorResponse clientError, String defaultMessage) {
		if (clientError instanceof ResponseStatusException exception && StringUtils.hasText(exception.getReason())) {
			return exception.getReason();
		}
		return defaultMessage;
	}

	private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, List<FieldError> fields) {
		return toJsonResponse(errorCode.getStatus(), ErrorResponse.of(errorCode, fields));
	}

	private ResponseEntity<ErrorResponse> toJsonResponse(HttpStatusCode status, ErrorResponse body) {
		RequestContextHolder.currentRequestAttributes()
				.setAttribute(ErrorCodeAttribute.NAME, body.error().code(), RequestAttributes.SCOPE_REQUEST);
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
	}

}
