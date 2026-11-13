package com.example.pickupservice.user.adapter.in.web;

import jakarta.validation.constraints.NotNull;

public record ChangeChildLocationRequest(
		@NotNull Boolean enabled
) {
}
