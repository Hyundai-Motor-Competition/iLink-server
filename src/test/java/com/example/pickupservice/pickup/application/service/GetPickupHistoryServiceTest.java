package com.example.pickupservice.pickup.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.security.AuthPrincipal;
import com.example.pickupservice.common.security.PrincipalType;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.pickup.application.port.in.HistoryItem;
import com.example.pickupservice.pickup.application.port.in.HistoryQuery;
import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.pickup.application.port.out.LoadHistoryPort;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetPickupHistoryServiceTest {

	private static final long FAMILY_ID = 12L;
	private static final long GUARDIAN_ID = 31L;
	private static final long CO_GUARDIAN_ID = 32L;
	private static final long MINJUN_ID = 33L;

	private static final long OTHER_FAMILY_ID = 13L;
	private static final long OTHER_FAMILY_CHILD_ID = 92L;

	private static final YearMonth NOVEMBER_2026 = YearMonth.of(2026, 11);
	private static final LocalDate NOVEMBER_FIRST = LocalDate.of(2026, 11, 1);
	private static final LocalDate NOVEMBER_LAST = LocalDate.of(2026, 11, 30);

	// 11월 3일 완료(한국 18:12:30), 11월 9일 취소
	private static final List<HistoryItem> NOVEMBER_ITEMS = List.of(
			new HistoryItem(LocalDate.of(2026, 11, 3), 480L, SessionStatus.COMPLETED,
					Instant.parse("2026-11-03T09:12:30Z")),
			new HistoryItem(LocalDate.of(2026, 11, 9), 501L, SessionStatus.CANCELLED, null));

	@Mock
	private LoadHistoryPort loadHistoryPort;

	@Mock
	private LoadFamilyPort loadFamilyPort;

	private GetPickupHistoryService getPickupHistoryService;

	@BeforeEach
	void setUp() {
		getPickupHistoryService = new GetPickupHistoryService(loadHistoryPort, loadFamilyPort);
		MemberInfo minjun = new MemberInfo(MINJUN_ID, FAMILY_ID, Role.CHILD, "민준", null);
		lenient().when(loadFamilyPort.loadMember(MINJUN_ID)).thenReturn(Optional.of(minjun));
		lenient().when(loadFamilyPort.loadMember(OTHER_FAMILY_CHILD_ID)).thenReturn(Optional.of(
				new MemberInfo(OTHER_FAMILY_CHILD_ID, OTHER_FAMILY_ID, Role.CHILD, "다른집", null)));
		lenient().when(loadFamilyPort.loadMembers(FAMILY_ID)).thenReturn(List.of(
				new MemberInfo(GUARDIAN_ID, FAMILY_ID, Role.GUARDIAN, "김보호", "010-1234-5678"),
				new MemberInfo(CO_GUARDIAN_ID, FAMILY_ID, Role.CO_GUARDIAN, "이보호", "010-2345-6789"),
				minjun));
		lenient().when(loadHistoryPort.loadHistory(MINJUN_ID, NOVEMBER_FIRST, NOVEMBER_LAST))
				.thenReturn(NOVEMBER_ITEMS);
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"GUARDIAN", "CO_GUARDIAN"})
	@DisplayName("픽업 이력은 month 2026-11이면 한국 날짜 2026-11-01 ~ 2026-11-30 범위로 그 자녀의 세션을 읽어 그대로 돌려준다")
	void historyLoadsWholeKoreanMonthOfChild(Role guardianRole) {
		long guardianId = guardianRole == Role.GUARDIAN ? GUARDIAN_ID : CO_GUARDIAN_ID;

		List<HistoryItem> items = getPickupHistoryService.history(query(guardianId, guardianRole, MINJUN_ID));

		verify(loadHistoryPort).loadHistory(MINJUN_ID, NOVEMBER_FIRST, NOVEMBER_LAST);
		assertThat(items).containsExactlyElementsOf(NOVEMBER_ITEMS);
	}

	@Test
	@DisplayName("픽업 이력은 다른 가족의 자녀면 NOT_FOUND로 거절하고 이력을 읽지 않는다")
	void historyOfOtherFamilyChildIsNotFound() {
		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> getPickupHistoryService.history(query(GUARDIAN_ID, Role.GUARDIAN, OTHER_FAMILY_CHILD_ID)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo("NOT_FOUND");
		verify(loadHistoryPort, never()).loadHistory(anyLong(), any(), any());
	}

	private static HistoryQuery query(long guardianId, Role role, long childId) {
		AuthPrincipal guardian = new AuthPrincipal(PrincipalType.USER, guardianId, FAMILY_ID, role);
		return new HistoryQuery(guardian, childId, NOVEMBER_2026);
	}

}
