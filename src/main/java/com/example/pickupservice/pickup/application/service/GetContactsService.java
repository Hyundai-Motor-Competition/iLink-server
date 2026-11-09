package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.ContactResult;
import com.example.pickupservice.pickup.application.port.in.GetContactsUseCase;
import com.example.pickupservice.pickup.application.port.in.SessionQuery;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadSchedulePort;
import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import com.example.pickupservice.pickup.domain.SessionChild;
import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.application.port.in.ScheduleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetContactsService implements GetContactsUseCase {

	private static final String ACADEMY_ROLE = "ACADEMY";

	private final LoadSessionPort loadSessionPort;
	private final LoadFamilyPort loadFamilyPort;
	private final LoadSchedulePort loadSchedulePort;

	@Override
	public List<ContactResult> getContacts(SessionQuery query) {
		PickupSession session = loadSessionPort.load(query.sessionId())
				.orElseThrow(PickupErrors::notFound);
		SessionAccess.requireVisible(session, query.principal());
		List<ContactResult> contacts = new ArrayList<>();
		loadFamilyPort.loadMembers(session.getFamilyId()).stream()
				.filter(member -> member.role() != Role.CHILD && member.phone() != null)
				.map(member -> new ContactResult(member.name(), member.role().name(), member.phone()))
				.forEach(contacts::add);
		contacts.addAll(academyContacts(session));
		return contacts;
	}

	private List<ContactResult> academyContacts(PickupSession session) {
		List<Long> scheduleIds = session.getChildren().stream()
				.map(SessionChild::getScheduleId)
				.toList();
		Set<Long> seenAcademyIds = new HashSet<>();
		return loadSchedulePort.loadSchedules(scheduleIds).stream()
				.map(ScheduleInfo::academy)
				.filter(academy -> seenAcademyIds.add(academy.academyId()))
				.map(GetContactsService::academyContact)
				.toList();
	}

	private static ContactResult academyContact(AcademyInfo academy) {
		return new ContactResult(academy.name(), ACADEMY_ROLE, academy.phone());
	}

}
