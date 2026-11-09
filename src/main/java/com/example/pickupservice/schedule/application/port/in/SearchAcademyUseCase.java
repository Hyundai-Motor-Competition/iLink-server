package com.example.pickupservice.schedule.application.port.in;

import java.util.List;

public interface SearchAcademyUseCase {

	List<AcademyCandidate> searchAcademies(String query);
}
