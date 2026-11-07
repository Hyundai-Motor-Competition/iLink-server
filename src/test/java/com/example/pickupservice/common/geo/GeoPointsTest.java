package com.example.pickupservice.common.geo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import static org.assertj.core.api.Assertions.assertThat;

class GeoPointsTest {

	private static final double LAT = 37.501234;
	private static final double LNG = 127.039876;

	@Test
	@DisplayName("GeoPoints.toPoint는 lat 37.501234 · lng 127.039876을 SRID 4326 Point로 바꾼다")
	void toPointCreatesPointWithSrid4326() {
		Point point = GeoPoints.toPoint(LAT, LNG);

		assertThat(point).isNotNull();
		assertThat(point.isEmpty()).isFalse();
		assertThat(point.getSRID()).isEqualTo(4326);
	}

	@Test
	@DisplayName("GeoPoints.toPoint는 경도 127.039876을 x에, 위도 37.501234를 y에 넣는다")
	void toPointPutsLongitudeInXAndLatitudeInY() {
		Point point = GeoPoints.toPoint(LAT, LNG);

		assertThat(point.getX()).isEqualTo(LNG);
		assertThat(point.getY()).isEqualTo(LAT);
	}

	@Test
	@DisplayName("GeoPoints.toLatLng는 x=127.039876 · y=37.501234인 SRID 4326 Point를 lat 37.501234 · lng 127.039876으로 돌려준다")
	void toLatLngReadsLatitudeFromYAndLongitudeFromX() {
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
		Point point = factory.createPoint(new Coordinate(LNG, LAT));

		LatLng latLng = GeoPoints.toLatLng(point);

		assertThat(latLng.lat()).isEqualTo(LAT);
		assertThat(latLng.lng()).isEqualTo(LNG);
	}

	@Test
	@DisplayName("GeoPoints.toPoint로 바꾼 Point를 toLatLng로 다시 바꾸면 lat 37.501234 · lng 127.039876이 그대로 나온다")
	void toLatLngReturnsSameValuesGivenToToPoint() {
		Point point = GeoPoints.toPoint(LAT, LNG);

		LatLng latLng = GeoPoints.toLatLng(point);

		assertThat(latLng.lat()).isEqualTo(LAT);
		assertThat(latLng.lng()).isEqualTo(LNG);
	}

}
