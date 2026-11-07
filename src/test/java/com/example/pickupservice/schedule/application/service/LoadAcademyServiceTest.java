package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyInfo;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.domain.Academy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoadAcademyServiceTest {

	private static final long ACADEMY_ID = 301L;
	private static final long MISSING_ACADEMY_ID = 999L;

	private static final LatLng LOCATION = new LatLng(37.501234, 127.039876);
	private static final LatLng ENTRANCE = new LatLng(37.501150, 127.039790);

	@Mock
	private LoadAcademyPort loadAcademyPort;

	@InjectMocks
	private LoadAcademyService service;

	// #61

	@Test
	@DisplayName("findAcademy는 학원 id의 이름 · 주소 · 위치 · 출입구 · 대표번호를 돌려준다")
	void findAcademyReturnsAcademyInfo() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.of(Academy.restore(ACADEMY_ID, "kakao:12345",
				"해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, ENTRANCE, "02-555-1234")));

		Optional<AcademyInfo> academy = service.findAcademy(ACADEMY_ID);

		assertThat(academy).contains(
				new AcademyInfo(ACADEMY_ID, "해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, ENTRANCE, "02-555-1234"));
	}

	@Test
	@DisplayName("findAcademy는 출입구가 없는 학원이면 entrance를 null로 돌려준다")
	void findAcademyReturnsNullEntranceWhenAcademyHasNoEntrance() {
		given(loadAcademyPort.loadAcademy(ACADEMY_ID)).willReturn(Optional.of(Academy.restore(ACADEMY_ID, "kakao:12345",
				"해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, null, "02-555-1234")));

		Optional<AcademyInfo> academy = service.findAcademy(ACADEMY_ID);

		assertThat(academy).contains(
				new AcademyInfo(ACADEMY_ID, "해솔영어학원", "서울 강남구 테헤란로 1", LOCATION, null, "02-555-1234"));
	}

	@Test
	@DisplayName("findAcademy는 학원이 없으면 빈 값을 돌려준다")
	void findAcademyReturnsEmptyWhenAcademyIsMissing() {
		given(loadAcademyPort.loadAcademy(MISSING_ACADEMY_ID)).willReturn(Optional.empty());

		Optional<AcademyInfo> academy = service.findAcademy(MISSING_ACADEMY_ID);

		assertThat(academy).isEmpty();
	}
}
