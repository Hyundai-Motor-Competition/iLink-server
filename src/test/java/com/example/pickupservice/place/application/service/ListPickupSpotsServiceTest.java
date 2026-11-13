package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.AcademySpots;
import com.example.pickupservice.place.application.port.in.ListSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotGroups;
import com.example.pickupservice.place.application.port.in.SpotItem;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.CheckActivePickupPort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListPickupSpotsServiceTest {

	private static final long FAMILY_ID = 10L;
	private static final long GUARDIAN_ID = 1001L;
	private static final long ACADEMY_ID = 55L;
	private static final long OTHER_ACADEMY_ID = 77L;

	private static final LatLng ACADEMY_LOCATION = new LatLng(37.501000, 127.039500);
	private static final LatLng OTHER_ACADEMY_LOCATION = new LatLng(37.510000, 127.050000);
	private static final LatLng STOP = new LatLng(37.501234, 127.039876);
	private static final LatLng MEETING = new LatLng(37.501301, 127.039901);
	private static final LatLng NEAR = new LatLng(37.501200, 127.039800);

	private static final double NORTH_10M_LAT_DELTA = 0.00009;
	private static final double NORTH_300M_LAT_DELTA = 0.0026980;
	private static final double EAST_400M_LNG_DELTA = 0.0045343;

	@Mock
	private LoadSpotPort loadSpotPort;

	@Mock
	private LoadAcademyPort loadAcademyPort;

	@Mock
	private CheckActivePickupPort checkActivePickupPort;

	private ListPickupSpotsService listPickupSpotsService;

	@BeforeEach
	void setUp() {
		listPickupSpotsService = new ListPickupSpotsService(loadSpotPort, loadAcademyPort, checkActivePickupPort);
		lenient().when(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.thenReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", ACADEMY_LOCATION)));
		lenient().when(loadAcademyPort.loadAcademy(OTHER_ACADEMY_ID))
				.thenReturn(Optional.of(new AcademyInfo(OTHER_ACADEMY_ID, "바른수학학원", OTHER_ACADEMY_LOCATION)));
		lenient().when(checkActivePickupPort.loadActiveChildLocation(anyLong())).thenReturn(Optional.empty());
	}

	@Test
	@DisplayName("장소 조회는 학원별로 묶어 academyName과 함께 academyId · spotId 오름차순으로 돌려준다")
	void listSpotsGroupsSpotsByAcademyWithAcademyName() {
		PickupSpot otherAcademySpot = spot(31L, OTHER_ACADEMY_ID, "학원 건물 앞", "", false);
		PickupSpot backGate = spot(22L, ACADEMY_ID, "후문", null, false);
		PickupSpot frontGate = spot(21L, ACADEMY_ID, "정문 앞", "편의점 앞", true);
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of(otherAcademySpot, backGate, frontGate));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		assertThat(result.academies()).extracting(AcademySpots::academyId, AcademySpots::academyName)
				.containsExactly(
						tuple(ACADEMY_ID, "해솔영어학원"),
						tuple(OTHER_ACADEMY_ID, "바른수학학원"));
		AcademySpots haesol = result.academies().get(0);
		assertThat(haesol.spots()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("walkMinutes")
				.containsExactly(
						new SpotItem(21L, "정문 앞", STOP, MEETING, "편의점 앞", true, null),
						new SpotItem(22L, "후문", STOP, MEETING, null, false, null));
		assertThat(result.academies().get(1).spots()).extracting(SpotItem::spotId).containsExactly(31L);
		verify(loadAcademyPort, times(1)).loadAcademy(ACADEMY_ID);
		verify(loadAcademyPort, times(1)).loadAcademy(OTHER_ACADEMY_ID);
	}

	@Test
	@DisplayName("장소 조회는 academyId를 주면 그 학원 장소만 읽어 한 묶음으로 돌려준다")
	void listSpotsWithAcademyIdLoadsOnlyThatAcademy() {
		PickupSpot frontGate = spot(21L, ACADEMY_ID, "정문 앞", "편의점 앞", true);
		given(loadSpotPort.loadSpots(FAMILY_ID, ACADEMY_ID)).willReturn(List.of(frontGate));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, ACADEMY_ID, null, null));

		assertThat(result.academies()).hasSize(1);
		assertThat(result.academies().get(0).academyId()).isEqualTo(ACADEMY_ID);
		assertThat(result.academies().get(0).academyName()).isEqualTo("해솔영어학원");
		assertThat(result.academies().get(0).spots()).extracting(SpotItem::spotId).containsExactly(21L);
		verify(loadSpotPort, never()).loadSpotsWithin(anyLong(), any(), any(), anyInt());
	}

	@Test
	@DisplayName("장소 조회는 near · radiusM을 함께 주면 반경 조회 결과를 학원별로 묶어 돌려준다")
	void listSpotsWithNearAndRadiusUsesWithinQuery() {
		PickupSpot frontGate = spot(21L, ACADEMY_ID, "정문 앞", "편의점 앞", true);
		given(loadSpotPort.loadSpotsWithin(FAMILY_ID, null, NEAR, 300)).willReturn(List.of(frontGate));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, NEAR, 300));

		assertThat(result.academies()).extracting(AcademySpots::academyId).containsExactly(ACADEMY_ID);
		assertThat(result.academies().get(0).academyName()).isEqualTo("해솔영어학원");
		assertThat(result.academies().get(0).spots()).extracting(SpotItem::spotId).containsExactly(21L);
		verify(loadSpotPort, never()).loadSpots(anyLong(), any());
	}

	@Test
	@DisplayName("장소 조회는 장소가 없으면 빈 academies를 돌려준다")
	void listSpotsReturnsEmptyAcademiesWhenNoSpot() {
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of());

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		assertThat(result.academies()).isEmpty();
		verify(loadAcademyPort, never()).loadAcademy(anyLong());
	}

	@Test
	@DisplayName("장소 조회는 출입구가 있으면 출입구에서 만남 지점까지 블록 우회 거리(700m)를 초속 1.1m로 나눠 분으로 올림한 11분을 walkMinutes로 돌려준다")
	void listSpotsComputesWalkMinutesFromEntranceWhenAcademyHasEntrance() {
		LatLng entrance = new LatLng(37.502000, 127.040000);
		LatLng farLocation = new LatLng(37.511000, 127.040000);
		given(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", farLocation, entrance)));
		PickupSpot frontGate = spotMeeting(21L, ACADEMY_ID, northEast300x400From(entrance));
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of(frontGate));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		assertThat(result.academies().get(0).spots()).extracting(SpotItem::walkMinutes).containsExactly(11);
	}

	@Test
	@DisplayName("장소 조회는 출입구가 없으면 학원 위치에서 만남 지점까지 블록 우회 거리(700m)를 초속 1.1m로 나눠 분으로 올림한 11분을 walkMinutes로 돌려준다")
	void listSpotsComputesWalkMinutesFromAcademyLocationWhenNoEntrance() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", ACADEMY_LOCATION, null)));
		PickupSpot frontGate = spotMeeting(21L, ACADEMY_ID, northEast300x400From(ACADEMY_LOCATION));
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of(frontGate));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		assertThat(result.academies().get(0).spots()).extracting(SpotItem::walkMinutes).containsExactly(11);
	}

	@Test
	@DisplayName("장소 조회는 진행 중 세션이 없어도 walkMinutes를 계산하고, 만남 지점이 가까우면 1분 미만도 1분으로 올림한다")
	void listSpotsRoundsUpWalkMinutesAndComputesWithoutActiveSession() {
		LatLng nearMeeting = new LatLng(ACADEMY_LOCATION.lat() + NORTH_10M_LAT_DELTA, ACADEMY_LOCATION.lng());
		LatLng northOnlyMeeting = new LatLng(ACADEMY_LOCATION.lat() + NORTH_300M_LAT_DELTA, ACADEMY_LOCATION.lng());
		PickupSpot near = spotMeeting(21L, ACADEMY_ID, nearMeeting);
		PickupSpot northOnly = spotMeeting(22L, ACADEMY_ID, northOnlyMeeting);
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of(near, northOnly));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		assertThat(result.academies().get(0).spots())
				.extracting(SpotItem::spotId, SpotItem::walkMinutes)
				.containsExactly(tuple(21L, 1), tuple(22L, 5));
	}

	@Test
	@DisplayName("장소 조회는 학원마다 그 학원의 출입구 또는 위치를 기준으로 walkMinutes를 따로 계산하고, 학원 정보가 없으면 walkMinutes를 null로 돌려준다")
	void listSpotsComputesWalkMinutesPerAcademyAndNullForMissingAcademy() {
		long missingAcademyId = 99L;
		LatLng entrance = new LatLng(37.502000, 127.040000);
		given(loadAcademyPort.loadAcademy(ACADEMY_ID))
				.willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "해솔영어학원", ACADEMY_LOCATION, entrance)));
		given(loadAcademyPort.loadAcademy(missingAcademyId)).willReturn(Optional.empty());
		PickupSpot withEntrance = spotMeeting(21L, ACADEMY_ID, northEast300x400From(entrance));
		PickupSpot withLocation = spotMeeting(31L, OTHER_ACADEMY_ID, northEast300x400From(OTHER_ACADEMY_LOCATION));
		PickupSpot orphan = spotMeeting(41L, missingAcademyId, northEast300x400From(ACADEMY_LOCATION));
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of(withEntrance, withLocation, orphan));

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		List<SpotItem> allSpots = result.academies().stream().flatMap(group -> group.spots().stream()).toList();
		assertThat(allSpots).extracting(SpotItem::spotId, SpotItem::walkMinutes)
				.containsExactly(tuple(21L, 11), tuple(31L, 11), tuple(41L, null));
	}

	private static LatLng northEast300x400From(LatLng origin) {
		return new LatLng(origin.lat() + NORTH_300M_LAT_DELTA, origin.lng() + EAST_400M_LNG_DELTA);
	}

	private static PickupSpot spotMeeting(long spotId, long academyId, LatLng meetingPoint) {
		return PickupSpot.restore(spotId, FAMILY_ID, academyId, "정문 앞", null, STOP, meetingPoint, false, GUARDIAN_ID);
	}

	@Test
	@DisplayName("장소 조회는 학원 정보가 없는 학원의 장소도 academyName null로 묶어 돌려준다")
	void listSpotsGroupsSpotsOfMissingAcademyWithNullAcademyName() {
		long missingAcademyId = 99L;
		PickupSpot frontGate = spot(21L, ACADEMY_ID, "정문 앞", "편의점 앞", true);
		PickupSpot orphanDefault = spot(41L, missingAcademyId, "옛 학원 앞", null, true);
		PickupSpot orphanOther = spot(42L, missingAcademyId, "옛 학원 옆", null, false);
		given(loadSpotPort.loadSpots(FAMILY_ID, null)).willReturn(List.of(orphanOther, frontGate, orphanDefault));
		given(loadAcademyPort.loadAcademy(missingAcademyId)).willReturn(Optional.empty());

		SpotGroups result = listPickupSpotsService.listSpots(new ListSpotsQuery(FAMILY_ID, null, null, null));

		assertThat(result.academies()).extracting(AcademySpots::academyId, AcademySpots::academyName)
				.containsExactly(
						tuple(ACADEMY_ID, "해솔영어학원"),
						tuple(missingAcademyId, null));
		assertThat(result.academies().get(1).spots()).containsExactly(
				new SpotItem(41L, "옛 학원 앞", STOP, MEETING, null, true, null),
				new SpotItem(42L, "옛 학원 옆", STOP, MEETING, null, false, null));
		verify(loadAcademyPort, times(1)).loadAcademy(missingAcademyId);
	}

	private static PickupSpot spot(long spotId, long academyId, String name, String memo, boolean isDefault) {
		return PickupSpot.restore(spotId, FAMILY_ID, academyId, name, memo, STOP, MEETING, isDefault, GUARDIAN_ID);
	}
}
