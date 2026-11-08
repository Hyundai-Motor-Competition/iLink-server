package com.example.pickupservice.place.application.port.out;

public class DefaultSpotConflictException extends RuntimeException {

	public DefaultSpotConflictException(Throwable cause) {
		super("같은 가족 · 학원에 기본 장소가 이미 있습니다", cause);
	}
}
