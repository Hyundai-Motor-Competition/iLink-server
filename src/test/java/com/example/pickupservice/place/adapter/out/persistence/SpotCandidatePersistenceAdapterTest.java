package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.fixture.GeoOffsets;
import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.LoadSpotCandidatePort;
import com.example.pickupservice.place.domain.CurbStopCandidate;
import com.example.pickupservice.place.domain.LineKind;
import com.example.pickupservice.place.domain.PublicParking;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RequiredArgsConstructor
class SpotCandidatePersistenceAdapterTest extends IntegrationTestSupport {

	private static final LatLng CENTER = new LatLng(33.5, 126.5);
	private static final int RADIUS_M = 500;
	private static final double SIX_DECIMALS = 0.0000005;

	private static final String PARKING_IN = "TEST-P-IN";
	private static final String PARKING_IN_NULLS = "TEST-P-IN-NULL";
	private static final String PARKING_OUT = "TEST-P-OUT";
	private static final long CURB_IN_YELLOW = 900_000_001L;
	private static final long CURB_IN_NONE = 900_000_002L;
	private static final long CURB_OUT = 900_000_003L;

	private final LoadSpotCandidatePort loadSpotCandidatePort;
	private final JdbcTemplate jdbcTemplate;

	private LatLng parkingInLocation;
	private LatLng parkingNullsLocation;
	private LatLng curbYellowLocation;
	private LatLng curbNoneLocation;

	@BeforeEach
	void seed() {
		clear();
		parkingInLocation = GeoOffsets.northOf(CENTER, 499);
		parkingNullsLocation = new LatLng(CENTER.lat(), CENTER.lng() + 0.0001);
		insertParking(PARKING_IN, "테스트 공영주차장", parkingInLocation, 120, 30, 1500);
		insertParking(PARKING_IN_NULLS, "정보 없는 주차장", parkingNullsLocation, null, null, null);
		insertParking(PARKING_OUT, "먼 공영주차장", GeoOffsets.northOf(CENTER, 501), 10, 5, 500);

		curbYellowLocation = GeoOffsets.northOf(CENTER, 499);
		curbNoneLocation = new LatLng(CENTER.lat(), CENTER.lng() - 0.0001);
		insertCurb(CURB_IN_YELLOW, curbYellowLocation, "테헤란로", LineKind.DOTTED_YELLOW, true);
		insertCurb(CURB_IN_NONE, curbNoneLocation, null, LineKind.NONE, false);
		insertCurb(CURB_OUT, GeoOffsets.northOf(CENTER, 501), "먼 길", LineKind.NONE, false);
	}

	@AfterEach
	void clear() {
		jdbcTemplate.update("delete from public_parking where parking_id in (?, ?, ?)",
				PARKING_IN, PARKING_IN_NULLS, PARKING_OUT);
		jdbcTemplate.update("delete from curb_stop_candidate where candidate_id in (?, ?, ?)",
				CURB_IN_YELLOW, CURB_IN_NONE, CURB_OUT);
	}

	@Test
	@DisplayName("loadPublicParkingWithin은 중심에서 499m인 주차장은 돌려주고 501m인 주차장은 돌려주지 않는다")
	void loadPublicParkingWithinReturnsOnlyParkingInsideRadius() {
		List<PublicParking> parkings = loadSpotCandidatePort.loadPublicParkingWithin(CENTER, RADIUS_M);

		assertThat(parkings).extracting(PublicParking::parkingId)
				.containsExactlyInAnyOrder(PARKING_IN, PARKING_IN_NULLS);
	}

	@Test
	@DisplayName("loadPublicParkingWithin은 parkingId · name · 좌표 · spaces · baseMinutes · baseFeeWon을 도메인 값으로 옮긴다")
	void loadPublicParkingWithinMapsAllFields() {
		PublicParking parking = loadSpotCandidatePort.loadPublicParkingWithin(CENTER, RADIUS_M).stream()
				.filter(p -> p.parkingId().equals(PARKING_IN))
				.findFirst()
				.orElseThrow();

		assertThat(parking.name()).isEqualTo("테스트 공영주차장");
		assertThat(parking.location().lat()).isCloseTo(parkingInLocation.lat(), within(SIX_DECIMALS));
		assertThat(parking.location().lng()).isCloseTo(parkingInLocation.lng(), within(SIX_DECIMALS));
		assertThat(parking.spaces()).isEqualTo(120);
		assertThat(parking.baseMinutes()).isEqualTo(30);
		assertThat(parking.baseFeeWon()).isEqualTo(1500);
	}

