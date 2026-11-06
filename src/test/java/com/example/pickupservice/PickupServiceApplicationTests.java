package com.example.pickupservice;

import com.example.pickupservice.common.fixture.PostgisTestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Import(PostgisTestcontainersConfiguration.class)
class PickupServiceApplicationTests {

	@Test
	@DisplayName("dev 프로필과 PostGIS 컨테이너로 애플리케이션 컨텍스트가 뜬다")
	void contextLoads() {
	}

}
