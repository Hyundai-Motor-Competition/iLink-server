package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.ServiceAreaPort;
import com.example.pickupservice.place.domain.ServiceArea;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
class GangnamServiceAreaAdapter implements ServiceAreaPort {

	private static final String BOUNDARY_PATH = "reference/gangnam/boundary_gangnam.geojson";

	private final ServiceArea serviceArea;

	GangnamServiceAreaAdapter() {
		this.serviceArea = readServiceArea();
	}

	@Override
	public boolean isSupported(LatLng point) {
		return serviceArea.contains(point);
	}

	private static ServiceArea readServiceArea() {
		try (InputStream in = new ClassPathResource(BOUNDARY_PATH).getInputStream()) {
			JsonNode root = JsonMapper.builder().build().readTree(in);
			JsonNode ring = root.get("features").get(0).get("geometry").get("coordinates").get(0);
			List<LatLng> outerRing = new ArrayList<>(ring.size());
			for (int i = 0; i < ring.size(); i++) {
				JsonNode coordinate = ring.get(i);
				outerRing.add(new LatLng(coordinate.get(1).doubleValue(), coordinate.get(0).doubleValue()));
			}
			return new ServiceArea(outerRing);
		}
		catch (IOException e) {
			throw new IllegalStateException("강남구 경계 파일을 읽을 수 없습니다: " + BOUNDARY_PATH, e);
		}
	}
}
