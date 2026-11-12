package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotRankingRuleTest {

	private static SpotCandidate spot(String name, int walkMeters, int walkSeconds) {
		return new SpotCandidate(SpotKind.CURB_STOP, name, new LatLng(37.5, 127.0),
				walkMeters, walkSeconds, LineKind.NONE, false);
	}

	@Test
	@DisplayName("추천 순위 규칙은 totalSeconds가 작은 순으로 3곳에 rank 1~3을 붙이고 totalSeconds를 걷는 시간과 차량 시간의 합으로 채운다")
	void ranksTopThreeByTotalSeconds() {
		List<SpotCandidate> candidates = List.of(
				spot("A", 100, 100),
				spot("B", 200, 200),
				spot("C", 300, 300),
				spot("D", 50, 50),
				spot("E", 400, 400));
		// 합: A=400, B=300, C=1000, D=1050, E=500
		List<Integer> drive = List.of(300, 100, 700, 1000, 100);

		List<RecommendedSpot> result = SpotRankingRule.rank(candidates, drive);

		assertThat(result).extracting(r -> r.candidate().name()).containsExactly("B", "A", "E");
		assertThat(result).extracting(RecommendedSpot::rank).containsExactly(1, 2, 3);
		assertThat(result).extracting(RecommendedSpot::totalSeconds).containsExactly(300, 400, 500);
		assertThat(result).extracting(RecommendedSpot::driveSeconds).containsExactly(100, 300, 100);
	}

	@Test
	@DisplayName("추천 순위 규칙은 totalSeconds가 같으면 walkMeters가 작은 쪽을 앞에 둔다")
	void tieBreaksByWalkMeters() {
		List<SpotCandidate> candidates = List.of(
				spot("먼곳", 300, 300),
				spot("가까운곳", 100, 100),
				spot("중간", 200, 200));
		// 합이 모두 500
		List<Integer> drive = List.of(200, 400, 300);

		List<RecommendedSpot> result = SpotRankingRule.rank(candidates, drive);

		assertThat(result).extracting(r -> r.candidate().name()).containsExactly("가까운곳", "중간", "먼곳");
		assertThat(result).extracting(RecommendedSpot::rank).containsExactly(1, 2, 3);
	}

	@Test
	@DisplayName("추천 순위 규칙은 후보가 3곳보다 적으면 있는 만큼만 돌려준다")
	void returnsFewerWhenCandidatesAreFewer() {
		List<SpotCandidate> candidates = List.of(spot("A", 100, 100), spot("B", 200, 200));

		List<RecommendedSpot> result = SpotRankingRule.rank(candidates, List.of(500, 100));

		assertThat(result).extracting(r -> r.candidate().name()).containsExactly("B", "A");
		assertThat(result).extracting(RecommendedSpot::rank).containsExactly(1, 2);
	}

	@Test
	@DisplayName("추천 순위 규칙은 driveSeconds가 없으면 totalSeconds를 null로 두고 walkMeters가 작은 순으로 3곳을 고른다")
	void ranksByWalkMetersWhenDriveSecondsMissing() {
		List<SpotCandidate> candidates = List.of(
				spot("D", 400, 364),
				spot("B", 200, 182),
				spot("A", 100, 91),
				spot("C", 300, 273));

		List<RecommendedSpot> result = SpotRankingRule.rank(candidates, List.of());

		assertThat(result).extracting(r -> r.candidate().name()).containsExactly("A", "B", "C");
		assertThat(result).extracting(RecommendedSpot::rank).containsExactly(1, 2, 3);
		assertThat(result).extracting(RecommendedSpot::driveSeconds).containsOnlyNulls();
		assertThat(result).extracting(RecommendedSpot::totalSeconds).containsOnlyNulls();
	}

	@Test
	@DisplayName("추천 순위 규칙은 driveSeconds가 없고 후보가 3곳보다 적으면 있는 만큼만 돌려준다")
	void returnsFewerWithoutDriveSeconds() {
		List<SpotCandidate> candidates = List.of(spot("B", 200, 182), spot("A", 100, 91));

		List<RecommendedSpot> result = SpotRankingRule.rank(candidates, List.of());

		assertThat(result).extracting(r -> r.candidate().name()).containsExactly("A", "B");
		assertThat(result).extracting(RecommendedSpot::rank).containsExactly(1, 2);
	}

	@Test
	@DisplayName("추천 순위 규칙은 각 결과의 cautions를 주의 표시 규칙으로 채운다")
	void fillsCautionsFromCautionRule() {
		SpotCandidate curb = new SpotCandidate(SpotKind.CURB_STOP, "길가", new LatLng(37.5, 127.0),
				100, 91, LineKind.DOTTED_YELLOW, true);
		SpotCandidate parking = new SpotCandidate(SpotKind.PUBLIC_PARKING, "주차장", new LatLng(37.5, 127.0),
				200, 182, null, false);

		List<RecommendedSpot> result = SpotRankingRule.rank(List.of(curb, parking), List.of(100, 100));

		assertThat(result.get(0).cautions()).containsExactly(
				SpotCaution.LINE_DATA_2021, SpotCaution.STOP_5MIN_ONLY, SpotCaution.ENFORCEMENT_CAMERA_NEAR);
		assertThat(result.get(1).cautions()).isEmpty();
	}
}
