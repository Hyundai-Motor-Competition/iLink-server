package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotCandidateMergerTest {

	private static final LatLng ACADEMY = new LatLng(37.5, 127.0);

	private static LatLng north(int step) {
		return new LatLng(37.5 + step * 0.001, 127.0);
	}

	private static CurbStopCandidate curb(long id, int step, String roadName) {
		return new CurbStopCandidate(id, north(step), roadName, LineKind.NONE, false);
	}

	private static PublicParking parking(String id, int step) {
		return new PublicParking(id, "주차장" + id, north(step), 100, 5, 430);
	}

	@Test
	@DisplayName("후보 합치기 규칙은 같은 도로의 길가 후보 2곳 중 학원에 가까운 쪽만 남긴다")
	void keepsNearestCurbStopPerRoad() {
		List<CurbStopCandidate> curbs = List.of(
				curb(1, 3, "테헤란로"),
				curb(2, 1, "테헤란로"));

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY, List.of(), curbs);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).location()).isEqualTo(north(1));
		assertThat(result.get(0).name()).isEqualTo("테헤란로");
		assertThat(result.get(0).kind()).isEqualTo(SpotKind.CURB_STOP);
	}

	@Test
	@DisplayName("후보 합치기 규칙은 roadName이 null인 길가 후보를 한 묶음으로 보고 가장 가까운 1곳만 남긴다")
	void treatsNullRoadNamesAsOneGroup() {
		List<CurbStopCandidate> curbs = List.of(
				curb(1, 4, null),
				curb(2, 2, null),
				curb(3, 3, null),
				curb(4, 5, "논현로"));

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY, List.of(), curbs);

		assertThat(result).extracting(SpotCandidate::location)
				.containsExactly(north(2), north(5));
	}

	@Test
	@DisplayName("후보 합치기 규칙은 도로가 5개면 학원에 가까운 길가 후보 4곳만 남긴다")
	void keepsAtMostFourCurbStops() {
		List<CurbStopCandidate> curbs = List.of(
				curb(1, 5, "도로E"),
				curb(2, 2, "도로B"),
				curb(3, 4, "도로D"),
				curb(4, 1, "도로A"),
				curb(5, 3, "도로C"));

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY, List.of(), curbs);

		assertThat(result).extracting(SpotCandidate::name)
				.containsExactly("도로A", "도로B", "도로C", "도로D");
	}

	@Test
	@DisplayName("후보 합치기 규칙은 길가 후보를 4곳으로 줄인 뒤 공영주차장과 합친다: 길가 5곳 + 주차장 4곳이면 8곳 중 길가는 4곳")
	void limitsCurbStopsBeforeMerging() {
		List<CurbStopCandidate> curbs = List.of(
				curb(1, 1, "도로A"),
				curb(2, 2, "도로B"),
				curb(3, 3, "도로C"),
				curb(4, 4, "도로D"),
				curb(5, 5, "도로E"));
		List<PublicParking> parkings = List.of(
				parking("P1", 6),
				parking("P2", 7),
				parking("P3", 8),
				parking("P4", 9));

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY, parkings, curbs);

		assertThat(result).hasSize(8);
		assertThat(result).extracting(SpotCandidate::name)
				.doesNotContain("도로E");
		assertThat(result).filteredOn(c -> c.kind() == SpotKind.CURB_STOP).hasSize(4);
		assertThat(result).filteredOn(c -> c.kind() == SpotKind.PUBLIC_PARKING).hasSize(4);
	}

	@Test
	@DisplayName("후보 합치기 규칙은 후보 10곳이면 walkMeters가 작은 8곳만 순서대로 남긴다")
	void keepsEightNearestSortedByWalkMeters() {
		List<PublicParking> parkings = List.of(
				parking("P9", 9),
				parking("P2", 2),
				parking("P6", 6),
				parking("P4", 4),
				parking("P10", 10),
				parking("P1", 1));
		List<CurbStopCandidate> curbs = List.of(
				curb(8, 8, "도로H"),
				curb(3, 3, "도로C"),
				curb(7, 7, "도로G"),
				curb(5, 5, "도로E"));

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY, parkings, curbs);

		assertThat(result).extracting(SpotCandidate::location)
				.containsExactly(north(1), north(2), north(3), north(4),
						north(5), north(6), north(7), north(8));
		assertThat(result).extracting(SpotCandidate::kind)
				.containsExactly(
						SpotKind.PUBLIC_PARKING, SpotKind.PUBLIC_PARKING, SpotKind.CURB_STOP, SpotKind.PUBLIC_PARKING,
						SpotKind.CURB_STOP, SpotKind.PUBLIC_PARKING, SpotKind.CURB_STOP, SpotKind.CURB_STOP);
		assertThat(result).extracting(SpotCandidate::walkMeters)
				.isSorted();
	}

	@Test
	@DisplayName("후보 합치기 규칙은 후보가 8곳 이하면 모두 돌려주고 후보가 없으면 빈 목록을 돌려준다")
	void keepsAllWhenAtMostEight() {
		assertThat(SpotCandidateMerger.merge(ACADEMY, List.of(), List.of())).isEmpty();

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY,
				List.of(parking("P1", 2), parking("P2", 1)), List.of(curb(1, 3, "도로A")));

		assertThat(result).hasSize(3);
	}

	@Test
	@DisplayName("후보 합치기 규칙은 후보마다 걷는 시간 규칙을 적용하고 공영주차장은 name · lineKind null, 길가는 roadName · lineKind · nearEnforcementCctv를 옮긴다")
	void fillsWalkTimeAndCandidateFields() {
		PublicParking parking = new PublicParking("P1", "개포 공영주차장", north(1), 80, 30, 1000);
		CurbStopCandidate curb = new CurbStopCandidate(7L, north(2), "도곡로", LineKind.DOTTED_YELLOW, true);

		List<SpotCandidate> result = SpotCandidateMerger.merge(ACADEMY, List.of(parking), List.of(curb));

		SpotCandidate first = result.get(0);
		assertThat(first.kind()).isEqualTo(SpotKind.PUBLIC_PARKING);
		assertThat(first.name()).isEqualTo("개포 공영주차장");
		assertThat(first.location()).isEqualTo(north(1));
		assertThat(first.lineKind()).isNull();
		assertThat(first.nearEnforcementCctv()).isFalse();
		assertThat(first.walkMeters()).isBetween(144, 146);
		assertThat(first.walkSeconds()).isEqualTo(ceilDivideBy1Point1(first.walkMeters()));

		SpotCandidate second = result.get(1);
		assertThat(second.kind()).isEqualTo(SpotKind.CURB_STOP);
		assertThat(second.name()).isEqualTo("도곡로");
		assertThat(second.lineKind()).isEqualTo(LineKind.DOTTED_YELLOW);
		assertThat(second.nearEnforcementCctv()).isTrue();
		assertThat(second.walkMeters()).isBetween(288, 292);
		assertThat(second.walkSeconds()).isEqualTo(ceilDivideBy1Point1(second.walkMeters()));
	}

	private static int ceilDivideBy1Point1(int meters) {
		return (meters * 10 + 10) / 11;
	}
}
