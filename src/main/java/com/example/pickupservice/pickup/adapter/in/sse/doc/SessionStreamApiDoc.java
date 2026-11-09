package com.example.pickupservice.pickup.adapter.in.sse.doc;

import com.example.pickupservice.common.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "픽업 세션 실시간", description = "픽업 세션 SSE 연결")
public interface SessionStreamApiDoc {

	@Operation(summary = "세션 실시간 이벤트 (SES-08)",
			description = "그 세션의 차량 토큰. text/event-stream으로 이벤트를 보낸다. "
					+ "Last-Event-ID(마지막으로 받은 seq)를 보내면 그 다음 seq부터 저장된 이벤트를 먼저 보낸다")
	@Parameter(name = "Last-Event-ID", in = ParameterIn.HEADER, description = "마지막으로 받은 이벤트 seq")
	@ApiResponse(responseCode = "200", description = "text/event-stream")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "403", description = "FORBIDDEN (차량 토큰이 아님)")
	@ApiResponse(responseCode = "404", description = "NOT_FOUND (세션이 없거나 다른 차량의 것)")
	SseEmitter stream(@Parameter(hidden = true) AuthPrincipal principal, long sessionId,
			@Parameter(hidden = true) Long lastEventId);

}
