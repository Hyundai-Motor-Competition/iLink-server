package com.example.pickupservice.place.adapter.out;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class})
class PlaceModuleLinkAdaptersTest extends IntegrationTestSupport {

	private static final String PICKUP_ADAPTER_PACKAGE = "com.example.pickupservice.place.adapter.out.pickup.";
	private static final String SCHEDULE_ADAPTER_PACKAGE = "com.example.pickupservice.place.adapter.out.schedule.";
	/** 소수점 6자리까지 같다 = 차이가 0.0000005 미만. */
	private static final double SIX_DECIMALS = 0.0000005;
	private static final long MISSING_ACADEMY_ID = Long.MAX_VALUE - 1;

	private final ApplicationContext applicationContext;
	private final ScheduleSeed scheduleSeed;

	@Test
	@DisplayName("pickup 구현 전 임시 CheckActivePickupPort는 isUsedByActiveSession false, loadActiveChildLocation 빈 값을 돌려준다")
	void temporaryPickupActivityAdapterReportsNoActiveSession() {
		CheckActivePickupPort pickupPort = onlyBeanOf(CheckActivePickupPort.class);

		assertThat(pickupPort.isUsedByActiveSession(1L)).isFalse();
		assertThat(pickupPort.isUsedByActiveSession(9_999_999L)).isFalse();
		assertThat(pickupPort.loadActiveChildLocation(1L)).isNotNull().isEmpty();
		assertThat(pickupPort.loadActiveChildLocation(9_999_999L)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("LoadAcademyPort 어댑터는 schedule의 LoadAcademyUseCase.findAcademy 결과를 place 학원 정보(academyId · name · location)로 바꿔 돌려준다")
	void academyAdapterConvertsScheduleAcademyToPlaceAcademyInfo() {
		LoadAcademyPort academyPort = onlyBeanOf(LoadAcademyPort.class);
		ScheduleSeed.Result seeded = scheduleSeed.seed();
		try {
			AcademyInfo academy = academyPort.loadAcademy(seeded.academyId()).orElseThrow();

			assertThat(academy.academyId()).isEqualTo(seeded.academyId());
			assertThat(academy.name()).isEqualTo(ScheduleSeed.ACADEMY_NAME);
			assertThat(academy.location()).as("location은 학원 위치(입구 아님)").isNotNull();
			assertThat(academy.location().lat()).isCloseTo(ScheduleSeed.ACADEMY_LAT, within(SIX_DECIMALS));
			assertThat(academy.location().lng()).isCloseTo(ScheduleSeed.ACADEMY_LNG, within(SIX_DECIMALS));
		} finally {
			scheduleSeed.clear(seeded);
		}
	}

	@Test
	@DisplayName("LoadAcademyPort 어댑터는 schedule에 없는 학원이면 빈 값을 돌려준다")
	void academyAdapterReturnsEmptyForMissingAcademy() {
		LoadAcademyPort academyPort = onlyBeanOf(LoadAcademyPort.class);

		assertThat(academyPort.loadAcademy(MISSING_ACADEMY_ID)).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("place의 CheckActivePickupPort · LoadAcademyPort는 각각 빈이 정확히 1개이고 place.adapter.out.pickup · place.adapter.out.schedule 아래 클래스가 구현한다")
	void eachPortHasExactlyOneBeanInPlaceOutAdapterPackage() {
		assertThat(AopUtils.getTargetClass(onlyBeanOf(CheckActivePickupPort.class)).getName())
				.startsWith(PICKUP_ADAPTER_PACKAGE);
		assertThat(AopUtils.getTargetClass(onlyBeanOf(LoadAcademyPort.class)).getName())
				.startsWith(SCHEDULE_ADAPTER_PACKAGE);
	}

	private <T> T onlyBeanOf(Class<T> type) {
		Map<String, T> beans = applicationContext.getBeansOfType(type);
		assertThat(beans).as("%s 빈 개수", type.getName()).hasSize(1);
		return beans.values().iterator().next();
	}
}
