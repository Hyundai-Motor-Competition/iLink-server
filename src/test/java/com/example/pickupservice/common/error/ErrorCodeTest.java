package com.example.pickupservice.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

	private static final Path MAIN_SOURCE = Path.of("src/main/java/com/example/pickupservice");

	@Test
	@DisplayName("common ErrorCode.VEHICLE_ALREADY_LINKED는 409 「이미 보호자가 연결된 차량입니다」다")
	void vehicleAlreadyLinkedIsConflictWithGuardianLinkedMessage() {
		assertThat(ErrorCode.VEHICLE_ALREADY_LINKED.getStatus()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(ErrorCode.VEHICLE_ALREADY_LINKED.getMessage()).isEqualTo("이미 보호자가 연결된 차량입니다");
	}

	@Test
	@DisplayName("BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED)는 409 · code VEHICLE_ALREADY_LINKED · 「이미 보호자가 연결된 차량입니다」다")
	void businessExceptionOfVehicleAlreadyLinkedCarriesCommonCode() {
		BusinessException exception = BusinessException.of(ErrorCode.VEHICLE_ALREADY_LINKED);

		assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(exception.code()).isEqualTo("VEHICLE_ALREADY_LINKED");
		assertThat(exception.getMessage()).isEqualTo("이미 보호자가 연결된 차량입니다");
	}

	@Test
	@DisplayName("VEHICLE_ALREADY_LINKED는 common ErrorCode에만 정의되고 UserErrors · AuthErrors 등 main 소스 어디에도 \"VEHICLE_ALREADY_LINKED\" 문자열 코드로 따로 정의하지 않는다")
	void vehicleAlreadyLinkedIsNotRedefinedOutsideCommonErrorCode() {
		List<Path> redefining = mainSources()
				.filter(path -> contains(path, "\"VEHICLE_ALREADY_LINKED\""))
				.toList();

		assertThat(redefining).as("\"VEHICLE_ALREADY_LINKED\" 문자열로 코드를 만드는 main 소스").isEmpty();
		assertThat(read("user/application/service/UserErrors.java")).doesNotContain("vehicleAlreadyLinked");
		assertThat(read("auth/application/service/AuthErrors.java")).doesNotContain("vehicleAlreadyLinked");
	}

	private static Stream<Path> mainSources() {
		try (Stream<Path> files = Files.walk(MAIN_SOURCE)) {
			return files.filter(path -> path.toString().endsWith(".java")).toList().stream();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String read(String relative) {
		try {
			return Files.readString(MAIN_SOURCE.resolve(relative), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static boolean contains(Path path, String text) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8).contains(text);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
