package com.example.pickupservice.common.logging;

import com.example.pickupservice.common.error.ErrorCodeAttribute;
import com.example.pickupservice.common.security.CurrentAuthPrincipal;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class RequestLoggingInterceptor implements AsyncHandlerInterceptor {

	private static final String STARTED_AT_NANOS = RequestLoggingInterceptor.class.getName() + ".startedAtNanos";
	private static final String ANONYMOUS = "ANONYMOUS";
	private static final String STREAM_CLOSED_FIELD = " stream=closed";
	private static final String UNKNOWN_PATH = "unknown";

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
			log.info("requestId={} principal={} method={} path={} status={} stream=open",
					MDC.get(RequestIdFilter.MDC_KEY), principalOf(), request.getMethod(), pathPatternOf(request),
					response.getStatus());
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception exception) {
		if (isLogged(request) && !isUnchangedPollState(request, response, handler)) {
			log.info("requestId={} principal={} method={} path={} status={} durationMs={}{}{}",
					MDC.get(RequestIdFilter.MDC_KEY), principalOf(), request.getMethod(), pathPatternOf(request),
					response.getStatus(), elapsedMillisOf(request), errorCodeFieldOf(request),
					streamClosedFieldOf(request));
		}
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

	private static String principalOf() {
		return CurrentAuthPrincipal.find()
				.map(principal -> principal.type() + ":" + principal.id())
				.orElse(ANONYMOUS);
	}

	private static String pathPatternOf(HttpServletRequest request) {
		Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		return pattern == null ? UNKNOWN_PATH : String.valueOf(pattern);
	}

	private static String errorCodeFieldOf(HttpServletRequest request) {
		Object errorCode = request.getAttribute(ErrorCodeAttribute.NAME);
		return errorCode == null ? "" : " errorCode=" + errorCode;
	}

	private static String streamClosedFieldOf(HttpServletRequest request) {
		return isAsyncDispatch(request) ? STREAM_CLOSED_FIELD : "";
	}

	private static long elapsedMillisOf(HttpServletRequest request) {
		long startedAtNanos = (Long) request.getAttribute(STARTED_AT_NANOS);
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
	}

}
