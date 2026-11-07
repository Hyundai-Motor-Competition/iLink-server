package com.example.pickupservice.common.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlateNumberMaskerTest {

	private static final Path MAIN_SOURCE = Path.of("src/main/java/com/example/pickupservice");

	@Test
	@DisplayName("번호판 마스킹은 「12가 3456」을 「12가 ****」로 바꾼다")
	void masksLastFourDigitsOfPlateNumber() {
		assertThat(PlateNumberMasker.mask("12가 3456")).isEqualTo("12가 ****");
	}

	@ParameterizedTest(name = "[{index}] \"{0}\" -> \"{1}\"")
	@CsvSource({
			"12가 3456, 12가 ****",
			"123가 4567, 123가 ****",
			"서울12가3456, 서울12가****"
	})
	@DisplayName("번호판 마스킹은 앞부분을 그대로 두고 마지막 숫자 4자리만 *로 바꾼다")
	void keepsPrefixAndMasksOnlyLastFourDigits(String plateNumber, String expected) {
		assertThat(PlateNumberMasker.mask(plateNumber)).isEqualTo(expected);
	}

	@Test
	@DisplayName("번호판 마스킹은 null 번호판에 null을 돌려준다")
	void returnsNullForNullPlateNumber() {
		assertThat(PlateNumberMasker.mask(null)).isNull();
	}

	@Test
	@DisplayName("번호판 마스킹(PlateNumberMasker)은 common에 한 번만 두고 user 가족 조회 · auth 연결 코드 확인이 그것을 쓴다")
	void plateNumberMaskerLivesOnlyInCommonAndFamilyQueryAndLinkCodeCheckUseIt() {
		assertThatThrownBy(() -> Class.forName("com.example.pickupservice.user.domain.PlateNumberMasker"))
				.isInstanceOf(ClassNotFoundException.class);
		assertThat(PlateNumberMasker.class.getPackageName()).isEqualTo("com.example.pickupservice.common.text");

		assertThat(mainSourcesNamed("PlateNumberMasker.java"))
				.containsExactly(MAIN_SOURCE.resolve("common/text/PlateNumberMasker.java"));

		assertThat(read("user/application/service/GetMyFamilyService.java"))
				.contains("import com.example.pickupservice.common.text.PlateNumberMasker;");
		assertThat(read("auth/application/service/CheckLinkCodeService.java"))
				.contains("import com.example.pickupservice.common.text.PlateNumberMasker;");

		assertThat(sourcesUnderContaining(MAIN_SOURCE, "import com.example.pickupservice.user.domain.PlateNumberMasker"))
				.isEmpty();
		assertThat(sourcesUnderContaining(MAIN_SOURCE.resolve("auth"), "import com.example.pickupservice.user.domain."))
				.isEmpty();
	}

	private static String read(String relative) {
		try {
			return Files.readString(MAIN_SOURCE.resolve(relative), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<Path> mainSourcesNamed(String fileName) {
		try (Stream<Path> files = Files.walk(MAIN_SOURCE)) {
			return files.filter(path -> path.getFileName().toString().equals(fileName)).toList();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<Path> sourcesUnderContaining(Path root, String text) {
		try (Stream<Path> files = Files.walk(root)) {
			return files.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> contains(path, text))
					.toList();
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
