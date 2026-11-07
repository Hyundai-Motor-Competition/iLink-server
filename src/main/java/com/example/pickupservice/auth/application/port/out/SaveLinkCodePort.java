package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.LinkCode;

public interface SaveLinkCodePort {

	LinkCode saveLinkCode(LinkCode linkCode);
}
