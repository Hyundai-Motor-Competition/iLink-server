package com.example.pickupservice.place.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SpotRankingRule {

	private static final int RANK_LIMIT = 3;

	private SpotRankingRule() {
	}

	public static List<RecommendedSpot> rank(List<SpotCandidate> candidates, List<Integer> driveSeconds) {
		boolean hasDrive = !driveSeconds.isEmpty();
		Comparator<Integer> byWalkMeters = Comparator.comparingInt(i -> candidates.get(i).walkMeters());
		Comparator<Integer> order = byWalkMeters;
		if (hasDrive) {
			order = Comparator.<Integer>comparingInt(i -> candidates.get(i).walkSeconds() + driveSeconds.get(i))
					.thenComparing(byWalkMeters);
		}
		List<Integer> picked = IntStream.range(0, candidates.size()).boxed()
				.sorted(order)
				.limit(RANK_LIMIT)
				.collect(Collectors.toList());

		List<RecommendedSpot> result = new ArrayList<>();
		for (int r = 0; r < picked.size(); r++) {
			int i = picked.get(r);
			SpotCandidate candidate = candidates.get(i);
			Integer drive = hasDrive ? driveSeconds.get(i) : null;
			Integer total = hasDrive ? candidate.walkSeconds() + driveSeconds.get(i) : null;
			result.add(new RecommendedSpot(r + 1, candidate, drive, total,
					SpotCautionRule.cautionsOf(candidate)));
		}
		return result;
	}
}
