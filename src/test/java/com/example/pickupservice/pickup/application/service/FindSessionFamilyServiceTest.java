package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.pickup.application.port.out.LoadSessionPort;
import com.example.pickupservice.pickup.domain.PickupSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSessionFamilyServiceTest {

	private static final long SESSION_ID = 501L;
	private static final long FAMILY_ID = 1L;

	@Mock
	private LoadSessionPort loadSessionPort;
	@Mock
	private PickupSession session;

	@Test
	@DisplayName("세션이 있으면 그 세션의 familyId를 돌려준다")
	void returnsFamilyIdOfExistingSession() {
		when(session.getFamilyId()).thenReturn(FAMILY_ID);
		when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.of(session));
		FindSessionFamilyService service = new FindSessionFamilyService(loadSessionPort);

		assertThat(service.findFamilyId(SESSION_ID)).contains(FAMILY_ID);
	}

	@Test
	@DisplayName("세션이 없으면 빈 값을 돌려준다")
	void returnsEmptyForMissingSession() {
		when(loadSessionPort.load(SESSION_ID)).thenReturn(Optional.empty());
		FindSessionFamilyService service = new FindSessionFamilyService(loadSessionPort);

		assertThat(service.findFamilyId(SESSION_ID)).isNotNull().isEmpty();
	}
}
