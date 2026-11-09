package com.example.pickupservice.pickup.application.port.out;

public interface ProcessedVehicleEventPort {

	boolean markProcessed(long sessionId, String vehicleEventId);

}
