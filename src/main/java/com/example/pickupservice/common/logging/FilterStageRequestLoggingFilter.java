package com.example.pickupservice.common.logging;

import com.example.pickupservice.common.error.FilterStageEnd;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class FilterStageRequestLoggingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long startedAtNanos = System.nanoTime();
		filterChain.doFilter(request, response);
		if (FilterStageEnd.isMarked(request)) {
			log.info(RequestLogLine.COMPLETED_FORMAT,
					MDC.get(RequestIdFilter.MDC_KEY), RequestLogLine.filterStagePrincipalOf(request),
					request.getMethod(), RequestLogLine.pathPatternOf(request), response.getStatus(),
					TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos),
					RequestLogLine.errorCodeFieldOf(request), RequestLogLine.NO_FIELD);
		}
	}

}
