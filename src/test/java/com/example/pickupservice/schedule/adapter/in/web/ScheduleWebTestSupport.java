package com.example.pickupservice.schedule.adapter.in.web;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.schedule.application.port.in.SearchAcademyUseCase;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class ScheduleWebTestSupport extends IntegrationTestSupport {

	@MockitoBean
	protected SearchAcademyUseCase searchAcademyUseCase;

}
