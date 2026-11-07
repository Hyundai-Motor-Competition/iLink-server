package com.example.pickupservice.common.fixture;

import com.example.pickupservice.user.fixture.UserSeed;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({PostgisTestcontainersConfiguration.class, AccessTokenFixture.class, UserSeed.class})
public abstract class IntegrationTestSupport {
}
