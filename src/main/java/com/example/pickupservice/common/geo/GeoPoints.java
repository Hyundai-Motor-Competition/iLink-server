package com.example.pickupservice.common.geo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeoPoints {

	private static final int WGS84_SRID = 4326;
	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

	public static Point toPoint(double lat, double lng) {
		return FACTORY.createPoint(new Coordinate(lng, lat));
	}

	public static LatLng toLatLng(Point point) {
		return new LatLng(point.getY(), point.getX());
	}

}
