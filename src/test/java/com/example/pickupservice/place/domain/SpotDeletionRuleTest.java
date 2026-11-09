package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotDeletionRuleTest {

	private static final long FAMILY_ID = 1L;
	private static final long OTHER_FAMILY_ID = 2L;
	private static final long ACADEMY_ID = 55L;
	private static final long OTHER_ACADEMY_ID = 56L;
	private static final long GUARDIAN_ID = 100L;
	private static final LatLng STOP = new LatLng(37.501234, 127.039876);
	private static final LatLng MEETING = new LatLng(37.501301, 127.039901);

	@Test
	@DisplayName("삭제 규칙은 같은 학원에 다른 장소가 남아 있는 기본 장소의 삭제를 거절한다")
	void rejectsDeletingDefaultSpotWhenOtherSpotRemainsInAcademy() {
		PickupSpot target = spot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true);
		List<PickupSpot> existing = List.of(target, spot(11L, FAMILY_ID, ACADEMY_ID, "후문", false));

		assertThat(SpotDeletionRule.canDelete(target, existing)).isFalse();
	}

	@Test
	@DisplayName("삭제 규칙은 학원에 하나 남은 기본 장소의 삭제를 허용한다")
	void allowsDeletingLastDefaultSpotInAcademy() {
		PickupSpot target = spot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true);

		assertThat(SpotDeletionRule.canDelete(target, List.of(target))).isTrue();
	}

	@Test
	@DisplayName("삭제 규칙은 다른 가족 · 다른 학원 장소만 남아 있으면 기본 장소의 삭제를 허용한다")
	void allowsDeletingDefaultSpotWhenOnlyOtherFamilyOrAcademySpotsRemain() {
		PickupSpot target = spot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true);
		List<PickupSpot> existing = List.of(
				target,
				spot(11L, OTHER_FAMILY_ID, ACADEMY_ID, "남의 집 장소", false),
				spot(12L, FAMILY_ID, OTHER_ACADEMY_ID, "다른 학원 장소", false));

		assertThat(SpotDeletionRule.canDelete(target, existing)).isTrue();
	}

	@Test
	@DisplayName("삭제 규칙은 기본이 아닌 장소의 삭제를 허용한다")
	void allowsDeletingNonDefaultSpot() {
		PickupSpot target = spot(11L, FAMILY_ID, ACADEMY_ID, "후문", false);
		List<PickupSpot> existing = List.of(spot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true), target);

		assertThat(SpotDeletionRule.canDelete(target, existing)).isTrue();
	}

	private static PickupSpot spot(long spotId, long familyId, long academyId, String name, boolean isDefault) {
		return PickupSpot.restore(spotId, familyId, academyId, name, null, STOP, MEETING, isDefault, GUARDIAN_ID);
	}
}
