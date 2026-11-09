package com.example.pickupservice.schedule.application.service;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import com.example.pickupservice.schedule.application.port.out.SaveAcademyPort;
import com.example.pickupservice.schedule.application.port.out.SearchAcademyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchAcademyServiceTest {

	private static final String QUERY = "해법수학";

	@Mock
	private SearchAcademyPort searchAcademyPort;

	@Mock
	private SaveAcademyPort saveAcademyPort;

	private SearchAcademyService searchAcademyService;

	@BeforeEach
	void setUp() {
		searchAcademyService = new SearchAcademyService(searchAcademyPort);
	}

	@Test
	@DisplayName("학원 검색은 외부 지도 API 결과를 저장하지 않고 externalPlaceId · name · address · location으로 돌려준다")
	void searchAcademiesRelaysMapResultsWithoutSaving() {
		List<AcademyCandidate> candidates = List.of(
				new AcademyCandidate("kakao:12345", "해법수학 대치점", "서울 강남구 대치동 1",
						new LatLng(37.4995, 127.0631)),
				new AcademyCandidate("kakao:67890", "해법수학 역삼점", "서울 강남구 역삼동 2",
						new LatLng(37.5006, 127.0364)));
		given(searchAcademyPort.search(QUERY)).willReturn(candidates);

		List<AcademyCandidate> result = searchAcademyService.searchAcademies(QUERY);

		assertThat(result).containsExactly(
				new AcademyCandidate("kakao:12345", "해법수학 대치점", "서울 강남구 대치동 1",
						new LatLng(37.4995, 127.0631)),
				new AcademyCandidate("kakao:67890", "해법수학 역삼점", "서울 강남구 역삼동 2",
						new LatLng(37.5006, 127.0364)));
		verifyNoInteractions(saveAcademyPort);
	}

	@Test
	@DisplayName("학원 검색은 외부 지도 API 결과가 없으면 빈 목록을 돌려준다")
	void searchAcademiesReturnsEmptyListWhenMapHasNoResult() {
		given(searchAcademyPort.search(QUERY)).willReturn(List.of());

		List<AcademyCandidate> result = searchAcademyService.searchAcademies(QUERY);

		assertThat(result).isEmpty();
		verifyNoInteractions(saveAcademyPort);
	}

	@Test
	@DisplayName("학원 검색은 외부 지도 API가 예외로 실패하면 502 MAP_API_UNAVAILABLE로 거절한다")
	void searchAcademiesThrowsMapApiUnavailableWhenPortFails() {
		given(searchAcademyPort.search(QUERY)).willThrow(new IllegalStateException("kakao timeout"));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> searchAcademyService.searchAcademies(QUERY));

		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo("MAP_API_UNAVAILABLE");
		assertThat(thrown.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
		verifyNoInteractions(saveAcademyPort);
	}

	@Test
	@DisplayName("학원 검색은 지도 API 포트가 MAP_API_UNAVAILABLE로 거절하면 그대로 502 MAP_API_UNAVAILABLE로 거절한다")
	void searchAcademiesPropagatesMapApiUnavailableFromPort() {
		given(searchAcademyPort.search(QUERY)).willThrow(
				BusinessException.of(HttpStatus.BAD_GATEWAY, "MAP_API_UNAVAILABLE", "지도 검색을 잠시 사용할 수 없습니다"));

		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> searchAcademyService.searchAcademies(QUERY));

		assertThat(thrown).isNotNull();
		assertThat(thrown.code()).isEqualTo("MAP_API_UNAVAILABLE");
		assertThat(thrown.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
	}
}
