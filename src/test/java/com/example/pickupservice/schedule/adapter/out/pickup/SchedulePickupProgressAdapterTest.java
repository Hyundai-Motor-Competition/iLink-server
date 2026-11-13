package com.example.pickupservice.schedule.adapter.out.pickup;

import com.example.pickupservice.pickup.application.port.in.PickupProgress;
import com.example.pickupservice.pickup.application.port.in.PickupProgressQueryUseCase;
import com.example.pickupservice.schedule.application.port.in.TodayPickupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SchedulePickupProgressAdapterTest {

	private static final long FAMILY_ID = 12L;
	private static final LocalDate TODAY_KST = LocalDate.of(2026, 11, 16);

	@Mock
	private PickupProgressQueryUseCase pickupProgressQueryUseCase;

	private SchedulePickupProgressAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new SchedulePickupProgressAdapter(pickupProgressQueryUseCase);
	}

	@Test
	@DisplayName("LoadPickupProgressPort는 pickup의 일정별 PickupProgress를 schedule TodayPickupStatus로 바꿔 돌려준다: IN_PROGRESS → IN_PROGRESS, COMPLETED → COMPLETED")
	void loadProgressConvertsPickupProgressToTodayPickupStatus() {
		given(pickupProgressQueryUseCase.findProgressBySchedule(FAMILY_ID, TODAY_KST)).willReturn(Map.of(
				801L, PickupProgress.IN_PROGRESS,
				802L, PickupProgress.COMPLETED));

		Map<Long, TodayPickupStatus> progress = adapter.loadProgress(FAMILY_ID, TODAY_KST);

		assertThat(progress).containsExactlyInAnyOrderEntriesOf(Map.of(
				801L, TodayPickupStatus.IN_PROGRESS,
				802L, TodayPickupStatus.COMPLETED));
	}

	@Test
	@DisplayName("LoadPickupProgressPort는 pickup이 빈 Map을 주면 빈 Map을 돌려준다")
	void loadProgressReturnsEmptyMapWhenPickupHasNoProgress() {
		given(pickupProgressQueryUseCase.findProgressBySchedule(FAMILY_ID, TODAY_KST)).willReturn(Map.of());

		assertThat(adapter.loadProgress(FAMILY_ID, TODAY_KST)).isNotNull().isEmpty();
	}
}
