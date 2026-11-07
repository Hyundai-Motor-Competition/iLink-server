package com.example.pickupservice.common.security;

import org.springframework.http.HttpMethod;

public record PublicEndpoint(HttpMethod method, String pattern) {
}
