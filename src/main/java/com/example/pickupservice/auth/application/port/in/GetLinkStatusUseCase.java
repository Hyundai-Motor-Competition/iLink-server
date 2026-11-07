package com.example.pickupservice.auth.application.port.in;

public interface GetLinkStatusUseCase {

	LinkStatusResult getLinkStatus(long codeId, long requesterVehicleId);
}
