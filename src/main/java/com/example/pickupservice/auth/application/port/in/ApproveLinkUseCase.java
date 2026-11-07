package com.example.pickupservice.auth.application.port.in;

public interface ApproveLinkUseCase {

	VehicleLinkResult approveLink(ApproveLinkCommand command);
}
