package com.example.pickupservice.common.fixture;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Windows용 테스트 DB: zonky 내장 PostgreSQL 17에 OSGeo PostGIS Windows 묶음을 얹어 로컬 프로세스로 띄운다(Docker 없음).
 * JVM당 서버 1개를 처음 쓸 때 빈 포트로 띄우고, JVM이 끝나면 멈추고 데이터 폴더를 지운다.
 */
final class EmbeddedPostgis {

	private static final String POSTGIS_DIR_PROPERTY = "pickup.test.postgis-dir";
	private static final Path BINARIES_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "pickup-embedded-pg");
	private static final String SUPERUSER = "postgres";
	private static final String MAX_CONNECTIONS = "200";
	private static final Pattern CONTROL_VERSION = Pattern.compile("default_version\\s*=\\s*'([^']+)'");
	private static final List<String> POSTGIS_SUBDIRS = List.of("bin", "lib", "share");
	private static final Map<String, String> PG_CONFIG_NAMES = Map.of("bin", "BINDIR", "lib", "PKGLIBDIR", "share", "SHAREDIR");
	private static final String INSTALLED_MARKER = ".postgis-installed";
	private static final String INSTALL_LOCK = ".postgis-install.lock";

	private EmbeddedPostgis() {
	}

	static TestPostgis.Database database(String name) {
		return Server.INSTANCE.database(name);
	}

	private static final class Server {

		private static final Server INSTANCE = start();

		private final EmbeddedPostgres postgres;
		private final Set<String> createdDatabases = new HashSet<>();

		private Server(EmbeddedPostgres postgres) {
			this.postgres = postgres;
		}

		private static Server start() {
			Path postgisDir = postgisDir();
			try {
				EmbeddedPostgres postgres = EmbeddedPostgres.builder()
						.setOverrideWorkingDirectory(BINARIES_ROOT.resolve("postgis-" + postgisVersion(postgisDir)).toFile())
						.setLocaleConfig("locale", "C")
						.setServerConfig("max_connections", MAX_CONNECTIONS)
						.setServerConfig("logging_collector", "on")
						.start();
				installPostgis(postgres, postgisDir);
				return new Server(postgres);
			} catch (IOException | SQLException e) {
				throw new IllegalStateException("테스트용 PostgreSQL + PostGIS 서버를 띄우지 못했습니다", e);
			}
		}

		private synchronized TestPostgis.Database database(String name) {
			if (!createdDatabases.contains(name)) {
				createDatabaseWithPostgis(name);
				createdDatabases.add(name);
			}
			return new TestPostgis.Database("jdbc:postgresql://localhost:" + postgres.getPort() + "/" + name, SUPERUSER, "");
		}

		private void createDatabaseWithPostgis(String name) {
			try {
				try (Connection connection = postgres.getPostgresDatabase().getConnection();
					 Statement statement = connection.createStatement()) {
					statement.execute("CREATE DATABASE " + name);
				}
				try (Connection connection = postgres.getDatabase(SUPERUSER, name).getConnection();
					 Statement statement = connection.createStatement()) {
					statement.execute("CREATE EXTENSION postgis");
				}
			} catch (SQLException e) {
				throw new IllegalStateException("테스트 DB " + name + "를 만들지 못했습니다", e);
			}
		}

	}

	private static Path postgisDir() {
		String dir = System.getProperty(POSTGIS_DIR_PROPERTY);
		if (dir == null || dir.isBlank()) {
			throw new IllegalStateException("시스템 속성 " + POSTGIS_DIR_PROPERTY + "가 없습니다. "
					+ "Windows에서 Gradle(test · bootTestRun)로 실행하면 PostGIS 묶음을 받아 이 속성으로 넘깁니다. "
					+ "Windows가 아니면 " + TestPostgis.MODE_PROPERTY + "=testcontainers로 돌립니다");
		}
		return Path.of(dir);
	}

	private static String postgisVersion(Path postgisDir) throws IOException {
		String control = Files.readString(postgisDir.resolve("share").resolve("extension").resolve("postgis.control"));
		Matcher matcher = CONTROL_VERSION.matcher(control);
		if (!matcher.find()) {
			throw new IllegalStateException("postgis.control에 default_version이 없습니다: " + postgisDir);
		}
		return matcher.group(1);
	}

	private static void installPostgis(EmbeddedPostgres postgres, Path postgisDir) throws SQLException, IOException {
		Map<String, Path> targets = pgConfigDirs(postgres);
		Path pgRoot = targets.get("bin").getParent();
		try (FileChannel lockChannel = FileChannel.open(pgRoot.resolve(INSTALL_LOCK),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			 FileLock ignored = lockChannel.lock()) {
			Path marker = pgRoot.resolve(INSTALLED_MARKER);
			if (Files.exists(marker)) {
				return;
			}
			for (String subdir : POSTGIS_SUBDIRS) {
				copyMissingFiles(postgisDir.resolve(subdir), targets.get(subdir));
			}
			Files.createFile(marker);
		}
	}

	private static Map<String, Path> pgConfigDirs(EmbeddedPostgres postgres) throws SQLException {
		Map<String, Path> dirs = new HashMap<>();
		try (Connection connection = postgres.getPostgresDatabase().getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT name, setting FROM pg_config")) {
			Map<String, String> settings = new HashMap<>();
			while (resultSet.next()) {
				settings.put(resultSet.getString("name"), resultSet.getString("setting"));
			}
			PG_CONFIG_NAMES.forEach((subdir, configName) -> dirs.put(subdir, Path.of(settings.get(configName))));
		}
		return dirs;
	}

	private static void copyMissingFiles(Path source, Path target) throws IOException {
		try (Stream<Path> files = Files.walk(source)) {
			files.filter(Files::isRegularFile).forEach(file -> copyIfMissing(file, target.resolve(source.relativize(file))));
		}
	}

	private static void copyIfMissing(Path file, Path destination) {
		if (Files.exists(destination)) {
			return;
		}
		try {
			Files.createDirectories(destination.getParent());
			Path partial = destination.resolveSibling(destination.getFileName() + ".partial");
			Files.copy(file, partial, StandardCopyOption.REPLACE_EXISTING);
			Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