	@Test
	@DisplayName("loadPublicParkingWithin은 spaces · baseMinutes · baseFeeWon이 NULL인 행을 null 그대로 돌려준다")
	void loadPublicParkingWithinKeepsNullNumbers() {
		PublicParking parking = loadSpotCandidatePort.loadPublicParkingWithin(CENTER, RADIUS_M).stream()
				.filter(p -> p.parkingId().equals(PARKING_IN_NULLS))
				.findFirst()
				.orElseThrow();

		assertThat(parking.spaces()).isNull();
		assertThat(parking.baseMinutes()).isNull();
		assertThat(parking.baseFeeWon()).isNull();
		assertThat(parking.location().lng()).isCloseTo(parkingNullsLocation.lng(), within(SIX_DECIMALS));
	}

	@Test
	@DisplayName("loadPublicParkingWithin은 반경 안에 주차장이 없으면 빈 목록을 돌려준다")
	void loadPublicParkingWithinReturnsEmptyWhenNothingNearby() {
		LatLng farAway = new LatLng(34.0, 126.5);

		assertThat(loadSpotCandidatePort.loadPublicParkingWithin(farAway, RADIUS_M)).isEmpty();
	}

	@Test
	@DisplayName("loadCurbStopsWithin은 중심에서 499m인 길가 후보는 돌려주고 501m인 후보는 돌려주지 않는다")
	void loadCurbStopsWithinReturnsOnlyCandidatesInsideRadius() {
		List<CurbStopCandidate> curbs = loadSpotCandidatePort.loadCurbStopsWithin(CENTER, RADIUS_M);

		assertThat(curbs).extracting(CurbStopCandidate::candidateId)
				.containsExactlyInAnyOrder(CURB_IN_YELLOW, CURB_IN_NONE);
	}

	@Test
	@DisplayName("loadCurbStopsWithin은 candidateId · 좌표 · roadName · lineKind · nearEnforcementCctv를 도메인 값으로 옮긴다")
	void loadCurbStopsWithinMapsAllFields() {
		CurbStopCandidate yellow = findCurb(CURB_IN_YELLOW);
		assertThat(yellow.location().lat()).isCloseTo(curbYellowLocation.lat(), within(SIX_DECIMALS));
		assertThat(yellow.location().lng()).isCloseTo(curbYellowLocation.lng(), within(SIX_DECIMALS));
		assertThat(yellow.roadName()).isEqualTo("테헤란로");
		assertThat(yellow.lineKind()).isEqualTo(LineKind.DOTTED_YELLOW);
		assertThat(yellow.nearEnforcementCctv()).isTrue();

		CurbStopCandidate none = findCurb(CURB_IN_NONE);
		assertThat(none.location().lng()).isCloseTo(curbNoneLocation.lng(), within(SIX_DECIMALS));
		assertThat(none.roadName()).isNull();
		assertThat(none.lineKind()).isEqualTo(LineKind.NONE);
		assertThat(none.nearEnforcementCctv()).isFalse();
	}

	@Test
	@DisplayName("loadCurbStopsWithin은 반경 안에 후보가 없으면 빈 목록을 돌려준다")
	void loadCurbStopsWithinReturnsEmptyWhenNothingNearby() {
		LatLng farAway = new LatLng(34.0, 126.5);

		assertThat(loadSpotCandidatePort.loadCurbStopsWithin(farAway, RADIUS_M)).isEmpty();
	}

	private CurbStopCandidate findCurb(long candidateId) {
		return loadSpotCandidatePort.loadCurbStopsWithin(CENTER, RADIUS_M).stream()
				.filter(c -> c.candidateId() == candidateId)
				.findFirst()
				.orElseThrow();
	}

	private void insertParking(String parkingId, String name, LatLng location, Integer spaces, Integer baseMinutes,
			Integer baseFeeWon) {
		jdbcTemplate.update("""
				insert into public_parking (parking_id, name, location, spaces, base_minutes, base_fee_won,
				                            created_at, updated_at)
				values (?, ?, st_setsrid(st_makepoint(?, ?), 4326)::geography, ?, ?, ?, now(), now())
				""", parkingId, name, location.lng(), location.lat(), spaces, baseMinutes, baseFeeWon);
	}

	private void insertCurb(long candidateId, LatLng location, String roadName, LineKind lineKind,
			boolean nearEnforcementCctv) {
		jdbcTemplate.update("""
				insert into curb_stop_candidate (candidate_id, location, road_name, line_kind, near_enforcement_cctv,
				                                 created_at, updated_at)
				values (?, st_setsrid(st_makepoint(?, ?), 4326)::geography, ?, ?, ?, now(), now())
				""", candidateId, location.lng(), location.lat(), roadName, lineKind.name(), nearEnforcementCctv);
	}
}
