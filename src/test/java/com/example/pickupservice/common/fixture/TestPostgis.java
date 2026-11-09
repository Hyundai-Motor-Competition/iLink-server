package com.example.pickupservice.common.fixture;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 테스트 DB(PostgreSQL 17 + PostGIS 3) 입구. 시스템 속성 pickup.test.db로 방식을 고른다.
 * <ul>
 *   <li>auto(없을 때): Windows는 EMBEDDED, 그 밖의 OS는 TESTCONTAINERS</li>
 *   <li>embedded: 내장 PostgreSQL 17 + PostGIS Windows 묶음(Docker 없음, Windows x64만)</li>
 *   <li>testcontainers: postgis/postgis:17-3.7 컨테이너(Docker 필요)</li>
 * </ul>
 * Gradle은 같은 규칙으로 고른 값을 이 속성으로 넘긴다(build.gradle의 -Ppickup.test.db).
 */
public final class TestPostgis {

	public static final String MODE_PROPERTY = "pickup.test.db";
	private static final Pattern DATABASE_NAME = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

	private TestPostgis() {
	}

	public static Database database(String name) {
		if (name == null || !DATABASE_NAME.matcher(name).matches()) {
			throw new IllegalArgumentException("DB 이름은 소문자 · 숫자 · _만 씁니다: " + name);
		}
		return switch (mode()) {
			case EMBEDDED -> EmbeddedPostgis.database(name);
			case TESTCONTAINERS -> PostgisContainer.database(name);
		};
	}

	public static Mode mode() {
		return Mode.resolve(System.getProperty(MODE_PROPERTY), System.getProperty("os.name"));
	}

	public record Database(String jdbcUrl, String username, String password) {
	}

	public enum Mode {

		EMBEDDED,
		TESTCONTAINERS;

		static Mode resolve(String requested, String osName) {
			String value = requested == null ? "" : requested.trim().toLowerCase(Locale.ROOT);
			return switch (value) {
				case "", "auto" -> isWindows(osName) ? EMBEDDED : TESTCONTAINERS;
				case "embedded" -> EMBEDDED;
				case "testcontainers" -> TESTCONTAINERS;
				default -> throw new IllegalArgumentException(
						MODE_PROPERTY + "는 auto · embedded · testcontainers 중 하나입니다: " + requested);
			};
		}

		private static boolean isWindows(String osName) {
			return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows");
		}

	}

}
