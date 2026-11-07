package com.example.pickupservice.auth.application.port.in;

import com.example.pickupservice.auth.domain.ConsentType;

public record ConsentAgreement(ConsentType type, boolean agreed) {
}
