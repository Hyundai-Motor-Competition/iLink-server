package com.example.pickupservice.place.adapter.out.map;

import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DriveTimePort;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class KakaoMobilityDriveTimeAdapter implements DriveTimePort {

	private static final String DIRECTIONS_PATH = "/v1/directions";
	private static final String FUTURE_DIRECTIONS_PATH = "/v1/future/directions";
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DEPARTURE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

	private final RestClient restClient;
	private final String apiKey;

	public KakaoMobilityDriveTimeAdapter(RestClient.Builder restClientBuilder,
			@Value("${pickup.map.kakao-mobility.base-url}") String baseUrl,
			@Value("${pickup.map.kakao.api-key:}") String apiKey) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.apiKey = apiKey;
	}

	@Override
	public List<Integer> driveSecondsNow(LatLng origin, List<LatLng> destinations) {
		return collect(origin, destinations, null);
	}

	@Override
	public List<Integer> driveSecondsAt(LatLng origin, List<LatLng> destinations, Instant departAt) {
		return collect(origin, destinations, DEPARTURE_TIME_FORMAT.format(departAt.atZone(SEOUL)));
	}

	private List<Integer> collect(LatLng origin, List<LatLng> destinations, String departureTime) {
		List<Integer> result = new ArrayList<>(destinations.size());
		for (LatLng destination : destinations) {
			Optional<Integer> duration = requestDuration(origin, destination, departureTime);
			if (duration.isEmpty()) {
				return List.of();
			}
			result.add(duration.get());
		}
		return result;
	}

	private Optional<Integer> requestDuration(LatLng origin, LatLng destination, String departureTime) {
		try {
			DirectionsResponse response = restClient.get()
					.uri(uriBuilder -> {
						uriBuilder.path(departureTime == null ? DIRECTIONS_PATH : FUTURE_DIRECTIONS_PATH)
								.queryParam("origin", "{origin}")
								.queryParam("destination", "{destination}");
						if (departureTime == null) {
							return uriBuilder.queryParam("summary", "true")
									.build(lngLat(origin), lngLat(destination));
						}
						return uriBuilder.queryParam("departure_time", "{departureTime}")
								.build(lngLat(origin), lngLat(destination), departureTime);
					})
					.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
					.retrieve()
					.body(DirectionsResponse.class);
			Optional<Integer> duration = Optional.ofNullable(response).flatMap(DirectionsResponse::duration);
			if (duration.isEmpty()) {
				log.warn("kakao mobility api failed reason=no usable duration");
			}
			return duration;
		} catch (RestClientResponseException e) {
			log.warn("kakao mobility api failed status={}", e.getStatusCode().value());
			return Optional.empty();
		} catch (RestClientException e) {
			log.warn("kakao mobility api failed exception={}", e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private static String lngLat(LatLng point) {
		return point.lng() + "," + point.lat();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DirectionsResponse(List<Route> routes) {

		private Optional<Integer> duration() {
			if (routes == null || routes.isEmpty() || routes.get(0) == null) {
				return Optional.empty();
			}
			Route route = routes.get(0);
			if (route.resultCode() == null || route.resultCode() != 0 || route.summary() == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(route.summary().duration());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Route(@JsonProperty("result_code") Integer resultCode, Summary summary) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Summary(Integer duration) {
	}
}
