package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.error.ErrorCode;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.NearbySpot;
import com.example.pickupservice.place.application.port.in.NearbySpots;
import com.example.pickupservice.place.application.port.in.RecommendSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotRecommendations;
import com.example.pickupservice.place.application.port.out.AcademyInfo;
import com.example.pickupservice.place.application.port.out.DriveTimePort;
import com.example.pickupservice.place.application.port.out.LoadAcademyPort;
import com.example.pickupservice.place.application.port.out.LoadSpotCandidatePort;
import com.example.pickupservice.place.application.port.out.ServiceAreaPort;
import com.example.pickupservice.place.domain.CurbStopCandidate;
import com.example.pickupservice.place.domain.LineKind;
import com.example.pickupservice.place.domain.PublicParking;
import com.example.pickupservice.place.domain.RecommendedSpot;
import com.example.pickupservice.place.domain.SpotCandidate;
import com.example.pickupservice.place.domain.SpotCandidateMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecommendPickupSpotsServiceTest {

	private static final long ACADEMY_ID = 55L;
	private static final LatLng ACADEMY = new LatLng(37.500000, 127.040000);
	private static final LatLng ORIGIN = new LatLng(37.520000, 127.060000);
	private static final Instant ARRIVE_AT = Instant.parse("2026-11-13T07:30:00Z");

	// 학원에서 북쪽으로 떨어진 거리 순서: 공영1(약 111m) < A로(약 222m) < 공영2(약 334m) < B로(약 445m)
	private static final LatLng PARKING_1 = new LatLng(37.501000, 127.040000);
	private static final LatLng CURB_A = new LatLng(37.502000, 127.040000);
	private static final LatLng PARKING_2 = new LatLng(37.503000, 127.040000);
	private static final LatLng CURB_B = new LatLng(37.504000, 127.040000);
	private static final List<LatLng> MERGED_LOCATIONS = List.of(PARKING_1, CURB_A, PARKING_2, CURB_B);

	@Mock
	private LoadAcademyPort loadAcademyPort;

	@Mock
	private ServiceAreaPort serviceAreaPort;

	@Mock
	private LoadSpotCandidatePort loadSpotCandidatePort;

	@Mock
	private DriveTimePort driveTimePort;

	private RecommendPickupSpotsService service;

	@BeforeEach
	void setUp() {
		service = new RecommendPickupSpotsService(loadAcademyPort, serviceAreaPort, loadSpotCandidatePort, driveTimePort);
	}

	private void givenAcademyInSupportedArea() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "강남학원", ACADEMY)));
		given(serviceAreaPort.isSupported(ACADEMY)).willReturn(true);
	}

	private void givenCandidates() {
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(List.of(
				new PublicParking("P2", "공영2", PARKING_2, 40, 30, 1000),
				new PublicParking("P1", "공영1", PARKING_1, 20, 30, 1500)));
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of(
				new CurbStopCandidate(2L, CURB_B, "B로", LineKind.NONE, false),
				new CurbStopCandidate(1L, CURB_A, "A로", LineKind.DOTTED_YELLOW, true)));
	}

	@Test
	@DisplayName("학원 좌표는 LoadAcademyPort가 돌려준 location이고 지원 지역 확인과 후보 조회에 그 좌표를 쓴다")
	void usesAcademyLocationReadFromLoadAcademyPort() {
		givenAcademyInSupportedArea();
		givenCandidates();
		given(driveTimePort.driveSecondsNow(ORIGIN, MERGED_LOCATIONS)).willReturn(List.of(600, 100, 300, 50));

		service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		verify(loadAcademyPort).loadAcademy(ACADEMY_ID);
		verify(serviceAreaPort).isSupported(ACADEMY);
		verify(loadSpotCandidatePort).loadPublicParkingWithin(ACADEMY, 500);
		verify(loadSpotCandidatePort).loadCurbStopsWithin(ACADEMY, 450);
	}

	@Test
	@DisplayName("학원이 없으면 NOT_FOUND로 거절하고 지원 지역 · 후보 조회 · 차량 시간 포트를 부르지 않는다")
	void rejectsWithNotFoundWhenAcademyDoesNotExist() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null)));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		verifyNoInteractions(serviceAreaPort, loadSpotCandidatePort, driveTimePort);
	}

	@Test
	@DisplayName("후보가 3곳보다 적으면 있는 만큼만 rank 1~2로 돌려준다")
	void returnsOnlyAvailableCandidatesWhenFewerThanThree() {
		givenAcademyInSupportedArea();
		// 공영주차장 2곳만 있고 길가는 없다. 합친 후보 순서(걷는 거리 순): 공영1, 공영2
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(List.of(
				new PublicParking("P2", "공영2", PARKING_2, 40, 30, 1000),
				new PublicParking("P1", "공영1", PARKING_1, 20, 30, 1500)));
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of());
		given(driveTimePort.driveSecondsNow(ORIGIN, List.of(PARKING_1, PARKING_2))).willReturn(List.of(100, 300));

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		assertThat(result.supported()).isTrue();
		List<RecommendedSpot> items = result.items();
		assertThat(items).hasSize(2);
		assertThat(items).extracting(RecommendedSpot::rank).containsExactly(1, 2);
		assertThat(items).extracting(item -> item.candidate().name()).containsExactly("공영1", "공영2");
	}

	@Test
	@DisplayName("후보가 0곳이면 supported true와 빈 items를 돌려주고 목적지가 없으니 차량 시간 포트를 부르지 않는다")
	void returnsSupportedWithEmptyItemsAndSkipsDriveTimeWhenNoCandidates() {
		givenAcademyInSupportedArea();
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(List.of());
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of());

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		assertThat(result.supported()).isTrue();
		assertThat(result.items()).isEmpty();
		verifyNoInteractions(driveTimePort);
	}

	@Test
	@DisplayName("학원 좌표가 지원 지역(강남구) 밖이면 supported false와 빈 items를 돌려주고 후보 조회 · 차량 시간 포트를 부르지 않는다")
	void returnsUnsupportedWithoutCallingExternalPorts() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "강남학원", ACADEMY)));
		given(serviceAreaPort.isSupported(ACADEMY)).willReturn(false);

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		assertThat(result.supported()).isFalse();
		assertThat(result.items()).isEmpty();
		verifyNoInteractions(loadSpotCandidatePort, driveTimePort);
	}

	@Test
	@DisplayName("공영주차장 2곳과 길가 2곳을 합쳐 totalSeconds가 작은 3곳을 rank 1~3으로 돌려주고 반경은 공영주차장 500 · 길가 450이다")
	void ranksThreeSmallestTotalSecondsFromMergedCandidates() {
		givenAcademyInSupportedArea();
		givenCandidates();
		// 합친 후보 순서(걷는 거리 순): 공영1, A로, 공영2, B로
		given(driveTimePort.driveSecondsNow(ORIGIN, MERGED_LOCATIONS)).willReturn(List.of(600, 100, 300, 50));

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		assertThat(result.supported()).isTrue();
		List<RecommendedSpot> items = result.items();
		assertThat(items).hasSize(3);
		assertThat(items).extracting(RecommendedSpot::rank).containsExactly(1, 2, 3);
		assertThat(items).extracting(item -> item.candidate().name()).containsExactly("A로", "B로", "공영2");
		assertThat(items).extracting(RecommendedSpot::driveSeconds).containsExactly(100, 50, 300);
		assertThat(items).extracting(RecommendedSpot::totalSeconds)
				.containsExactly(
						items.get(0).candidate().walkSeconds() + 100,
						items.get(1).candidate().walkSeconds() + 50,
						items.get(2).candidate().walkSeconds() + 300);
		assertThat(items.get(0).totalSeconds()).isLessThan(items.get(1).totalSeconds());
		assertThat(items.get(1).totalSeconds()).isLessThan(items.get(2).totalSeconds());
		verify(loadSpotCandidatePort).loadPublicParkingWithin(ACADEMY, 500);
		verify(loadSpotCandidatePort).loadCurbStopsWithin(ACADEMY, 450);
	}

	@Test
	@DisplayName("arriveAt이 없으면 driveSecondsNow만 부르고 driveSecondsAt은 부르지 않는다")
	void callsDriveSecondsNowWhenArriveAtIsNull() {
		givenAcademyInSupportedArea();
		givenCandidates();
		given(driveTimePort.driveSecondsNow(ORIGIN, MERGED_LOCATIONS)).willReturn(List.of(600, 100, 300, 50));

		service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		verify(driveTimePort).driveSecondsNow(ORIGIN, MERGED_LOCATIONS);
		verify(driveTimePort, never()).driveSecondsAt(any(), anyList(), any());
	}

	@Test
	@DisplayName("arriveAt이 있으면 같은 arriveAt으로 driveSecondsAt만 부르고 driveSecondsNow는 부르지 않는다")
	void callsDriveSecondsAtWhenArriveAtIsGiven() {
		givenAcademyInSupportedArea();
		givenCandidates();
		given(driveTimePort.driveSecondsAt(ORIGIN, MERGED_LOCATIONS, ARRIVE_AT)).willReturn(List.of(600, 100, 300, 50));

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, ARRIVE_AT));

		verify(driveTimePort).driveSecondsAt(ORIGIN, MERGED_LOCATIONS, ARRIVE_AT);
		verify(driveTimePort, never()).driveSecondsNow(any(), anyList());
		assertThat(result.items()).extracting(item -> item.candidate().name()).containsExactly("A로", "B로", "공영2");
	}

	@Test
	@DisplayName("도로 이름이 null · 빈 문자열 · 공백뿐인 길가 후보의 name은 「이름 없는 길」이고 이름 있는 길가 · 공영주차장 이름은 그대로다")
	void returnsUnnamedRoadNameForBlankRoadNames() {
		givenAcademyInSupportedArea();
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(List.of(
				new PublicParking("P1", "공영1", PARKING_1, 20, 30, 1500)));
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of(
				new CurbStopCandidate(1L, CURB_A, null, LineKind.NONE, false),
				new CurbStopCandidate(2L, PARKING_2, "", LineKind.NONE, false),
				new CurbStopCandidate(3L, CURB_B, "  ", LineKind.NONE, false),
				new CurbStopCandidate(4L, new LatLng(37.505000, 127.040000), "B로", LineKind.NONE, false)));
		given(driveTimePort.driveSecondsNow(any(), anyList())).willReturn(List.of());

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		assertThat(result.items()).extracting(item -> item.candidate().name())
				.containsExactly("공영1", "이름 없는 길", "이름 없는 길");
	}

	@Test
	@DisplayName("차량 시간 포트가 빈 목록을 돌려주면 driveSeconds · totalSeconds를 null로 두고 walkMeters가 작은 순 3곳을 돌려준다")
	void returnsByWalkMetersWithNullTimesWhenDriveTimesAreEmpty() {
		givenAcademyInSupportedArea();
		givenCandidates();
		given(driveTimePort.driveSecondsNow(ORIGIN, MERGED_LOCATIONS)).willReturn(List.of());

		SpotRecommendations result = service.recommendSpots(new RecommendSpotsQuery(ACADEMY_ID, ORIGIN, null));

		assertThat(result.supported()).isTrue();
		List<RecommendedSpot> items = result.items();
		assertThat(items).hasSize(3);
		assertThat(items).extracting(RecommendedSpot::rank).containsExactly(1, 2, 3);
		assertThat(items).extracting(item -> item.candidate().name()).containsExactly("공영1", "A로", "공영2");
		assertThat(items).extracting(RecommendedSpot::driveSeconds).containsOnlyNulls();
		assertThat(items).extracting(RecommendedSpot::totalSeconds).containsOnlyNulls();
		assertThat(items).extracting(item -> item.candidate().walkMeters()).isSorted();
	}

	// ---- SPOT-05 학원 기준 근처 추천 (#103)

	@Test
	@DisplayName("학원 기준 근처 추천은 공영주차장 500m · 길가 450m 후보를 합쳐 name · location · walkMeters · walkSeconds를 걷는 거리 짧은 순으로 돌려주고 카카오 길찾기(DriveTimePort)를 부르지 않는다")
	void recommendNearbyReturnsMergedCandidatesByWalkDistanceWithoutDriveTime() {
		givenAcademyInSupportedArea();
		givenCandidates();
		List<SpotCandidate> merged = SpotCandidateMerger.merge(ACADEMY,
				List.of(new PublicParking("P2", "공영2", PARKING_2, 40, 30, 1000),
						new PublicParking("P1", "공영1", PARKING_1, 20, 30, 1500)),
				List.of(new CurbStopCandidate(2L, CURB_B, "B로", LineKind.NONE, false),
						new CurbStopCandidate(1L, CURB_A, "A로", LineKind.DOTTED_YELLOW, true)));

		NearbySpots result = service.recommendNearbySpots(ACADEMY_ID);

		assertThat(result.supported()).isTrue();
		assertThat(result.items()).containsExactlyElementsOf(merged.stream()
				.map(c -> new NearbySpot(c.name(), c.location(), c.walkMeters(), c.walkSeconds()))
				.toList());
		assertThat(result.items()).extracting(NearbySpot::name).containsExactly("공영1", "A로", "공영2", "B로");
		assertThat(result.items()).extracting(NearbySpot::location).containsExactlyElementsOf(MERGED_LOCATIONS);
		assertThat(result.items()).extracting(NearbySpot::walkMeters).isSorted();
		assertThat(result.items()).allSatisfy(item -> assertThat(item.walkSeconds()).isPositive());
		verify(loadAcademyPort).loadAcademy(ACADEMY_ID);
		verify(serviceAreaPort).isSupported(ACADEMY);
		verify(loadSpotCandidatePort).loadPublicParkingWithin(ACADEMY, 500);
		verify(loadSpotCandidatePort).loadCurbStopsWithin(ACADEMY, 450);
		verifyNoInteractions(driveTimePort);
	}

	@Test
	@DisplayName("학원 기준 근처 추천은 합친 후보가 8곳을 넘으면 걷는 거리 짧은 순 8곳만 돌려준다")
	void recommendNearbyReturnsAtMostEightCandidates() {
		givenAcademyInSupportedArea();
		List<PublicParking> parkings = new ArrayList<>();
		for (int i = 10; i >= 1; i--) {
			parkings.add(new PublicParking("P" + i, "공영" + i, new LatLng(37.500000 + i * 0.0003, 127.040000), 10, 30, 1000));
		}
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(parkings);
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of());

		NearbySpots result = service.recommendNearbySpots(ACADEMY_ID);

		assertThat(result.supported()).isTrue();
		assertThat(result.items()).extracting(NearbySpot::name)
				.containsExactly("공영1", "공영2", "공영3", "공영4", "공영5", "공영6", "공영7", "공영8");
		verifyNoInteractions(driveTimePort);
	}

	@Test
	@DisplayName("학원 기준 근처 추천은 도로 이름이 빈 길가 후보의 name을 「이름 없는 길」로 돌려준다")
	void recommendNearbyReturnsUnnamedRoadNameForBlankRoadName() {
		givenAcademyInSupportedArea();
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(List.of());
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of(
				new CurbStopCandidate(1L, CURB_A, " ", LineKind.NONE, false)));

		NearbySpots result = service.recommendNearbySpots(ACADEMY_ID);

		assertThat(result.items()).extracting(NearbySpot::name).containsExactly("이름 없는 길");
	}

	@Test
	@DisplayName("학원 기준 근처 추천은 후보가 0곳이면 supported true와 빈 items를 돌려준다")
	void recommendNearbyReturnsSupportedWithEmptyItemsWhenNoCandidates() {
		givenAcademyInSupportedArea();
		given(loadSpotCandidatePort.loadPublicParkingWithin(ACADEMY, 500)).willReturn(List.of());
		given(loadSpotCandidatePort.loadCurbStopsWithin(ACADEMY, 450)).willReturn(List.of());

		NearbySpots result = service.recommendNearbySpots(ACADEMY_ID);

		assertThat(result.supported()).isTrue();
		assertThat(result.items()).isEmpty();
		verifyNoInteractions(driveTimePort);
	}

	@Test
	@DisplayName("학원 기준 근처 추천은 학원 좌표가 지원 지역 밖이면 supported false와 빈 items를 돌려주고 후보 조회 · 차량 시간 포트를 부르지 않는다")
	void recommendNearbyReturnsUnsupportedWithoutCallingCandidatePorts() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.of(new AcademyInfo(ACADEMY_ID, "강남학원", ACADEMY)));
		given(serviceAreaPort.isSupported(ACADEMY)).willReturn(false);

		NearbySpots result = service.recommendNearbySpots(ACADEMY_ID);

		assertThat(result.supported()).isFalse();
		assertThat(result.items()).isEmpty();
		verifyNoInteractions(loadSpotCandidatePort, driveTimePort);
	}

	@Test
	@DisplayName("학원 기준 근처 추천은 학원이 없으면 NOT_FOUND로 거절하고 지원 지역 · 후보 조회 · 차량 시간 포트를 부르지 않는다")
	void recommendNearbyRejectsWithNotFoundWhenAcademyDoesNotExist() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(BusinessException.class,
				() -> service.recommendNearbySpots(ACADEMY_ID));

		assertThat(exception).isNotNull();
		assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.code()).isEqualTo(ErrorCode.NOT_FOUND.name());
		verifyNoInteractions(serviceAreaPort, loadSpotCandidatePort, driveTimePort);
	}
}
