package com.example.pickupservice.schedule.adapter.out.map;

import com.example.pickupservice.common.error.BusinessException;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.in.AcademyCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class KakaoMapSearchAdapterTest {

	private static final String BASE_URL = "https://dapi.kakao.test";
	private static final String API_KEY = "test-kakao-key";
	private static final String KEYWORD_PATH = BASE_URL + "/v2/local/search/keyword.json";

	private MockRestServiceServer server;
	private KakaoMapSearchAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new KakaoMapSearchAdapter(builder, BASE_URL, API_KEY);
	}

	@Test
	@DisplayName("카카오 키워드 검색 응답을 kakao:{id} · 이름 · 도로명 주소 · 위치(y=lat, x=lng)의 AcademyCandidate로 바꾼다")
	void search_mapsKakaoDocumentsToAcademyCandidates() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withSuccess("""
						{
						  "documents": [
						    {
						      "id": "26338954",
						      "place_name": "한빛수학학원",
						      "address_name": "서울 강남구 대치동 900",
						      "road_address_name": "서울 강남구 삼성로 212",
						      "category_group_code": "AC5",
						      "x": "127.0612",
						      "y": "37.4991"
						    },
						    {
						      "id": "1180475",
						      "place_name": "대치영어학원",
						      "address_name": "서울 강남구 대치동 316",
						      "road_address_name": "",
						      "category_group_code": "AC5",
						      "x": "127.0583",
						      "y": "37.4945"
						    }
						  ],
						  "meta": { "total_count": 2, "pageable_count": 2, "is_end": true }
						}
						""", MediaType.APPLICATION_JSON));

		List<AcademyCandidate> candidates = adapter.search("학원");

		assertThat(candidates).containsExactly(
				new AcademyCandidate("kakao:26338954", "한빛수학학원", "서울 강남구 삼성로 212",
						new LatLng(37.4991, 127.0612)),
				new AcademyCandidate("kakao:1180475", "대치영어학원", "서울 강남구 대치동 316",
						new LatLng(37.4945, 127.0583)));
		server.verify();
	}

	@Test
	@DisplayName("카카오 키워드 검색을 GET · KakaoAK 키 헤더 · query 파라미터로 부른다")
	void search_callsKakaoKeywordSearchWithKakaoAkHeaderAndQuery() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY))
				.andExpect(decodedQueryParam("query", "수학학원"))
				.andRespond(withSuccess(EMPTY_RESPONSE, MediaType.APPLICATION_JSON));

		adapter.search("수학학원");

		server.verify();
	}

	@Test
	@DisplayName("카카오 검색 결과가 없으면 빈 목록을 돌려준다")
	void search_returnsEmptyListWhenNoDocuments() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withSuccess(EMPTY_RESPONSE, MediaType.APPLICATION_JSON));

		List<AcademyCandidate> candidates = adapter.search("없는학원이름");

		assertThat(candidates).isEmpty();
		server.verify();
	}

	@ParameterizedTest(name = "카카오 응답 {0}")
	@EnumSource(value = HttpStatus.class,
			names = {"INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE", "UNAUTHORIZED", "BAD_REQUEST"})
	@DisplayName("카카오 API가 4xx · 5xx로 답하면 502 MAP_API_UNAVAILABLE로 거절한다")
	void search_rejectsWithMapApiUnavailableWhenKakaoRespondsWithError(HttpStatus kakaoStatus) {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withStatus(kakaoStatus));

		assertMapApiUnavailable();
	}

	@Test
	@DisplayName("카카오 API 응답 읽기 시간이 초과되면 502 MAP_API_UNAVAILABLE로 거절한다")
	void search_rejectsWithMapApiUnavailableWhenReadTimesOut() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withException(new SocketTimeoutException("Read timed out")));

		assertMapApiUnavailable();
	}

	@Test
	@DisplayName("카카오 API에 연결하지 못하면 502 MAP_API_UNAVAILABLE로 거절한다")
	void search_rejectsWithMapApiUnavailableWhenConnectionFails() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withException(new ConnectException("Connection refused")));

		assertMapApiUnavailable();
	}

	@Test
	@DisplayName("카카오 API가 200이라도 본문이 JSON이 아니면 502 MAP_API_UNAVAILABLE로 거절한다")
	void search_rejectsWithMapApiUnavailableWhenBodyIsNotJson() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withSuccess("<html>점검 중입니다</html>", MediaType.APPLICATION_JSON));

		assertMapApiUnavailable();
	}

	@Test
	@DisplayName("카카오 API가 200이라도 documents 항목에 x · y 좌표가 없으면 502 MAP_API_UNAVAILABLE로 거절한다")
	void search_rejectsWithMapApiUnavailableWhenDocumentHasNoCoordinates() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withSuccess("""
						{
						  "documents": [
						    {
						      "id": "26338954",
						      "place_name": "한빛수학학원",
						      "address_name": "서울 강남구 대치동 900",
						      "road_address_name": "서울 강남구 삼성로 212"
						    }
						  ],
						  "meta": { "total_count": 1, "pageable_count": 1, "is_end": true }
						}
						""", MediaType.APPLICATION_JSON));

		assertMapApiUnavailable();
	}

	@Test
	@DisplayName("카카오 API가 200이라도 좌표 x가 숫자가 아니면 502 MAP_API_UNAVAILABLE로 거절한다")
	void search_rejectsWithMapApiUnavailableWhenCoordinateIsNotNumber() {
		server.expect(requestTo(startsWith(KEYWORD_PATH)))
				.andRespond(withSuccess("""
						{
						  "documents": [
						    {
						      "id": "26338954",
						      "place_name": "한빛수학학원",
						      "address_name": "서울 강남구 대치동 900",
						      "road_address_name": "서울 강남구 삼성로 212",
						      "x": "abc",
						      "y": "37.4991"
						    }
						  ],
						  "meta": { "total_count": 1, "pageable_count": 1, "is_end": true }
						}
						""", MediaType.APPLICATION_JSON));

		assertMapApiUnavailable();
	}

	private void assertMapApiUnavailable() {
		assertThatThrownBy(() -> adapter.search("학원"))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(e.code()).isEqualTo("MAP_API_UNAVAILABLE");
				});
	}

	private static RequestMatcher decodedQueryParam(String name, String expected) {
		return request -> {
			String raw = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(name);
			assertThat(raw).as("query parameter " + name).isNotNull();
			assertThat(URLDecoder.decode(raw, StandardCharsets.UTF_8)).isEqualTo(expected);
		};
	}

	private static final String EMPTY_RESPONSE = """
			{ "documents": [], "meta": { "total_count": 0, "pageable_count": 0, "is_end": true } }
			""";
}
