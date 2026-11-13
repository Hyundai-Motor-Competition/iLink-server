package com.example.pickupservice.common.logging;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RequestLoggingInterceptor implements AsyncHandlerInterceptor {

	private static final String STARTED_AT_NANOS = RequestLoggingInterceptor.class.getName() + ".startedAtNanos";
	private static final String STREAM_CLOSED_FIELD = " stream=closed";

	private final PollStateStore pollStateStore;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isRestControllerMethod(handler) && !isAsyncDispatch(request)) {
			request.setAttribute(STARTED_AT_NANOS, System.nanoTime());
		}
		return true;
	}

	@Override
	public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
			Object handler) {
		if (isLogged(request)) {
			log.info(RequestLogLine.STREAM_OPENED_FORMAT,
					MDC.get(RequestIdFilter.MDC_KEY), RequestLogLine.currentPrincipal(), request.getMethod(),
					RequestLogLine.pathPatternOf(request), response.getStatus());
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception exception) {
		if (isLogged(request) && !isLeftToSecurityHandler(exception)
				&& !isUnchangedPollState(request, response, handler)) {
			log.info(RequestLogLine.COMPLETED_FORMAT,
					MDC.get(RequestIdFilter.MDC_KEY), RequestLogLine.currentPrincipal(), request.getMethod(),
					RequestLogLine.pathPatternOf(request), response.getStatus(), elapsedMillisOf(request),
					RequestLogLine.errorCodeFieldOf(request), streamClosedFieldOf(request));
		}
	}

	private static boolean isLeftToSecurityHandler(Exception exception) {
		return exception instanceof AccessDeniedException || exception instanceof AuthenticationException;
	}

	private boolean isUnchangedPollState(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!isPollingMethod(handler) || !HttpStatusCode.valueOf(response.getStatus()).is2xxSuccessful()
				|| !(request.getAttribute(RequestLogAttributes.POLL_STATE) instanceof String state)) {
			return false;
		}
		return Objects.equals(pollStateStore.put(request.getRequestURI(), state), state);
	}

	private static boolean isPollingMethod(Object handler) {
		return handler instanceof HandlerMethod handlerMethod && handlerMethod.hasMethodAnnotation(PollingLog.class);
	}

	private static boolean isRestControllerMethod(Object handler) {
		return handler instanceof HandlerMethod handlerMethod
				&& AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RestController.class);
	}

	private static boolean isAsyncDispatch(HttpServletRequest request) {
		return request.getDispatcherType() == DispatcherType.ASYNC;
	}

	private static boolean isLogged(HttpServletRequest request) {
		return MDC.get(RequestIdFilter.MDC_KEY) != null && request.getAttribute(STARTED_AT_NANOS) instanceof Long;
	}

	private static String streamClosedFieldOf(HttpServletRequest request) {
		return isAsyncDispatch(request) ? STREAM_CLOSED_FIELD : RequestLogLine.NO_FIELD;
	}

	private static long elapsedMillisOf(HttpServletRequest request) {
		long startedAtNanos = (Long) request.getAttribute(STARTED_AT_NANOS);
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
	}

}
