package com.example.pickupservice.place.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.in.RecommendPickupSpotsUseCase;
import com.example.pickupservice.place.application.port.in.RecommendSpotsQuery;
import com.example.pickupservice.place.application.port.in.SpotRecommendations;
import com.example.pickupservice.place.application.port.out.DriveTimePort;
import com.example.pickupservice.place.domain.RecommendedSpot;
import com.example.pickupservice.place.domain.SpotKind;
import com.example.pickupservice.schedule.adapter.out.persistence.AcademyEntity;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RequiredArgsConstructor
@Import(UserSeed.class)
class RecommendPickupSpotsServiceIntegrationTest extends IntegrationTestSupport {

	private static final LatLng DAECHI_ACADEMY = new LatLng(37.4998344, 127.0674141);
	private static final LatLng SEOUL_CITY_HALL = new LatLng(37.5665, 126.9780);
	private static final LatLng ORIGIN = new LatLng(37.49611, 127.05278);
	private static final Instant ARRIVE_AT = Instant.parse("2026-11-13T07:30:00Z");

	private static final String DAECHI_PLACE_ID = "test:recommend-daechi";
	private static final String CITY_HALL_PLACE_ID = "test:recommend-city-hall";

	private static final int DRIVE_SECONDS = 600;
	private static final int MAX_ITEMS = 3;
	private static final int MAX_CANDIDATES = 8;
	private static final int PARKING_RADIUS_METERS = 500;
	private static final int MAX_WALK_METERS = PARKING_RADIUS_METERS * 13 / 10;

	private final RecommendPickupSpotsUseCase recommendPickupSpotsUseCase;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final UserSeed userSeed;

	@MockitoBean
	private DriveTimePort driveTimePort;

	private long daechiAcademyId;
	private long cityHallAcademyId;
	private UserSeed.Result family;

	@BeforeEach
	void seedAcademies() {
		clearAcademies();
		family = userSeed.seed();
		daechiAcademyId = saveAcademy(DAECHI_PLACE_ID, "대치동 테스트학원", DAECHI_ACADEMY);
		cityHallAcademyId = saveAcademy(CITY_HALL_PLACE_ID, "시청 앞 테스트학원", SEOUL_CITY_HALL);
	}

	@AfterEach
	void clearFamily() {
		if (family != null) {
			userSeed.clear(family);
		}
	}

	@AfterEach
	void clearAcademies() {
		transactionTemplate.executeWithoutResult(status -> entityManager
				.createQuery("delete from AcademyEntity a where a.externalPlaceId in (:placeIds)")
				.setParameter("placeIds", List.of(DAECHI_PLACE_ID, CITY_HALL_PLACE_ID))
				.executeUpdate());
	}

	@Test
	@DisplayName("대치동 학원으로 추천하면 supported true이고 실제 참조 데이터의 공영주차장 은마아파트 · 길가 영동대로50길이 들어 있는 1~3곳을 rank 1부터 돌려준다")
	void recommendsPublicParkingAndCurbStopNearDaechiAcademyFromReferenceData() {
		given(driveTimePort.driveSecondsNow(any(), anyList()))
				.willAnswer(invocation -> sameDriveSecondsForEachDestination(invocation));

		SpotRecommendations result = recommendPickupSpotsUseCase
				.recommendSpots(new RecommendSpotsQuery(daechiAcademyId, ORIGIN, null));

		assertThat(result.supported()).as("supported").isTrue();
		List<RecommendedSpot> items = result.items();
		assertThat(items).as("items 개수").hasSizeBetween(1, MAX_ITEMS);
		assertThat(items).extracting(RecommendedSpot::rank).as("rank는 1부터 차례로")
				.containsExactlyElementsOf(IntStream.rangeClosed(1, items.size()).boxed().toList());
		assertThat(items).allSatisfy(item -> {
			assertThat(item.candidate().kind()).as("kind").isIn(SpotKind.PUBLIC_PARKING, SpotKind.CURB_STOP);
			assertThat(item.candidate().walkMeters()).as("walkMeters").isGreaterThan(0)
					.isLessThanOrEqualTo(MAX_WALK_METERS);
			assertThat(item.driveSeconds()).as("driveSeconds는 목이 돌려준 값").isEqualTo(DRIVE_SECONDS);
			assertThat(item.totalSeconds()).as("totalSeconds = walkSeconds + driveSeconds")
					.isEqualTo(item.candidate().walkSeconds() + DRIVE_SECONDS);
		});
		assertThat(items).extracting(item -> item.candidate().name(), item -> item.candidate().kind())
				.as("참조 데이터의 공영주차장 · 길가 후보")
				.contains(tuple("은마아파트", SpotKind.PUBLIC_PARKING), tuple("영동대로50길", SpotKind.CURB_STOP));
	}

