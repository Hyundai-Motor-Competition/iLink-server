package com.example.pickupservice.common.fixture;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Windows가 아닌 OS(Linux CI 등)의 테스트 DB: Testcontainers postgis/postgis:17-3.7 컨테이너(Docker 필요).
 * JVM당 컨테이너 1개를 처음 쓸 때 띄운다. 처음 요청한 DB는 이미지의 초기화 스크립트가 PostGIS 확장과 함께 만들고,
 * 그 뒤 다른 이름은 이미지의 template_postgis를 복제해 만든다.
 */
final class PostgisContainer {

	private static final DockerImageName POSTGIS_IMAGE =
			DockerImageName.parse("postgis/postgis:17-3.7").asCompatibleSubstituteFor("postgres");
	private static final int MAX_CONNECTIONS = 200;

	private static PostgreSQLContainer container;
	private static final Set<String> createdDatabases = new HashSet<>();

	private PostgisContainer() {
	}

	static synchronized TestPostgis.Database database(String name) {
		if (container == null) {
			container = start(name);
		}
		if (container.getDatabaseName().equals(name)) {
			return new TestPostgis.Database(container.getJdbcUrl(), container.getUsername(), container.getPassword());
		}
		if (createdDatabases.add(name)) {
			createDatabaseFromPostgisTemplate(name);
		}
		String jdbcUrl = "jdbc:postgresql://" + container.getHost() + ":"
				+ container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) + "/" + name;
		return new TestPostgis.Database(jdbcUrl, container.getUsername(), container.getPassword());
	}

	private static PostgreSQLContainer start(String databaseName) {
		PostgreSQLContainer postgis = new PostgreSQLContainer(POSTGIS_IMAGE)
				.withDatabaseName(databaseName)
				.withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=" + MAX_CONNECTIONS);
		postgis.start();
		return postgis;
	}

	private static void createDatabaseFromPostgisTemplate(String name) {
		try (Connection connection = container.createConnection("");
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE DATABASE " + name + " TEMPLATE template_postgis");
		} catch (SQLException e) {
			throw new IllegalStateException("테스트 DB " + name + "를 만들지 못했습니다", e);
		}
	}

}
