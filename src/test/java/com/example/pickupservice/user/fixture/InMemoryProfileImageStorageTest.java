package com.example.pickupservice.user.fixture;

import com.example.pickupservice.common.image.StorageUnavailableException;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage.StoredObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryProfileImageStorageTest {

	private static final String KEY = "profiles/3f6c0000-0000-0000-0000-000000000001.png";
	private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01};

	private final InMemoryProfileImageStorage storage = new InMemoryProfileImageStorage();

	@Test
	@DisplayName("put한 키 · 바이트 · Content-Type을 그대로 기록한다")
	void putRecordsKeyBytesAndContentType() {
		storage.put(KEY, PNG, "image/png");

		assertThat(storage.storedKeys()).containsExactly(KEY);
		StoredObject stored = storage.stored(KEY).orElseThrow();
		assertThat(stored.bytes()).containsExactly(PNG);
		assertThat(stored.contentType()).isEqualTo("image/png");
		assertThat(storage.stored("profiles/none.png")).isEmpty();
	}

	@Test
	@DisplayName("presignedUrl은 키가 담긴 가짜 서명 URL(https://storage.test/ilink/{key}?sig=test)을 준다")
	void presignedUrlContainsKey() {
		assertThat(storage.presignedUrl(KEY))
				.contains("https://storage.test/ilink/" + KEY + "?sig=test");
	}

	@Test
	@DisplayName("defaultUrl은 defaults/avatar.png의 서명 URL을 주고 기본 상태는 configured true다")
	void defaultUrlPointsToDefaultAvatarAndConfiguredByDefault() {
		assertThat(storage.configured()).isTrue();
		assertThat(storage.defaultUrl())
				.contains("https://storage.test/ilink/defaults/avatar.png?sig=test");
	}

	@Test
	@DisplayName("delete한 키를 기록하고 저장된 객체를 지운다")
	void deleteRecordsKeyAndRemovesObject() {
		storage.put(KEY, PNG, "image/png");

		storage.delete(KEY);

		assertThat(storage.deletedKeys()).containsExactly(KEY);
		assertThat(storage.stored(KEY)).isEmpty();
	}

	@Test
	@DisplayName("failUploads 뒤 put은 StorageUnavailableException으로 끝나고 아무것도 기록하지 않는다")
	void failUploadsMakesPutThrowWithoutRecording() {
		storage.failUploads();

		assertThatThrownBy(() -> storage.put(KEY, PNG, "image/png"))
				.isInstanceOf(StorageUnavailableException.class);
		assertThat(storage.storedKeys()).isEmpty();
		assertThat(storage.configured()).isTrue();
	}

	@Test
	@DisplayName("unconfigure 뒤에는 configured false, presignedUrl · defaultUrl은 비고, put은 StorageUnavailableException으로 끝난다")
	void unconfigureMakesStorageLookAbsent() {
		storage.unconfigure();

		assertThat(storage.configured()).isFalse();
		assertThat(storage.presignedUrl(KEY)).isEmpty();
		assertThat(storage.defaultUrl()).isEmpty();
		assertThatThrownBy(() -> storage.put(KEY, PNG, "image/png"))
				.isInstanceOf(StorageUnavailableException.class);
		assertThat(storage.storedKeys()).isEmpty();
	}

	@Test
	@DisplayName("reset은 기록을 지우고 정상 · 설정됨 상태로 되돌린다")
	void resetClearsRecordsAndRestoresNormalMode() {
		storage.put(KEY, PNG, "image/png");
		storage.delete(KEY);
		storage.unconfigure();

		storage.reset();

		assertThat(storage.storedKeys()).isEmpty();
		assertThat(storage.deletedKeys()).isEmpty();
		assertThat(storage.configured()).isTrue();
		storage.put(KEY, PNG, "image/png");
		assertThat(storage.stored(KEY)).isPresent();
	}
}
