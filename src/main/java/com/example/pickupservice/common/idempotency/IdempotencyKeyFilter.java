package com.example.pickupservice.common.idempotency;

import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.error.ErrorResponse.FieldError;
import com.example.pickupservice.common.error.ErrorResponseWriter;
import com.example.pickupservice.common.security.CurrentAuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyKeyFilter extends OncePerRequestFilter {

	private final IdempotencyKeyRequiredMatcher idempotencyKeyRequiredMatcher;
	private final ObjectProvider<IdempotencyKeyStore> idempotencyKeyStoreProvider;
	private final ErrorResponseWriter errorResponseWriter;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !idempotencyKeyRequiredMatcher.matches(request);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String key = request.getHeader(IdempotencyKeyHeader.NAME);
		Optional<FieldError> keyViolation = IdempotencyKeyHeader.violationOf(key);
		if (keyViolation.isPresent()) {
			errorResponseWriter.write(response, ErrorCode.VALIDATION_FAILED, List.of(keyViolation.get()));
			return;
		}
		CachedBodyRequest cachedRequest = new CachedBodyRequest(request);
		if (cachedRequest.multipart() && !partsReadable(cachedRequest)) {
			filterChain.doFilter(request, response);
			return;
		}
		filterIdempotently(cachedRequest, response, filterChain);
	}

	private static boolean partsReadable(HttpServletRequest request) {
		try {
			request.getParts();
			return true;
		} catch (IllegalStateException | IOException | ServletException unreadable) {
			return false;
		}
	}

	private void filterIdempotently(CachedBodyRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Optional<IdempotencyKeyEntity> reserved = reserveOrRespond(request, response);
		if (reserved.isEmpty()) {
			return;
		}
		ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
		try {
			filterChain.doFilter(request, cachedResponse);
			store().completeOrRelease(reserved.get(), cachedResponse);
		} catch (IOException | ServletException | RuntimeException e) {
			store().release(reserved.get());
			throw e;
		} finally {
			cachedResponse.copyBodyToResponse();
		}
	}

	private Optional<IdempotencyKeyEntity> reserveOrRespond(CachedBodyRequest request, HttpServletResponse response)
			throws IOException {
		IdempotentRequest idempotentRequest = idempotentRequestOf(request);
		try {
			Optional<IdempotencyKeyEntity> reserved = store().reserve(idempotentRequest);
			if (reserved.isEmpty()) {
				respondToExisting(idempotentRequest, response);
			}
			return reserved;
		} catch (DataAccessException | TransactionException storageFailure) {
			log.error("Idempotency-Key storage failed", storageFailure);
			errorResponseWriter.write(response, ErrorCode.INTERNAL_SERVER_ERROR);
			return Optional.empty();
		}
	}

	private void respondToExisting(IdempotentRequest request, HttpServletResponse response) throws IOException {
		Optional<IdempotencyKeyEntity> replayable = store().findReplayable(request);
		if (replayable.isEmpty()) {
			errorResponseWriter.write(response, ErrorCode.IDEMPOTENCY_KEY_REUSED);
			return;
		}
		replay(replayable.get(), response);
	}

	private IdempotencyKeyStore store() {
		return idempotencyKeyStoreProvider.getObject();
	}

	private IdempotentRequest idempotentRequestOf(CachedBodyRequest request) {
		return new IdempotentRequest(CurrentAuthPrincipal.find().orElse(null),
				request.getHeader(IdempotencyKeyHeader.NAME), RequestHasher.hash(request));
	}

	private static void replay(IdempotencyKeyEntity stored, HttpServletResponse response) throws IOException {
		response.setStatus(stored.getResponseStatus());
		if (stored.getResponseBody() == null) {
			return;
		}
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getOutputStream().write(stored.getResponseBody().getBytes(StandardCharsets.UTF_8));
	}

}
