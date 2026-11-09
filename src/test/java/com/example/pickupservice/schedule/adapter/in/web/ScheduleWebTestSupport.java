package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.schedule.application.port.in.CompleteSetupUseCase;
import com.example.pickupservice.schedule.application.port.in.CreateScheduleUseCase;
import com.example.pickupservice.schedule.application.port.in.GetSetupSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.GetVehicleSettingsSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.GetWeeklyPickupsUseCase;
import com.example.pickupservice.schedule.application.port.in.ListSchedulesUseCase;
import com.example.pickupservice.schedule.application.port.in.SearchAcademyUseCase;
import com.example.pickupservice.schedule.application.port.in.UpdateScheduleUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class ScheduleWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected SearchAcademyUseCase searchAcademyUseCase;

	@MockitoBean
	protected CreateScheduleUseCase createScheduleUseCase;

	@MockitoBean
	protected UpdateScheduleUseCase updateScheduleUseCase;

	@MockitoBean
	protected ListSchedulesUseCase listSchedulesUseCase;

	@MockitoBean
	protected GetSetupSummaryUseCase getSetupSummaryUseCase;

	@MockitoBean
	protected CompleteSetupUseCase completeSetupUseCase;

	@MockitoBean
	protected GetWeeklyPickupsUseCase getWeeklyPickupsUseCase;

	@MockitoBean
	protected GetVehicleSettingsSummaryUseCase getVehicleSettingsSummaryUseCase;

}
