package com.example.pickupservice.place.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DefaultSpotRuleTest {

	private static final long FAMILY_ID = 1L;
	private static final long OTHER_FAMILY_ID = 2L;
	private static final long ACADEMY_ID = 55L;
	private static final long OTHER_ACADEMY_ID = 56L;
	private static final long GUARDIAN_ID = 100L;
	private static final LatLng STOP = new LatLng(37.501234, 127.039876);
	private static final LatLng MEETING = new LatLng(37.501301, 127.039901);

	@Test
	@DisplayName("기본 장소 규칙은 가족 · 학원의 첫 장소를 isDefault false로 보내도 isDefault true로 만든다")
	void makesFirstSpotOfFamilyAcademyDefault() {
		PickupSpot candidate = newSpot(FAMILY_ID, ACADEMY_ID, "정문 앞", false);

		PickupSpot result = DefaultSpotRule.applyFirstSpotDefault(candidate, List.of());

		assertThat(result.isDefault()).isTrue();
		assertThat(result.getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(result.getAcademyId()).isEqualTo(ACADEMY_ID);
		assertThat(result.getName()).isEqualTo("정문 앞");
		assertThat(result.getStopPoint()).isEqualTo(STOP);
		assertThat(result.getMeetingPoint()).isEqualTo(MEETING);
		assertThat(result.getCreatedBy()).isEqualTo(GUARDIAN_ID);
	}

	@Test
	@DisplayName("기본 장소 규칙은 다른 가족 · 다른 학원 장소만 있으면 첫 장소로 보고 isDefault true로 만든다")
	void treatsSpotAsFirstWhenOnlyOtherFamilyOrAcademySpotsExist() {
		PickupSpot candidate = newSpot(FAMILY_ID, ACADEMY_ID, "정문 앞", false);
		List<PickupSpot> existing = List.of(
				existingSpot(10L, OTHER_FAMILY_ID, ACADEMY_ID, "남의 집 장소", true),
				existingSpot(11L, FAMILY_ID, OTHER_ACADEMY_ID, "다른 학원 장소", true));

		PickupSpot result = DefaultSpotRule.applyFirstSpotDefault(candidate, existing);

		assertThat(result.isDefault()).isTrue();
	}

	@Test
	@DisplayName("기본 장소 규칙은 같은 가족 · 학원에 장소가 이미 있으면 isDefault false를 그대로 둔다")
	void keepsNonDefaultWhenFamilyAcademyAlreadyHasSpot() {
		PickupSpot candidate = newSpot(FAMILY_ID, ACADEMY_ID, "후문", false);
		List<PickupSpot> existing = List.of(existingSpot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true));

		PickupSpot result = DefaultSpotRule.applyFirstSpotDefault(candidate, existing);

		assertThat(result.isDefault()).isFalse();
	}

	@Test
	@DisplayName("기본 장소 규칙은 새 기본 장소를 지정하면 같은 가족 · 학원의 이전 기본 장소를 false로 바꿔 돌려준다")
	void demotesPreviousDefaultOfSameFamilyAcademy() {
		PickupSpot newDefault = newSpot(FAMILY_ID, ACADEMY_ID, "후문", true);
		List<PickupSpot> existing = List.of(
				existingSpot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true),
				existingSpot(11L, FAMILY_ID, ACADEMY_ID, "편의점 앞", false));

		List<PickupSpot> changed = DefaultSpotRule.demotePreviousDefaults(newDefault, existing);

		assertThat(changed)
				.extracting(PickupSpot::getSpotId, PickupSpot::getName, PickupSpot::isDefault)
				.containsExactly(tuple(10L, "정문 앞", false));
	}

	@Test
	@DisplayName("기본 장소 규칙은 다른 가족 · 다른 학원의 기본 장소는 바꾸지 않는다")
	void doesNotDemoteDefaultOfOtherFamilyOrAcademy() {
		PickupSpot newDefault = newSpot(FAMILY_ID, ACADEMY_ID, "후문", true);
		List<PickupSpot> existing = List.of(
				existingSpot(10L, OTHER_FAMILY_ID, ACADEMY_ID, "남의 집 장소", true),
				existingSpot(11L, FAMILY_ID, OTHER_ACADEMY_ID, "다른 학원 장소", true));

		List<PickupSpot> changed = DefaultSpotRule.demotePreviousDefaults(newDefault, existing);

		assertThat(changed).isEmpty();
	}

	@Test
	@DisplayName("기본 장소 규칙은 기존 장소를 기본으로 바꿀 때 그 장소 자신은 해제 대상에 넣지 않는다")
	void doesNotDemoteTheNewDefaultItself() {
		PickupSpot newDefault = existingSpot(11L, FAMILY_ID, ACADEMY_ID, "후문", true);
		List<PickupSpot> existing = List.of(
				existingSpot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true),
				existingSpot(11L, FAMILY_ID, ACADEMY_ID, "후문", true));

		List<PickupSpot> changed = DefaultSpotRule.demotePreviousDefaults(newDefault, existing);

		assertThat(changed).extracting(PickupSpot::getSpotId).containsExactly(10L);
	}

	@Test
	@DisplayName("기본 장소 규칙은 새 장소가 기본이 아니면 이전 기본 장소를 바꾸지 않는다")
	void doesNotDemoteWhenNewSpotIsNotDefault() {
		PickupSpot candidate = newSpot(FAMILY_ID, ACADEMY_ID, "후문", false);
		List<PickupSpot> existing = List.of(existingSpot(10L, FAMILY_ID, ACADEMY_ID, "정문 앞", true));

		List<PickupSpot> changed = DefaultSpotRule.demotePreviousDefaults(candidate, existing);

		assertThat(changed).isEmpty();
	}

	private static PickupSpot newSpot(long familyId, long academyId, String name, boolean isDefault) {
		return PickupSpot.create(familyId, academyId, name, null, STOP, MEETING, isDefault, GUARDIAN_ID);
	}

	private static PickupSpot existingSpot(long spotId, long familyId, long academyId, String name, boolean isDefault) {
		return PickupSpot.restore(spotId, familyId, academyId, name, null, STOP, MEETING, isDefault, GUARDIAN_ID);
	}
}
