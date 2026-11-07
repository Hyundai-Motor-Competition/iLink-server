package com.example.pickupservice.common.idempotency;

import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

final class IdempotentOrderTestFixtures {

	static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	static final String VALIDATION_FAILED_MESSAGE = "입력값을 확인해 주세요";
	static final String REQUEST_BODY = """
			{"academyId":31,"name":"정문 앞","stopPoint":{"lat":37.501234,"lng":127.039876},"memo":null}""";
	static final String OTHER_REQUEST_BODY = """
			{"academyId":31,"name":"후문 앞","stopPoint":{"lat":37.502468,"lng":127.041122},"memo":"주차장 쪽"}""";

	private IdempotentOrderTestFixtures() {
	}

	static String bodyOf(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	record StopPoint(double lat, double lng) {
	}

	record OrderRequest(Long academyId, String name, StopPoint stopPoint, String memo) {
	}

	record OrderResponse(long orderNumber, Long academyId, String name, StopPoint stopPoint, String memo) {

		static OrderResponse of(long orderNumber, OrderRequest request) {
			return new OrderResponse(orderNumber, request.academyId(), request.name(), request.stopPoint(),
					request.memo());
		}

	}

}
