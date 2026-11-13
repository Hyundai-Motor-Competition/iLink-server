package com.example.pickupservice.common.fixture;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({TestPostgisConfiguration.class, AccessTokenFixture.class, InMemoryProfileImageStorageConfiguration.class})
public abstract class IntegrationTestSupport {
}
