package com.example.pickupservice.auth.application.port.out;

public interface IssueTokenPort {

	IssuedTokens issueTokens(TokenSubject subject);

	String hashRefreshToken(String refreshToken);
}
