package com.example.pickupservice.user.adapter.in.web;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.user.application.port.in.ChangeCallModeUseCase;
import com.example.pickupservice.user.application.port.in.GetMyFamilyUseCase;
import com.example.pickupservice.user.application.port.in.RegisterMemberUseCase;
import com.example.pickupservice.user.application.port.in.RemoveMemberUseCase;
import com.example.pickupservice.user.application.port.in.UnlinkVehicleUseCase;
import com.example.pickupservice.user.application.port.in.UpdateMemberUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class FamilyWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected GetMyFamilyUseCase getMyFamilyUseCase;

	@MockitoBean
	protected RegisterMemberUseCase registerMemberUseCase;

	@MockitoBean
	protected UpdateMemberUseCase updateMemberUseCase;

	@MockitoBean
	protected RemoveMemberUseCase removeMemberUseCase;

	@MockitoBean
	protected UnlinkVehicleUseCase unlinkVehicleUseCase;

	@MockitoBean
	protected ChangeCallModeUseCase changeCallModeUseCase;

}
