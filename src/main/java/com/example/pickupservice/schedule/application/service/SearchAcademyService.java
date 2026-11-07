package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import com.example.pickupservice.schedule.application.port.in.SearchAcademyUseCase;
import com.example.pickupservice.schedule.application.port.out.SearchAcademyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchAcademyService implements SearchAcademyUseCase {

	private final SearchAcademyPort searchAcademyPort;

	@Override
	public List<AcademyCandidate> searchAcademies(String query) {
		try {
			return searchAcademyPort.search(query);
		} catch (BusinessException e) {
			throw e;
		} catch (RuntimeException e) {
			throw ScheduleErrors.mapApiUnavailable(e);
		}
	}
}
