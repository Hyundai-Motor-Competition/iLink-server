package com.example.pickupservice.pickup.adapter.in.web;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.in.AcknowledgeCallUseCase;
import com.example.pickupservice.pickup.application.port.in.CancelSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.ChangeSpotUseCase;
import com.example.pickupservice.pickup.application.port.in.CompletePickupUseCase;
import com.example.pickupservice.pickup.application.port.in.ConfirmRouteUseCase;
import com.example.pickupservice.pickup.application.port.in.CorrectOutcomeUseCase;
import com.example.pickupservice.pickup.application.port.in.ExtendPrepTimeUseCase;
import com.example.pickupservice.pickup.application.port.in.FindActiveSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.GetContactsUseCase;
import com.example.pickupservice.pickup.application.port.in.GetOutcomeUseCase;
import com.example.pickupservice.pickup.application.port.in.GetPickupHistoryUseCase;
import com.example.pickupservice.pickup.application.port.in.GetSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.HoldCallUseCase;
import com.example.pickupservice.pickup.application.port.in.HoldSessionUseCase;
import com.example.pickupservice.pickup.application.port.in.ListSessionEventsUseCase;
import com.example.pickupservice.pickup.application.port.in.OpenIncidentUseCase;
import com.example.pickupservice.pickup.application.port.in.ReceiveTelemetryUseCase;
import com.example.pickupservice.pickup.application.port.in.ReceiveVehicleEventUseCase;
import com.example.pickupservice.pickup.application.port.in.RejectBoardingUseCase;
import com.example.pickupservice.pickup.application.port.in.ReportChildLocationUseCase;
import com.example.pickupservice.pickup.application.port.in.RequestHelpUseCase;
import com.example.pickupservice.pickup.application.port.in.RevokeCallUseCase;
import com.example.pickupservice.pickup.application.port.in.SendCallUseCase;
import com.example.pickupservice.pickup.application.port.in.StartPickupUseCase;
import com.example.pickupservice.pickup.application.port.in.UpdateDataSettingsUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class PickupWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected FindActiveSessionUseCase findActiveSessionUseCase;

	@MockitoBean
	protected StartPickupUseCase startPickupUseCase;

	@MockitoBean
	protected GetSessionUseCase getSessionUseCase;

	@MockitoBean
	protected ConfirmRouteUseCase confirmRouteUseCase;

	@MockitoBean
	protected ChangeSpotUseCase changeSpotUseCase;

	@MockitoBean
	protected HoldSessionUseCase holdSessionUseCase;

	@MockitoBean
	protected CancelSessionUseCase cancelSessionUseCase;

	@MockitoBean
	protected ListSessionEventsUseCase listSessionEventsUseCase;

	@MockitoBean
	protected GetContactsUseCase getContactsUseCase;

	@MockitoBean
	protected CompletePickupUseCase completePickupUseCase;

	@MockitoBean
	protected RejectBoardingUseCase rejectBoardingUseCase;

	@MockitoBean
	protected OpenIncidentUseCase openIncidentUseCase;

	@MockitoBean
	protected ReceiveTelemetryUseCase receiveTelemetryUseCase;

	@MockitoBean
	protected ReceiveVehicleEventUseCase receiveVehicleEventUseCase;

	@MockitoBean
	protected ExtendPrepTimeUseCase extendPrepTimeUseCase;

	@MockitoBean
	protected ReportChildLocationUseCase reportChildLocationUseCase;

	@MockitoBean
	protected RequestHelpUseCase requestHelpUseCase;

	@MockitoBean
	protected HoldCallUseCase holdCallUseCase;

	@MockitoBean
	protected SendCallUseCase sendCallUseCase;

	@MockitoBean
	protected RevokeCallUseCase revokeCallUseCase;

	@MockitoBean
	protected AcknowledgeCallUseCase acknowledgeCallUseCase;

	@MockitoBean
	protected GetPickupHistoryUseCase getPickupHistoryUseCase;

	@MockitoBean
	protected GetOutcomeUseCase getOutcomeUseCase;

	@MockitoBean
	protected CorrectOutcomeUseCase correctOutcomeUseCase;

	@MockitoBean
	protected UpdateDataSettingsUseCase updateDataSettingsUseCase;

}
