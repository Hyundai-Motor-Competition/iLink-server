package com.example.pickupservice.auth.adapter.in.web;

import com.example.pickupservice.auth.application.port.in.AcceptInviteUseCase;
import com.example.pickupservice.auth.application.port.in.ApproveLinkUseCase;
import com.example.pickupservice.auth.application.port.in.CheckLinkCodeUseCase;
import com.example.pickupservice.auth.application.port.in.GetInviteUseCase;
import com.example.pickupservice.auth.application.port.in.GetLinkStatusUseCase;
import com.example.pickupservice.auth.application.port.in.IssueInviteUseCase;
import com.example.pickupservice.auth.application.port.in.IssueLinkCodeUseCase;
import com.example.pickupservice.auth.application.port.in.LogoutVehicleUseCase;
import com.example.pickupservice.auth.application.port.in.RefreshTokenUseCase;
import com.example.pickupservice.auth.application.port.in.RegisterVehicleDeviceUseCase;
import com.example.pickupservice.auth.application.port.in.UpdatePushTokenUseCase;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class AuthWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected RefreshTokenUseCase refreshTokenUseCase;

	@MockitoBean
	protected RegisterVehicleDeviceUseCase registerVehicleDeviceUseCase;

	@MockitoBean
	protected UpdatePushTokenUseCase updatePushTokenUseCase;

	@MockitoBean
	protected LogoutVehicleUseCase logoutVehicleUseCase;

	@MockitoBean
	protected IssueLinkCodeUseCase issueLinkCodeUseCase;

	@MockitoBean
	protected GetLinkStatusUseCase getLinkStatusUseCase;

	@MockitoBean
	protected CheckLinkCodeUseCase checkLinkCodeUseCase;

	@MockitoBean
	protected ApproveLinkUseCase approveLinkUseCase;

	@MockitoBean
	protected IssueInviteUseCase issueInviteUseCase;

	@MockitoBean
	protected GetInviteUseCase getInviteUseCase;

	@MockitoBean
	protected AcceptInviteUseCase acceptInviteUseCase;

}
