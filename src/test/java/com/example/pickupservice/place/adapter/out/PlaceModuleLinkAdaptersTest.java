package com.example.pickupservice.place.adapter.out;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.pickup.application.port.in.ActivePickupQueryUseCase;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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

	@MockitoBean
	private ActivePickupQueryUseCase activePickupQueryUseCase;

	@Test
	@DisplayName("CheckActivePickupPort는 isUsedByActiveSession은 pickup isSpotInUse, loadActiveChildLocation은 pickup activeChildLocation 결과를 그대로 돌려준다")
	void pickupActivityAdapterDelegatesToPickupUseCase() {
		CheckActivePickupPort pickupPort = onlyBeanOf(CheckActivePickupPort.class);
		LatLng childLocation = new LatLng(37.5665, 126.9780);
		given(activePickupQueryUseCase.isSpotInUse(11L)).willReturn(true);
		given(activePickupQueryUseCase.isSpotInUse(12L)).willReturn(false);
		given(activePickupQueryUseCase.activeChildLocation(21L)).willReturn(Optional.of(childLocation));
		given(activePickupQueryUseCase.activeChildLocation(22L)).willReturn(Optional.empty());

		assertThat(pickupPort.isUsedByActiveSession(11L)).isTrue();
		assertThat(pickupPort.isUsedByActiveSession(12L)).isFalse();
		assertThat(pickupPort.loadActiveChildLocation(21L)).contains(childLocation);
		assertThat(pickupPort.loadActiveChildLocation(22L)).isNotNull().isEmpty();
		verify(activePickupQueryUseCase).isSpotInUse(11L);
		verify(activePickupQueryUseCase).activeChildLocation(21L);
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
