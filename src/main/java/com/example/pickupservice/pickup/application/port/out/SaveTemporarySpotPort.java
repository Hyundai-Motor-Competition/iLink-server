package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.common.geo.LatLng;

public interface SaveTemporarySpotPort {

	long saveTemporarySpot(long familyId, long academyId, String name, LatLng stopPoint, LatLng meetingPoint);

}
