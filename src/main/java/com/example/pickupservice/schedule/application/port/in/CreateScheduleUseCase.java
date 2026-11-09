package com.example.pickupservice.schedule.application.port.in;

public interface CreateScheduleUseCase {

	ScheduleResult createSchedule(CreateScheduleCommand command);
}
