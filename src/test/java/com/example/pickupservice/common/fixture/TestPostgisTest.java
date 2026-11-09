package com.example.pickupservice.common.fixture;

import com.example.pickupservice.common.fixture.TestPostgis.Mode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestPostgisTest {

	private static final String WINDOWS = "Windows 11";
	private static final String LINUX = "Linux";
	private static final String MAC = "Mac OS X";

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@NullAndEmptySource
	@ValueSource(strings = {"auto", " AUTO ", "   "})
	@DisplayName("pickup.test.db가 없거나 auto면 Windows에서는 내장 서버를 고른다")
	void autoPicksEmbeddedOnWindows(String requested) {
		assertThat(Mode.resolve(requested, WINDOWS)).isEqualTo(Mode.EMBEDDED);
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = {LINUX, MAC})
	@DisplayName("pickup.test.db가 없으면 Windows가 아닌 OS에서는 Testcontainers를 고른다")
	void autoPicksTestcontainersOutsideWindows(String osName) {
		assertThat(Mode.resolve(null, osName)).isEqualTo(Mode.TESTCONTAINERS);
	}

	@Test
	@DisplayName("pickup.test.db=embedded는 OS와 상관없이 내장 서버를 고른다")
	void embeddedPropertyForcesEmbedded() {
		assertThat(Mode.resolve("embedded", LINUX)).isEqualTo(Mode.EMBEDDED);
		assertThat(Mode.resolve(" Embedded ", WINDOWS)).isEqualTo(Mode.EMBEDDED);
	}

	@Test
	@DisplayName("pickup.test.db=testcontainers는 Windows에서도 Testcontainers를 고른다")
	void testcontainersPropertyForcesTestcontainers() {
		assertThat(Mode.resolve("testcontainers", WINDOWS)).isEqualTo(Mode.TESTCONTAINERS);
		assertThat(Mode.resolve("TESTCONTAINERS", LINUX)).isEqualTo(Mode.TESTCONTAINERS);
	}

	@Test
	@DisplayName("pickup.test.db에 정해진 값이 아닌 h2를 넣으면 쓸 수 있는 값을 알려 주며 실패한다")
	void unknownPropertyValueFails() {
		assertThatThrownBy(() -> Mode.resolve("h2", WINDOWS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("h2")
				.hasMessageContaining("auto")
				.hasMessageContaining("embedded")
				.hasMessageContaining("testcontainers");
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = {"Pickup", "pickup-test", "1pickup", "pickup;drop", ""})
	@DisplayName("DB 이름에 소문자 · 숫자 · _ 밖의 글자가 있으면 서버를 띄우기 전에 실패한다")
	void invalidDatabaseNameFailsBeforeStartingServer(String name) {
		assertThatThrownBy(() -> TestPostgis.database(name))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("DB 이름");
	}

}
