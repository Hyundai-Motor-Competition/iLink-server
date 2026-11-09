package com.example.pickupservice.schedule.application.port.in;

public interface UpdateScheduleUseCase {

	ScheduleResult updateSchedule(UpdateScheduleCommand command);
}
