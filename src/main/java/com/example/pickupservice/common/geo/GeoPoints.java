package com.example.pickupservice.common.geo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeoPoints {

	private static final double EARTH_RADIUS_METERS = 6_371_000;
	private static final int WGS84_SRID = 4326;
	private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

	public static Point toPoint(double lat, double lng) {
		return FACTORY.createPoint(new Coordinate(lng, lat));
	}

	public static LatLng toLatLng(Point point) {
		return new LatLng(point.getY(), point.getX());
	}

	public static double distanceMeters(LatLng from, LatLng to) {
		double lat1 = Math.toRadians(from.lat());
		double lat2 = Math.toRadians(to.lat());
		double dLat = lat2 - lat1;
		double dLng = Math.toRadians(to.lng() - from.lng());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
	}

}
