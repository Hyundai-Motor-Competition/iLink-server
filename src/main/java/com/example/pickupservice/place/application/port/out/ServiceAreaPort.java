package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;

public interface ServiceAreaPort {

	boolean isSupported(LatLng point);
}