	@Test
	@DisplayName("대치동 학원으로 추천하면 차량 시간 포트를 출발지와 후보 좌표(추천 수 이상 8곳 이하)로 1번만 부른다")
	void callsDriveTimePortOnceWithOriginAndCandidateLocations() {
		given(driveTimePort.driveSecondsNow(any(), anyList()))
				.willAnswer(invocation -> sameDriveSecondsForEachDestination(invocation));

		SpotRecommendations result = recommendPickupSpotsUseCase
				.recommendSpots(new RecommendSpotsQuery(daechiAcademyId, ORIGIN, null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<LatLng>> destinations = ArgumentCaptor.forClass(List.class);
		verify(driveTimePort).driveSecondsNow(eq(ORIGIN), destinations.capture());
		verify(driveTimePort, never()).driveSecondsAt(any(), anyList(), any());
		assertThat(destinations.getValue()).as("차량 시간을 구한 후보 수")
				.hasSizeBetween(result.items().size(), MAX_CANDIDATES);
		assertThat(destinations.getValue()).as("추천된 장소 좌표는 모두 차량 시간을 구한 목적지")
				.containsAll(result.items().stream().map(item -> item.candidate().location()).toList());
	}

	@Test
	@DisplayName("강남구 밖(서울시청) 학원으로 추천하면 supported false와 빈 items를 돌려주고 차량 시간 포트를 부르지 않는다")
	void returnsUnsupportedAndSkipsDriveTimePortForAcademyOutsideGangnam() {
		SpotRecommendations result = recommendPickupSpotsUseCase
				.recommendSpots(new RecommendSpotsQuery(cityHallAcademyId, ORIGIN, null));

		assertThat(result.supported()).as("supported").isFalse();
		assertThat(result.items()).as("items").isEmpty();
		verifyNoInteractions(driveTimePort);
	}

	@Test
	@DisplayName("arriveAt 없이 추천할 때 차량 시간 포트 driveSecondsNow가 불리는 순간 DB 트랜잭션이 열려 있지 않다")
	void noTransactionIsActiveWhenDriveSecondsNowIsCalled() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive())
				.as("전제: 테스트 자체는 트랜잭션 밖에서 돈다").isFalse();
		AtomicReference<Boolean> transactionActiveAtCall = new AtomicReference<>();
		given(driveTimePort.driveSecondsNow(any(), anyList())).willAnswer(invocation -> {
			transactionActiveAtCall.set(TransactionSynchronizationManager.isActualTransactionActive());
			return sameDriveSecondsForEachDestination(invocation);
		});

		recommendPickupSpotsUseCase.recommendSpots(new RecommendSpotsQuery(daechiAcademyId, ORIGIN, null));

		assertThat(transactionActiveAtCall.get()).as("driveSecondsNow가 불렸는지").isNotNull();
		assertThat(transactionActiveAtCall.get()).as("driveSecondsNow가 불린 순간 트랜잭션이 열려 있는지").isFalse();
	}

	@Test
	@DisplayName("arriveAt을 주고 추천할 때 차량 시간 포트 driveSecondsAt이 불리는 순간 DB 트랜잭션이 열려 있지 않다")
	void noTransactionIsActiveWhenDriveSecondsAtIsCalled() {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive())
				.as("전제: 테스트 자체는 트랜잭션 밖에서 돈다").isFalse();
		AtomicReference<Boolean> transactionActiveAtCall = new AtomicReference<>();
		given(driveTimePort.driveSecondsAt(any(), anyList(), any())).willAnswer(invocation -> {
			transactionActiveAtCall.set(TransactionSynchronizationManager.isActualTransactionActive());
			return sameDriveSecondsForEachDestination(invocation);
		});

		recommendPickupSpotsUseCase.recommendSpots(new RecommendSpotsQuery(daechiAcademyId, ORIGIN, ARRIVE_AT));

		assertThat(transactionActiveAtCall.get()).as("driveSecondsAt이 불렸는지").isNotNull();
		assertThat(transactionActiveAtCall.get()).as("driveSecondsAt이 불린 순간 트랜잭션이 열려 있는지").isFalse();
	}

	private static List<Integer> sameDriveSecondsForEachDestination(InvocationOnMock invocation) {
		List<LatLng> destinations = invocation.getArgument(1);
		return Collections.nCopies(destinations.size(), DRIVE_SECONDS);
	}

	private long saveAcademy(String externalPlaceId, String name, LatLng location) {
		return transactionTemplate.execute(status -> {
			AcademyEntity academy = new AcademyEntity(null, family.familyId(), externalPlaceId, name, "테스트 주소",
					GeoPoints.toPoint(location.lat(), location.lng()), null, null);
			entityManager.persist(academy);
			entityManager.flush();
			return academy.getAcademyId();
		});
	}
}
