package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorResponse;
import com.example.pickupservice.common.error.GlobalExceptionHandler;
import com.example.pickupservice.pickup.application.port.in.SessionView;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.example.pickupservice.pickup")
@RequiredArgsConstructor
public class PickupExceptionHandler {

	private final GlobalExceptionHandler globalExceptionHandler;

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
		ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusiness(exception);
		if (exception.session() instanceof SessionView view) {
			ErrorResponse body = new ErrorResponse(response.getBody().error(), SessionViewResponse.from(view));
			return new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode());
		}
		return response;
	}

}
