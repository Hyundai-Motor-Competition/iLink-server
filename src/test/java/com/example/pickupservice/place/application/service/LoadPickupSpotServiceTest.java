package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.SpotInfo;
import com.example.pickupservice.place.application.port.in.SpotSummary;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoadPickupSpotServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long GUARDIAN_ID = 1001L;
	private static final long ACADEMY_ID = 55L;
	private static final long OTHER_ACADEMY_ID = 77L;
	private static final long UNLISTED_ACADEMY_ID = 99L;

	private static final LatLng STOP = new LatLng(37.501234, 127.039876);
	private static final LatLng MEETING = new LatLng(37.501301, 127.039901);

	@Mock
	private LoadSpotPort loadSpotPort;

	private LoadPickupSpotService loadPickupSpotService;

	@BeforeEach
	void setUp() {
		loadPickupSpotService = new LoadPickupSpotService(loadSpotPort);
	}

	@Test
	@DisplayName("findDefaultSpot은 가족 · 학원의 기본 장소를 SpotInfo로 돌려준다")
	void findDefaultSpotReturnsDefaultSpotOfFamilyAndAcademy() {
		PickupSpot frontGate = spot(21L, ACADEMY_ID, "정문 앞", true);
		given(loadSpotPort.loadDefaultSpot(FAMILY_ID, ACADEMY_ID)).willReturn(Optional.of(frontGate));

		Optional<SpotInfo> result = loadPickupSpotService.findDefaultSpot(FAMILY_ID, ACADEMY_ID);

		assertThat(result).contains(new SpotInfo(21L, FAMILY_ID, ACADEMY_ID, "정문 앞", STOP, MEETING));
	}

	@Test
	@DisplayName("findDefaultSpot은 가족 · 학원의 기본 장소가 없으면 빈 값을 돌려준다")
	void findDefaultSpotReturnsEmptyWhenNoDefaultSpot() {
		given(loadSpotPort.loadDefaultSpot(FAMILY_ID, ACADEMY_ID)).willReturn(Optional.empty());

		Optional<SpotInfo> result = loadPickupSpotService.findDefaultSpot(FAMILY_ID, ACADEMY_ID);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findSpot은 spotId의 장소를 SpotInfo로 돌려준다")
	void findSpotReturnsSpotOfSpotId() {
		PickupSpot backGate = spot(22L, ACADEMY_ID, "후문", false);
		given(loadSpotPort.loadSpot(22L)).willReturn(Optional.of(backGate));

		Optional<SpotInfo> result = loadPickupSpotService.findSpot(22L);

		assertThat(result).contains(new SpotInfo(22L, FAMILY_ID, ACADEMY_ID, "후문", STOP, MEETING));
	}

	@Test
	@DisplayName("findSpot은 spotId의 장소가 없으면 빈 값을 돌려준다")
	void findSpotReturnsEmptyWhenSpotMissing() {
		given(loadSpotPort.loadSpot(404L)).willReturn(Optional.empty());

		Optional<SpotInfo> result = loadPickupSpotService.findSpot(404L);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findSpots는 가족의 학원 목록에 속한 장소만 spotId · academyId · name · isDefault로 돌려주고 장소 조회는 1회만 한다")
	void findSpotsReturnsSummariesOfSpotsInAcademyList() {
		PickupSpot frontGate = spot(21L, ACADEMY_ID, "정문 앞", true);
		PickupSpot backGate = spot(22L, ACADEMY_ID, "후문", false);
		PickupSpot otherAcademySpot = spot(31L, OTHER_ACADEMY_ID, "학원 건물 앞", true);
		PickupSpot unlistedAcademySpot = spot(41L, UNLISTED_ACADEMY_ID, "목록 밖 학원", true);
		given(loadSpotPort.loadSpots(FAMILY_ID, null))
				.willReturn(List.of(frontGate, backGate, otherAcademySpot, unlistedAcademySpot));

		List<SpotSummary> result = loadPickupSpotService.findSpots(FAMILY_ID, List.of(ACADEMY_ID, OTHER_ACADEMY_ID));

		assertThat(result).containsExactlyInAnyOrder(
				new SpotSummary(21L, ACADEMY_ID, "정문 앞", true),
				new SpotSummary(22L, ACADEMY_ID, "후문", false),
				new SpotSummary(31L, OTHER_ACADEMY_ID, "학원 건물 앞", true));
		verify(loadSpotPort, times(1)).loadSpots(anyLong(), any());
	}

	@Test
	@DisplayName("findSpots는 학원 목록이 비면 장소를 읽지 않고 빈 목록을 돌려준다")
	void findSpotsReturnsEmptyWithoutLoadingWhenAcademyListEmpty() {
		List<SpotSummary> result = loadPickupSpotService.findSpots(FAMILY_ID, List.of());

		assertThat(result).isEmpty();
		verify(loadSpotPort, never()).loadSpots(anyLong(), any());
	}

	private static PickupSpot spot(long spotId, long academyId, String name, boolean isDefault) {
		return PickupSpot.restore(spotId, FAMILY_ID, academyId, name, null, STOP, MEETING, isDefault, GUARDIAN_ID);
	}
}
