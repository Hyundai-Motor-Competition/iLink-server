package com.example.pickupservice.schedule.adapter.out.map;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import com.example.pickupservice.schedule.application.port.out.SearchAcademyPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class KakaoMapSearchAdapter implements SearchAcademyPort {

	private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
	private static final String MAP_API_UNAVAILABLE = "MAP_API_UNAVAILABLE";
	private static final String MAP_API_UNAVAILABLE_MESSAGE = "지도 API를 사용할 수 없습니다.";

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
		KeywordSearchResponse response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> uriBuilder.path(KEYWORD_SEARCH_PATH).queryParam("query", "{query}").build(query))
					.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
					.retrieve()
					.body(KeywordSearchResponse.class);
		} catch (RestClientResponseException | ResourceAccessException e) {
			throw BusinessException.of(HttpStatus.BAD_GATEWAY, MAP_API_UNAVAILABLE, MAP_API_UNAVAILABLE_MESSAGE);
		}
		if (response == null || response.documents() == null) {
			return List.of();
		}
		return response.documents().stream().map(Document::toCandidate).toList();
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
					new LatLng(Double.parseDouble(y), Double.parseDouble(x)));
		}
	}
}
