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
	@DisplayName("schedule main 소스 어디에도 user.domain 패키지 문자열(com.example.pickupservice.user.domain)이 없다")
	void scheduleMainSourcesDoNotContainUserDomainPackageName() throws IOException {
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

	@Test
	@DisplayName("schedule main 소스에서 pickup 모듈 참조는 adapter/out/pickup 아래의 pickup.application.port.in뿐이다")
	void scheduleMainSourcesReferPickupOnlyThroughInPortFromOutPickupAdapter() throws IOException {
		Path pickupAdapter = SCHEDULE_MAIN.resolve("adapter/out/pickup");
		List<Path> sources;
		try (Stream<Path> files = Files.walk(SCHEDULE_MAIN)) {
			sources = files.filter(file -> file.toString().endsWith(".java")).toList();
		}

		List<String> violations = sources.stream()
				.filter(file -> read(file).lines()
						.filter(line -> line.contains("com.example.pickupservice.pickup."))
						.anyMatch(line -> !file.startsWith(pickupAdapter)
								|| !line.contains("com.example.pickupservice.pickup.application.port.in.")))
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
