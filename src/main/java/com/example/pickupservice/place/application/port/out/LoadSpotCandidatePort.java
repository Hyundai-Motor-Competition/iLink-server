package com.example.pickupservice.place.application.port.out;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.domain.CurbStopCandidate;
import com.example.pickupservice.place.domain.PublicParking;

import java.util.List;

public interface LoadSpotCandidatePort {

	List<PublicParking> loadPublicParkingWithin(LatLng center, int radiusM);

	List<CurbStopCandidate> loadCurbStopsWithin(LatLng center, int radiusM);
}
