package com.example.pickupservice.schedule.application.port.out;

import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;

import java.util.List;

public interface SearchAcademyPort {

	List<AcademyCandidate> search(String query);
}
