package com.example.pickupservice.place;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceDomainImportTest {

	private static final Path PLACE_DOMAIN_MAIN = Path.of("src/main/java/com/example/pickupservice/place/domain");

	@Test
	@DisplayName("place domain 소스 어디에도 JTS를 쓰는 common.geo.GeoPoints 문자열이 없다")
	void placeDomainSourcesDoNotUseGeoPoints() throws IOException {
		List<Path> sources = javaSources();

		List<String> violations = sources.stream()
				.filter(file -> read(file).contains("common.geo.GeoPoints"))
				.map(file -> PLACE_DOMAIN_MAIN.relativize(file).toString())
				.toList();

		assertThat(sources).isNotEmpty();
		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("place domain 소스 어디에도 JTS 패키지(org.locationtech.jts)를 import하지 않는다")
	void placeDomainSourcesDoNotImportJts() throws IOException {
		List<Path> sources = javaSources();

		List<String> violations = sources.stream()
				.filter(file -> read(file).contains("org.locationtech.jts"))
				.map(file -> PLACE_DOMAIN_MAIN.relativize(file).toString())
				.toList();

		assertThat(sources).isNotEmpty();
		assertThat(violations).isEmpty();
	}

	private static List<Path> javaSources() throws IOException {
		try (Stream<Path> files = Files.walk(PLACE_DOMAIN_MAIN)) {
			return files.filter(file -> file.toString().endsWith(".java")).toList();
		}
	}

	private static String read(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
