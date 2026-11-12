package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.ServiceAreaPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** classpath의 reference/gangnam/boundary_gangnam.geojson을 읽는 어댑터이므로 DB 없이 직접 만든다. */
class GangnamServiceAreaAdapterTest {

	private final ServiceAreaPort serviceAreaPort = new GangnamServiceAreaAdapter();

	@Test
	@DisplayName("isSupported는 강남구 안의 덕스어학원 좌표에 true를 돌려준다")
	void isSupportedIsTrueForDuksAcademyInGangnam() {
		assertThat(serviceAreaPort.isSupported(new LatLng(37.4998344, 127.0674141))).isTrue();
	}

	@Test
	@DisplayName("isSupported는 강남구 안의 강남구청 좌표에 true를 돌려준다")
	void isSupportedIsTrueForGangnamOffice() {
		assertThat(serviceAreaPort.isSupported(new LatLng(37.5172, 127.0473))).isTrue();
	}

	@Test
	@DisplayName("isSupported는 강남구 밖의 서울시청 좌표에 false를 돌려준다")
	void isSupportedIsFalseForSeoulCityHall() {
		assertThat(serviceAreaPort.isSupported(new LatLng(37.5665, 126.9780))).isFalse();
	}

	@Test
	@DisplayName("isSupported는 강남구 밖의 교대역 좌표에 false를 돌려준다")
	void isSupportedIsFalseForGyodaeStation() {
		assertThat(serviceAreaPort.isSupported(new LatLng(37.4934, 127.0142))).isFalse();
	}
}
