package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.LinkCode;

import java.util.List;
import java.util.Optional;

public interface LoadLinkCodePort {

	Optional<LinkCode> loadLinkCode(long codeId);

	Optional<LinkCode> loadLinkCodeByCode(String code);

	List<LinkCode> loadIssuedCodes(long vehicleId);
}
