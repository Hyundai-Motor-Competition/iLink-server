package com.example.pickupservice.place.domain;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceAreaTest {

	private static ServiceArea gangnam;

	@BeforeAll
	static void loadGangnamBoundary() throws IOException {
		JsonNode root;
		try (InputStream in = ServiceAreaTest.class.getClassLoader()
				.getResourceAsStream("reference/gangnam/boundary_gangnam.geojson")) {
			assertThat(in).as("boundary_gangnam.geojson").isNotNull();
			root = JsonMapper.builder().build().readTree(in);
		}
		// geojson 좌표 순서는 [경도, 위도]다.
		JsonNode ring = root.path("features").get(0).path("geometry").path("coordinates").get(0);
		List<LatLng> points = new ArrayList<>();
		for (JsonNode position : ring) {
			points.add(new LatLng(position.get(1).asDouble(), position.get(0).asDouble()));
		}
		gangnam = new ServiceArea(points);
	}

	private static ServiceArea square() {
		return new ServiceArea(List.of(
				new LatLng(0, 0), new LatLng(0, 10), new LatLng(10, 10), new LatLng(10, 0), new LatLng(0, 0)));
	}

	// 오른쪽이 열린 ㄷ자: lat 4~6, lng 3~10 부분이 비어 있다.
	private static ServiceArea cShape() {
		return new ServiceArea(List.of(
				new LatLng(0, 0), new LatLng(0, 10), new LatLng(4, 10), new LatLng(4, 3),
				new LatLng(6, 3), new LatLng(6, 10), new LatLng(10, 10), new LatLng(10, 0),
				new LatLng(0, 0)));
	}

	@Test
	@DisplayName("지원 지역 규칙은 사각형 안의 좌표에 true를 돌려준다")
	void squareInside() {
		assertThat(square().contains(new LatLng(5, 5))).isTrue();
	}

	@Test
	@DisplayName("지원 지역 규칙은 사각형 밖의 좌표에 false를 돌려준다")
	void squareOutside() {
		assertThat(square().contains(new LatLng(5, 11))).isFalse();
		assertThat(square().contains(new LatLng(-1, 5))).isFalse();
		assertThat(square().contains(new LatLng(11, 5))).isFalse();
		assertThat(square().contains(new LatLng(5, -1))).isFalse();
	}

	@Test
	@DisplayName("지원 지역 규칙은 ㄷ자 도형의 몸통 안의 좌표에 true를 돌려준다")
	void cShapeInsideBody() {
		assertThat(cShape().contains(new LatLng(5, 1))).isTrue();
		assertThat(cShape().contains(new LatLng(2, 8))).isTrue();
		assertThat(cShape().contains(new LatLng(8, 8))).isTrue();
	}

	@Test
	@DisplayName("지원 지역 규칙은 ㄷ자 도형의 오목하게 들어간 부분의 좌표에 false를 돌려준다")
	void cShapeInsideNotch() {
		assertThat(cShape().contains(new LatLng(5, 6))).isFalse();
		assertThat(cShape().contains(new LatLng(5, 9.5))).isFalse();
	}

	@Test
	@DisplayName("지원 지역 규칙은 ㄷ자 도형의 경계 상자 밖 좌표에 false를 돌려준다")
	void cShapeOutsideBox() {
		assertThat(cShape().contains(new LatLng(5, 12))).isFalse();
		assertThat(cShape().contains(new LatLng(-3, 5))).isFalse();
	}

	@Test
	@DisplayName("지원 지역 규칙은 강남구 경계 안의 대치동 덕스어학원 좌표에 true를 돌려준다")
	void gangnamContainsDaechiAcademy() {
		assertThat(gangnam.contains(new LatLng(37.4998344, 127.0674141))).isTrue();
	}

	@Test
	@DisplayName("지원 지역 규칙은 강남구 경계 안의 강남구청 좌표에 true를 돌려준다")
	void gangnamContainsGangnamOffice() {
		assertThat(gangnam.contains(new LatLng(37.5172, 127.0473))).isTrue();
	}

	@Test
	@DisplayName("지원 지역 규칙은 강남구 밖의 서울시청 좌표에 false를 돌려준다")
	void gangnamExcludesSeoulCityHall() {
		assertThat(gangnam.contains(new LatLng(37.5665, 126.9780))).isFalse();
	}

	@Test
	@DisplayName("지원 지역 규칙은 강남구 경계 상자 안이지만 도형 밖인 교대역(서초구) 좌표에 false를 돌려준다")
	void gangnamExcludesGyodaeStation() {
		assertThat(gangnam.contains(new LatLng(37.4934, 127.0142))).isFalse();
	}

	@Test
	@DisplayName("지원 지역 규칙은 강남구 밖의 송파구청 좌표에 false를 돌려준다")
	void gangnamExcludesSongpaOffice() {
		assertThat(gangnam.contains(new LatLng(37.5145, 127.1059))).isFalse();
	}
}
