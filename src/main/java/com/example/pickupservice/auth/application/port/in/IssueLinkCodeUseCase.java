package com.example.pickupservice.auth.application.port.in;

public interface IssueLinkCodeUseCase {

	LinkCodeResult issueLinkCode(IssueLinkCodeCommand command);
}
