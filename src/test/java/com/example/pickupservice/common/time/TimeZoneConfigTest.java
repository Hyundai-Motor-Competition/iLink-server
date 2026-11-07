package com.example.pickupservice.common.time;

import com.example.pickupservice.common.fixture.PostgisTestcontainersConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TimeZoneConfigTest.SeoulDefaultTimeZoneBeforeServerStartExtension.class)
@SpringBootTest
@ActiveProfiles("dev")
@Import(PostgisTestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RequiredArgsConstructor
class TimeZoneConfigTest {

	private final DataSource dataSource;

	@Test
	@DisplayName("JVM 기본 시간대가 Asia/Seoul인 환경에서 서버가 뜨면 JVM 기본 시간대가 UTC로 바뀌어 있다")
	void serverStartupSetsJvmDefaultTimeZoneToUtc() {
		assertThat(TimeZone.getDefault().getID()).isIn("UTC", "Etc/UTC");
	}

	@Test
	@DisplayName("JVM 기본 시간대가 Asia/Seoul인 환경에서 서버가 뜨면 커넥션 풀의 모든 DB 연결에서 SHOW TIME ZONE 결과가 UTC다")
	void serverStartupSetsEveryPooledDbConnectionTimeZoneToUtc() throws SQLException {
		int poolSize = dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize();
		List<Connection> connections = new ArrayList<>();
		List<String> dbTimeZones = new ArrayList<>();
		try {
			for (int i = 0; i < poolSize; i++) {
				connections.add(dataSource.getConnection());
			}
			for (Connection connection : connections) {
				dbTimeZones.add(showTimeZone(connection));
			}
		} finally {
			for (Connection connection : connections) {
				connection.close();
			}
		}

		assertThat(dbTimeZones)
				.hasSize(poolSize)
				.allSatisfy(dbTimeZone -> assertThat(dbTimeZone).isIn("UTC", "Etc/UTC"));
	}

	private static String showTimeZone(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SHOW TIME ZONE")) {
			resultSet.next();
			return resultSet.getString(1);
		}
	}

	static class SeoulDefaultTimeZoneBeforeServerStartExtension implements BeforeAllCallback, AfterAllCallback {

		private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");
		private static final ExtensionContext.Namespace NAMESPACE =
				ExtensionContext.Namespace.create(SeoulDefaultTimeZoneBeforeServerStartExtension.class);
		private static final String ORIGINAL_DEFAULT_TIME_ZONE = "originalDefaultTimeZone";

		@Override
		public void beforeAll(ExtensionContext context) {
			context.getStore(NAMESPACE).put(ORIGINAL_DEFAULT_TIME_ZONE, TimeZone.getDefault());
			TimeZone.setDefault(SEOUL);
		}

		@Override
		public void afterAll(ExtensionContext context) {
			TimeZone original = context.getStore(NAMESPACE).remove(ORIGINAL_DEFAULT_TIME_ZONE, TimeZone.class);
			boolean serverLeftSeoulAsIs = TimeZone.getDefault().getID().equals(SEOUL.getID());
			if (serverLeftSeoulAsIs) {
				TimeZone.setDefault(original);
			}
		}

	}

}
