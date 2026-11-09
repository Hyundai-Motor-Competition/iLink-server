package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.schedule.application.port.in.ChildQuery;
import com.example.pickupservice.schedule.application.port.in.CompleteSetupCommand;
import com.example.pickupservice.schedule.application.port.in.CompleteSetupUseCase;
import com.example.pickupservice.schedule.application.port.in.GetSetupSummaryUseCase;
import com.example.pickupservice.schedule.application.port.in.SetupCompletionResult;
import com.example.pickupservice.schedule.application.port.in.SetupSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteSetupService implements CompleteSetupUseCase {

	private final GetSetupSummaryUseCase getSetupSummaryUseCase;

	@Override
	@Transactional(readOnly = true)
	public SetupCompletionResult completeSetup(CompleteSetupCommand command) {
		SetupSummary summary = getSetupSummaryUseCase.getSetupSummary(
				new ChildQuery(command.familyId(), command.childId()));
		if (summary.settingsVersion() != command.settingsVersion()) {
			throw ScheduleErrors.settingsVersionConflict();
		}
		if (!summary.missing().isEmpty()) {
			throw ScheduleErrors.setupIncomplete();
		}
		return new SetupCompletionResult(true);
	}
}
