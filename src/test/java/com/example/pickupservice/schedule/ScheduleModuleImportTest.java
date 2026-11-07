package com.example.pickupservice.schedule;

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

class ScheduleModuleImportTest {

	private static final Path SCHEDULE_MAIN = Path.of("src/main/java/com/example/pickupservice/schedule");

	@Test
	@DisplayName("schedule main 소스는 user.domain 타입을 쓰지 않는다(다른 모듈은 in 포트만)")
	void scheduleMainSourcesDoNotUseUserDomain() throws IOException {
		List<Path> sources;
		try (Stream<Path> files = Files.walk(SCHEDULE_MAIN)) {
			sources = files.filter(file -> file.toString().endsWith(".java")).toList();
		}

		List<String> violations = sources.stream()
				.filter(file -> read(file).contains("com.example.pickupservice.user.domain"))
				.map(file -> SCHEDULE_MAIN.relativize(file).toString())
				.toList();

		assertThat(sources).isNotEmpty();
		assertThat(violations).isEmpty();
	}

	private static String read(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
