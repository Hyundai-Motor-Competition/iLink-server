package com.example.pickupservice.schedule.adapter.out.map;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import com.example.pickupservice.schedule.application.port.out.SearchAcademyPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
public class KakaoMapSearchAdapter implements SearchAcademyPort {

	private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
	private static final String MAP_API_UNAVAILABLE = "MAP_API_UNAVAILABLE";
	private static final String MAP_API_UNAVAILABLE_MESSAGE = "지도 검색을 잠시 사용할 수 없습니다";

	private final RestClient restClient;
	private final String apiKey;

	public KakaoMapSearchAdapter(RestClient.Builder restClientBuilder,
			@Value("${pickup.map.kakao.base-url}") String baseUrl,
			@Value("${pickup.map.kakao.api-key:}") String apiKey) {
		this.restClient = restClientBuilder.baseUrl(baseUrl).build();
		this.apiKey = apiKey;
	}

	@Override
	public List<AcademyCandidate> search(String query) {
		KeywordSearchResponse response = requestKeywordSearch(query);
		if (response == null || response.documents() == null) {
			return List.of();
		}
		return response.documents().stream().map(Document::toCandidate).toList();
	}

	private KeywordSearchResponse requestKeywordSearch(String query) {
		try {
			return restClient.get()
					.uri(uriBuilder -> uriBuilder.path(KEYWORD_SEARCH_PATH).queryParam("query", "{query}").build(query))
					.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
					.retrieve()
					.body(KeywordSearchResponse.class);
		} catch (RestClientResponseException e) {
			log.warn("kakao map api failed status={}", e.getStatusCode().value());
			throw mapApiUnavailable();
		} catch (RestClientException e) {
			log.warn("kakao map api failed exception={}", e.getClass().getSimpleName());
			throw mapApiUnavailable();
		}
	}

	private static BusinessException mapApiUnavailable() {
		return BusinessException.of(HttpStatus.BAD_GATEWAY, MAP_API_UNAVAILABLE, MAP_API_UNAVAILABLE_MESSAGE);
	}

	private record KeywordSearchResponse(List<Document> documents) {
	}

	private record Document(
			String id,
			@JsonProperty("place_name") String placeName,
			@JsonProperty("address_name") String addressName,
			@JsonProperty("road_address_name") String roadAddressName,
			String x,
			String y
	) {

		private AcademyCandidate toCandidate() {
			String address = (roadAddressName == null || roadAddressName.isBlank()) ? addressName : roadAddressName;
			return new AcademyCandidate("kakao:" + id, placeName, address,
					new LatLng(parseCoordinate(y), parseCoordinate(x)));
		}

		private static double parseCoordinate(String value) {
			if (value == null) {
				log.warn("kakao map api failed reason=missing coordinate");
				throw mapApiUnavailable();
			}
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				log.warn("kakao map api failed reason=coordinate not number");
				throw mapApiUnavailable();
			}
		}
	}
}
