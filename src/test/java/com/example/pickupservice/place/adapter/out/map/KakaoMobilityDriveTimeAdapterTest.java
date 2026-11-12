package com.example.pickupservice.place.adapter.out.map;

import com.example.pickupservice.common.geo.LatLng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoMobilityDriveTimeAdapterTest {

	private static final String BASE_URL = "https://apis-navi.kakaomobility.test";
	private static final String API_KEY = "test-kakao-key";
	private static final String DIRECTIONS_PATH = "/v1/directions";
	private static final String FUTURE_DIRECTIONS_PATH = "/v1/future/directions";

	private static final LatLng ORIGIN = new LatLng(37.5000, 127.0300);
	private static final LatLng DEST_A = new LatLng(37.4991, 127.0612);
	private static final LatLng DEST_B = new LatLng(37.4945, 127.0583);
	private static final LatLng DEST_C = new LatLng(37.4902, 127.0555);

	private MockRestServiceServer server;
	private KakaoMobilityDriveTimeAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
		adapter = new KakaoMobilityDriveTimeAdapter(builder, BASE_URL, API_KEY);
	}

	@Test
	@DisplayName("자동차 길찾기를 목적지마다 불러 routes[0].summary.duration을 목적지 순서대로 돌려준다")
	void driveSecondsNow_callsDirectionsPerDestinationAndReturnsDurationsInOrder() {
		expectNow(DEST_A).andRespond(success(538));
		expectNow(DEST_B).andRespond(success(721));
		expectNow(DEST_C).andRespond(success(95));

		List<Integer> result = adapter.driveSecondsNow(ORIGIN, List.of(DEST_A, DEST_B, DEST_C));

		assertThat(result).containsExactly(538, 721, 95);
		server.verify();
	}

	@Test
	@DisplayName("목적지가 1곳이어도 자동차 길찾기를 1회 부르고 duration 1개를 돌려준다")
	void driveSecondsNow_callsDirectionsOnceForSingleDestination() {
		expectNow(DEST_A).andRespond(success(538));

		List<Integer> result = adapter.driveSecondsNow(ORIGIN, List.of(DEST_A));

		assertThat(result).containsExactly(538);
		server.verify();
	}

	@Test
	@DisplayName("자동차 길찾기는 GET · origin과 destination을 경도,위도 순서로 · summary=true · Authorization KakaoAK 키 헤더로 부른다")
	void driveSecondsNow_sendsLngLatOrderSummaryAndKakaoAkHeader() {
		server.expect(pathIs(DIRECTIONS_PATH))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY))
				.andExpect(queryParam("origin", "127.03,37.5"))
				.andExpect(queryParam("destination", "127.0612,37.4991"))
				.andExpect(queryParam("summary", "true"))
				.andRespond(success(538));

		List<Integer> result = adapter.driveSecondsNow(ORIGIN, List.of(DEST_A));

		assertThat(result).containsExactly(538);
		server.verify();
	}

	@Test
	@DisplayName("departAt이 있으면 미래 운행 정보 길찾기를 목적지마다 불러 duration을 목적지 순서대로 돌려준다")
	void driveSecondsAt_callsFutureDirectionsPerDestinationAndReturnsDurationsInOrder() {
		Instant departAt = Instant.parse("2026-11-13T13:00:00Z");
		expectAt(DEST_A, "202611132200").andRespond(success(610));
		expectAt(DEST_B, "202611132200").andRespond(success(430));

		List<Integer> result = adapter.driveSecondsAt(ORIGIN, List.of(DEST_A, DEST_B), departAt);

		assertThat(result).containsExactly(610, 430);
		server.verify();
	}

	@Test
	@DisplayName("departure_time은 departAt을 Asia/Seoul의 yyyyMMddHHmm으로 바꾼 값이다 (2026-11-13T13:00:00Z -> 202611132200)")
	void driveSecondsAt_formatsDepartureTimeInSeoulYyyyMMddHHmm() {
		server.expect(pathIs(FUTURE_DIRECTIONS_PATH))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK " + API_KEY))
				.andExpect(queryParam("origin", "127.03,37.5"))
				.andExpect(queryParam("destination", "127.0612,37.4991"))
				.andExpect(queryParam("departure_time", "202611132200"))
				.andRespond(success(610));

		List<Integer> result = adapter.driveSecondsAt(ORIGIN, List.of(DEST_A), Instant.parse("2026-11-13T13:00:00Z"));

		assertThat(result).containsExactly(610);
		server.verify();
	}

	@Test
	@DisplayName("서울 날짜가 UTC와 다르면 날짜도 서울 기준으로 바꾼다 (2026-11-13T16:30:00Z -> 202611140130)")
	void driveSecondsAt_usesSeoulDateWhenDayDiffersFromUtc() {
		expectAt(DEST_A, "202611140130").andRespond(success(610));

		List<Integer> result = adapter.driveSecondsAt(ORIGIN, List.of(DEST_A), Instant.parse("2026-11-13T16:30:00Z"));

		assertThat(result).containsExactly(610);
		server.verify();
	}

	@ParameterizedTest(name = "HTTP {0}이면 빈 목록")
	@ValueSource(ints = {400, 401, 500, 503})
	@DisplayName("자동차 길찾기가 HTTP 오류로 답하면 예외 없이 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenHttpError(int status) {
		expectNow(DEST_A).andRespond(withStatus(HttpStatus.valueOf(status)));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A))).isEmpty();
	}

	@ParameterizedTest(name = "HTTP {0}이면 빈 목록")
	@ValueSource(ints = {400, 401, 500, 503})
	@DisplayName("미래 운행 정보 길찾기가 HTTP 오류로 답하면 예외 없이 빈 목록을 돌려준다")
	void driveSecondsAt_returnsEmptyListWhenHttpError(int status) {
		expectAt(DEST_A, "202611132200").andRespond(withStatus(HttpStatus.valueOf(status)));

		assertThat(adapter.driveSecondsAt(ORIGIN, List.of(DEST_A), Instant.parse("2026-11-13T13:00:00Z"))).isEmpty();
	}

	@Test
	@DisplayName("연결 예외가 나면 예외 없이 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenConnectionFails() {
		expectNow(DEST_A).andRespond(withException(new ConnectException("Connection refused")));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A))).isEmpty();
	}

	@Test
	@DisplayName("읽기 시간이 초과되면 예외 없이 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenReadTimesOut() {
		expectNow(DEST_A).andRespond(withException(new SocketTimeoutException("Read timed out")));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A))).isEmpty();
	}

	@Test
	@DisplayName("미래 운행 정보 길찾기에서 연결 예외가 나도 예외 없이 빈 목록을 돌려준다")
	void driveSecondsAt_returnsEmptyListWhenConnectionFails() {
		expectAt(DEST_A, "202611132200").andRespond(withException(new ConnectException("Connection refused")));

		assertThat(adapter.driveSecondsAt(ORIGIN, List.of(DEST_A), Instant.parse("2026-11-13T13:00:00Z"))).isEmpty();
	}

	@Test
	@DisplayName("result_code가 0이 아닌 응답(304)이 오면 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenResultCodeIsNotZero() {
		expectNow(DEST_A).andRespond(json("""
				{"routes":[{"result_code":304,"result_msg":"경유지/목적지가 출발지 반경 10km를 넘습니다"}]}
				"""));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A))).isEmpty();
	}

	@Test
	@DisplayName("미래 운행 정보 길찾기에서 result_code가 0이 아니면 빈 목록을 돌려준다")
	void driveSecondsAt_returnsEmptyListWhenResultCodeIsNotZero() {
		expectAt(DEST_A, "202611132200").andRespond(json("""
				{"routes":[{"result_code":304,"result_msg":"실패"}]}
				"""));

		assertThat(adapter.driveSecondsAt(ORIGIN, List.of(DEST_A), Instant.parse("2026-11-13T13:00:00Z"))).isEmpty();
	}

	@ParameterizedTest(name = "응답 {0}")
	@ValueSource(strings = {
			"{\"routes\":[{\"result_code\":0,\"result_msg\":\"길찾기 성공\"}]}",
			"{\"routes\":[{\"result_code\":0,\"result_msg\":\"길찾기 성공\",\"summary\":{\"distance\":2218}}]}",
			"{\"routes\":[{\"result_code\":0,\"result_msg\":\"길찾기 성공\",\"summary\":{\"distance\":2218,\"duration\":null}}]}",
			"{\"routes\":[]}",
			"{\"trans_id\":\"abc\"}",
			"<html>점검 중입니다</html>"
	})
	@DisplayName("summary나 duration이 없거나 routes가 비었거나 JSON이 아닌 응답이면 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenBodyHasNoUsableDuration(String body) {
		expectNow(DEST_A).andRespond(json(body));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A))).isEmpty();
	}

	@Test
	@DisplayName("여러 목적지 중 한 곳만 result_code가 0이 아니어도 일부 값 없이 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenAnyDestinationFailsByResultCode() {
		expectNow(DEST_A).andRespond(success(538));
		expectNow(DEST_B).andRespond(json("""
				{"routes":[{"result_code":304,"result_msg":"실패"}]}
				"""));
		expectNow(DEST_C).andRespond(success(95));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A, DEST_B, DEST_C))).isEmpty();
	}

	@Test
	@DisplayName("여러 목적지 중 한 곳만 HTTP 오류여도 빈 목록을 돌려준다")
	void driveSecondsNow_returnsEmptyListWhenAnyDestinationFailsByHttpError() {
		expectNow(DEST_A).andRespond(success(538));
		expectNow(DEST_B).andRespond(withStatus(HttpStatus.BAD_REQUEST));
		expectNow(DEST_C).andRespond(success(95));

		assertThat(adapter.driveSecondsNow(ORIGIN, List.of(DEST_A, DEST_B, DEST_C))).isEmpty();
	}

	@Test
	@DisplayName("미래 운행 정보 길찾기에서 여러 목적지 중 한 곳만 실패해도 빈 목록을 돌려준다")
	void driveSecondsAt_returnsEmptyListWhenAnyDestinationFails() {
		expectAt(DEST_A, "202611132200").andRespond(success(610));
		expectAt(DEST_B, "202611132200").andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThat(adapter.driveSecondsAt(ORIGIN, List.of(DEST_A, DEST_B), Instant.parse("2026-11-13T13:00:00Z")))
				.isEmpty();
	}

	@Test
	@DisplayName("목적지가 빈 목록이면 외부 API를 부르지 않고 빈 목록을 돌려준다 (지금 출발)")
	void driveSecondsNow_returnsEmptyListWithoutCallWhenNoDestinations() {
		assertThat(adapter.driveSecondsNow(ORIGIN, List.of())).isEmpty();

		server.verify();
	}

	@Test
	@DisplayName("목적지가 빈 목록이면 외부 API를 부르지 않고 빈 목록을 돌려준다 (departAt 있음)")
	void driveSecondsAt_returnsEmptyListWithoutCallWhenNoDestinations() {
		assertThat(adapter.driveSecondsAt(ORIGIN, List.of(), Instant.parse("2026-11-13T13:00:00Z"))).isEmpty();

		server.verify();
	}

	private ResponseActions expectNow(LatLng destination) {
		return server.expect(pathIs(DIRECTIONS_PATH))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("destination", lngLat(destination)));
	}

	private ResponseActions expectAt(LatLng destination, String departureTime) {
		return server.expect(pathIs(FUTURE_DIRECTIONS_PATH))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("destination", lngLat(destination)))
				.andExpect(queryParam("departure_time", departureTime));
	}

	private static String lngLat(LatLng point) {
		return point.lng() + "," + point.lat();
	}

	private static org.springframework.test.web.client.ResponseCreator success(int duration) {
		return json("""
				{"trans_id":"t","routes":[{"result_code":0,"result_msg":"길찾기 성공","summary":{"distance":2218,"duration":%d}}]}
				""".formatted(duration));
	}

	private static org.springframework.test.web.client.ResponseCreator json(String body) {
		return withSuccess(body, MediaType.APPLICATION_JSON);
	}

	private static RequestMatcher pathIs(String path) {
		return request -> assertThat(request.getURI().toString())
				.as("request url")
				.startsWith(BASE_URL + path + "?");
	}

	private static RequestMatcher queryParam(String name, String expected) {
		return request -> {
			String raw = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(name);
			assertThat(raw).as("query parameter " + name).isNotNull();
			assertThat(URLDecoder.decode(raw, StandardCharsets.UTF_8)).as("query parameter " + name).isEqualTo(expected);
		};
	}
}
