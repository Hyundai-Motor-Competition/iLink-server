package com.example.pickupservice.place.adapter.in.web;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.application.port.in.AddPickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.DeletePickupSpotUseCase;
import com.example.pickupservice.place.application.port.in.ListPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.RecommendPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.UpdatePickupSpotUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class PlaceWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected ListPickupSpotsUseCase listPickupSpotsUseCase;

	@MockitoBean
	protected AddPickupSpotUseCase addPickupSpotUseCase;

	@MockitoBean
	protected UpdatePickupSpotUseCase updatePickupSpotUseCase;

	@MockitoBean
	protected DeletePickupSpotUseCase deletePickupSpotUseCase;

	@MockitoBean
	protected RecommendPickupSpotsUseCase recommendPickupSpotsUseCase;

}
