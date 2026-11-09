package com.example.pickupservice.schedule.domain;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyTest {

	private static final String EXTERNAL_PLACE_ID = "kakao:12345";
	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);
	private static final LatLng NEW_LOCATION = new LatLng(37.497942, 127.027621);
	private static final LatLng NEW_ENTRANCE = new LatLng(37.497900, 127.027600);

	@Test
	@DisplayName("Academy.create는 academyId 없이 externalPlaceId와 받은 이름 · 주소 · 위치 · 출입구 · 전화를 가진 학원을 만든다")
	void createKeepsExternalPlaceIdAndGivenInfoWithoutAcademyId() {
		Academy academy = Academy.create(EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, ENTRANCE,
				"02-555-1234");

		assertThat(academy.getAcademyId()).as("새로 만든 학원의 academyId").isNull();
		assertThat(academy.getExternalPlaceId()).isEqualTo(EXTERNAL_PLACE_ID);
		assertThat(academy.getName()).isEqualTo("해솔영어학원");
		assertThat(academy.getAddress()).isEqualTo("서울 강남구 테헤란로 152");
		assertThat(academy.getLocation()).isEqualTo(LOCATION);
		assertThat(academy.getEntrance()).isEqualTo(ENTRANCE);
		assertThat(academy.getPhone()).isEqualTo("02-555-1234");
		assertThat(academy.hasEntrance()).isTrue();
	}

	@Test
	@DisplayName("Academy.restore는 academyId와 externalPlaceId를 그대로 가진 학원을 되살린다")
	void restoreKeepsAcademyIdAndExternalPlaceId() {
		Academy academy = Academy.restore(301L, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, null,
				null);

		assertThat(academy.getAcademyId()).isEqualTo(301L);
		assertThat(academy.getExternalPlaceId()).isEqualTo(EXTERNAL_PLACE_ID);
		assertThat(academy.getName()).isEqualTo("해솔영어학원");
		assertThat(academy.getAddress()).isEqualTo("서울 강남구 테헤란로 152");
		assertThat(academy.getLocation()).isEqualTo(LOCATION);
		assertThat(academy.getEntrance()).isNull();
		assertThat(academy.getPhone()).isNull();
		assertThat(academy.hasEntrance()).isFalse();
	}

	@Test
	@DisplayName("updateInfo는 academyId · externalPlaceId는 그대로 두고 이름 · 주소 · 위치 · 출입구 · 전화를 받은 값으로 바꾼 학원을 돌려주며 원래 학원은 바뀌지 않는다")
	void updateInfoReturnsAcademyWithSameIdsAndNewInfo() {
		Academy original = Academy.restore(301L, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION,
				null, null);

		Academy updated = original.updateInfo("해솔영어학원 본관", "서울 강남구 테헤란로 200", NEW_LOCATION, NEW_ENTRANCE,
				"02-555-9999");

		assertThat(updated.getAcademyId()).as("갱신한 학원의 academyId").isEqualTo(301L);
		assertThat(updated.getExternalPlaceId()).as("갱신한 학원의 externalPlaceId").isEqualTo(EXTERNAL_PLACE_ID);
		assertThat(updated.getName()).isEqualTo("해솔영어학원 본관");
		assertThat(updated.getAddress()).isEqualTo("서울 강남구 테헤란로 200");
		assertThat(updated.getLocation()).isEqualTo(NEW_LOCATION);
		assertThat(updated.getEntrance()).isEqualTo(NEW_ENTRANCE);
		assertThat(updated.getPhone()).isEqualTo("02-555-9999");
		assertThat(updated.hasEntrance()).isTrue();

		assertThat(original.getName()).as("원래 학원의 name").isEqualTo("해솔영어학원");
		assertThat(original.getAddress()).as("원래 학원의 address").isEqualTo("서울 강남구 테헤란로 152");
		assertThat(original.getLocation()).as("원래 학원의 location").isEqualTo(LOCATION);
		assertThat(original.getEntrance()).as("원래 학원의 entrance").isNull();
		assertThat(original.getPhone()).as("원래 학원의 phone").isNull();
	}

	@Test
	@DisplayName("updateInfo는 받은 entrance · phone이 비어 있으면(null) 기존 entrance · phone을 유지하고 이름 · 주소 · 위치는 받은 값으로 바꾼다")
	void updateInfoKeepsExistingEntranceAndPhoneWhenGivenValuesAreNull() {
		Academy original = Academy.restore(301L, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION,
				ENTRANCE, "02-555-1234");

		Academy updated = original.updateInfo("해솔영어학원 본관", "서울 강남구 테헤란로 200", NEW_LOCATION, null, null);

		assertThat(updated.getAcademyId()).as("갱신한 학원의 academyId").isEqualTo(301L);
		assertThat(updated.getExternalPlaceId()).as("갱신한 학원의 externalPlaceId").isEqualTo(EXTERNAL_PLACE_ID);
		assertThat(updated.getName()).as("받은 값으로 바꾼 name").isEqualTo("해솔영어학원 본관");
		assertThat(updated.getAddress()).as("받은 값으로 바꾼 address").isEqualTo("서울 강남구 테헤란로 200");
		assertThat(updated.getLocation()).as("받은 값으로 바꾼 location").isEqualTo(NEW_LOCATION);
		assertThat(updated.getEntrance()).as("받은 entrance가 null이면 유지한 기존 entrance").isEqualTo(ENTRANCE);
		assertThat(updated.getPhone()).as("받은 phone이 null이면 유지한 기존 phone").isEqualTo("02-555-1234");
		assertThat(updated.hasEntrance()).as("기존 출입구를 유지했으니 hasEntrance").isTrue();
	}

	@Test
	@DisplayName("updateInfo는 기존 entrance · phone이 있어도 받은 entrance · phone이 있으면 받은 값으로 바꾼다")
	void updateInfoReplacesExistingEntranceAndPhoneWhenGivenValuesArePresent() {
		Academy original = Academy.restore(301L, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION,
				ENTRANCE, "02-555-1234");

		Academy updated = original.updateInfo("해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, NEW_ENTRANCE,
				"02-555-9999");

		assertThat(updated.getEntrance()).as("받은 값으로 바꾼 entrance").isEqualTo(NEW_ENTRANCE);
		assertThat(updated.getPhone()).as("받은 값으로 바꾼 phone").isEqualTo("02-555-9999");
	}

	@Test
	@DisplayName("updateInfo는 받은 entrance만 비어 있으면 entrance는 유지하고 phone은 받은 값으로 바꾼다 (두 값은 따로 판단한다)")
	void updateInfoJudgesEntranceAndPhoneSeparately() {
		Academy original = Academy.restore(301L, EXTERNAL_PLACE_ID, "해솔영어학원", "서울 강남구 테헤란로 152", LOCATION,
				ENTRANCE, "02-555-1234");

		Academy updated = original.updateInfo("해솔영어학원", "서울 강남구 테헤란로 152", LOCATION, null, "02-555-9999");

		assertThat(updated.getEntrance()).as("받은 entrance가 null이면 유지한 기존 entrance").isEqualTo(ENTRANCE);
		assertThat(updated.getPhone()).as("받은 값으로 바꾼 phone").isEqualTo("02-555-9999");
	}

}
