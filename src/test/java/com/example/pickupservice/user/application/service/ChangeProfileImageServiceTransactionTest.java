package com.example.pickupservice.user.application.service;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.image.ProfileImage;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageCommand;
import com.example.pickupservice.user.application.port.in.ChangeProfileImageUseCase;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.fixture.InMemoryProfileImageStorage;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.example.pickupservice.common.fixture.TestImages.PNG_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RequiredArgsConstructor
@Import(UserSeed.class)
class ChangeProfileImageServiceTransactionTest extends IntegrationTestSupport {

	private static final byte[] JPEG_BYTES = {
			(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};

	private final ChangeProfileImageUseCase changeProfileImageUseCase;
	private final LoadUserPort loadUserPort;
	private final InMemoryProfileImageStorage storage;
	private final UserSeed userSeed;
	private final TransactionTemplate transactionTemplate;

	private UserSeed.Result seeded;
	private String previousKey;

	@BeforeEach
	void setUp() {
		storage.reset();
		seeded = userSeed.seed();
		changeProfileImageUseCase.changeProfileImage(
				new ChangeProfileImageCommand(seeded.guardianId(), ProfileImage.of(JPEG_BYTES)));
		previousKey = profileImageKeyOf(seeded.guardianId());
		assertThat(previousKey).isNotNull();
		storage.reset();
	}

	@AfterEach
	void tearDown() {
		userSeed.clear(seeded);
		storage.reset();
	}

	@Test
	@DisplayName("연결 승인 RELINK의 이전 사진 객체는 트랜잭션이 커밋되기 전에는 지우지 않고 커밋된 뒤에 지운다")
	void deletesPreviousObjectOnlyAfterCommit() {
		List<String> deletedBeforeCommit = new ArrayList<>();

		transactionTemplate.executeWithoutResult(status -> {
			changeProfileImageUseCase.changeProfileImage(
					new ChangeProfileImageCommand(seeded.guardianId(), ProfileImage.of(PNG_BYTES)));
			deletedBeforeCommit.addAll(storage.deletedKeys());
		});

		assertThat(deletedBeforeCommit).isEmpty();
		assertThat(storage.deletedKeys()).containsExactly(previousKey);
		String currentKey = profileImageKeyOf(seeded.guardianId());
		assertThat(currentKey).isNotEqualTo(previousKey);
		assertThat(storage.storedKeys()).contains(currentKey);
	}

	@Test
	@DisplayName("연결 승인 RELINK에서 사진 교체 뒤 같은 트랜잭션의 다음 단계가 실패해 롤백되면 이전 사진 객체를 지우지 않는다")
	void keepsPreviousObjectWhenTransactionRollsBack() {
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			changeProfileImageUseCase.changeProfileImage(
					new ChangeProfileImageCommand(seeded.guardianId(), ProfileImage.of(PNG_BYTES)));
			throw new IllegalStateException("기기 등록 실패");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(storage.deletedKeys()).isEmpty();
		assertThat(profileImageKeyOf(seeded.guardianId())).isEqualTo(previousKey);
	}

	private String profileImageKeyOf(long userId) {
		return loadUserPort.loadUser(userId).orElseThrow().getProfileImageKey();
	}
}
