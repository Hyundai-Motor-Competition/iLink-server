package com.example.pickupservice;

import com.example.pickupservice.common.fixture.PostgisTestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import(PostgisTestcontainersConfiguration.class)
class RealServerStartupTest {

	private final RestClient restClient;

	RealServerStartupTest(@LocalServerPort int port) {
		this.restClient = RestClient.create("http://localhost:" + port);
	}

	@Test
	@DisplayName("실제 웹 서버(RANDOM_PORT)로 컨텍스트가 뜨고 GET /actuator/health가 200과 status UP을 돌려준다")
	void actuatorHealthIsUpOnRealWebServer() {
		ResponseEntity<Map<String, Object>> response = restClient.get()
				.uri("/actuator/health")
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
				})
				.toEntity(new ParameterizedTypeReference<>() {
				});

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("status", "UP");
	}

}
