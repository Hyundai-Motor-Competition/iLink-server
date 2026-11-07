package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.security.Role;
import com.example.pickupservice.user.domain.JoinCallMode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@RequiredArgsConstructor
class PhoneEncryptionConverterTest extends IntegrationTestSupport {

	private static final String PHONE = "+821012345678";
	private static final String PHONE_DIGITS = "1012345678";
	private static final int GCM_NONCE_BYTES = 12;
	private static final int GCM_TAG_BYTES = 16;

	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private long familyId;

	@BeforeEach
	void createFamily() {
		familyId = transactionTemplate.execute(status -> {
			FamilyEntity family = new FamilyEntity(null, JoinCallMode.AUTO, 0);
			entityManager.persist(family);
			entityManager.flush();
			return family.getFamilyId();
		});
	}

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> {
			entityManager.createQuery("delete from UserEntity u where u.familyId = :familyId")
					.setParameter("familyId", familyId)
					.executeUpdate();
			entityManager.createQuery("delete from FamilyEntity f where f.familyId = :familyId")
					.setParameter("familyId", familyId)
					.executeUpdate();
		});
	}

	@Test
	@DisplayName("users.phone 컬럼에는 평문이 아닌 암호문이 저장되고 읽으면 원래 번호로 돌아온다")
	void phoneIsStoredEncryptedAndReadBackAsPlainNumber() {
		long userId = persistUser(Role.GUARDIAN, "김보호", PHONE);

		String storedPhone = storedPhoneOf(userId);
		assertThat(storedPhone).as("DB 원문").isNotNull().isNotEqualTo(PHONE).doesNotContain(PHONE_DIGITS);
		assertThatCode(() -> Base64.getDecoder().decode(storedPhone)).as("Base64 디코딩").doesNotThrowAnyException();
		assertThat(Base64.getDecoder().decode(storedPhone))
				.as("nonce 12바이트 + 태그 16바이트 + 평문 바이트 이상")
				.hasSizeGreaterThanOrEqualTo(GCM_NONCE_BYTES + GCM_TAG_BYTES + PHONE.getBytes(StandardCharsets.UTF_8).length);

		assertThat(loadPhoneOf(userId)).isEqualTo(PHONE);
	}

	@Test
	@DisplayName("같은 전화번호를 두 번 저장하면 두 암호문이 서로 다르다")
	void samePhoneSavedTwiceProducesDifferentCiphertexts() {
		long firstUserId = persistUser(Role.GUARDIAN, "김보호", PHONE);
		long secondUserId = persistUser(Role.CO_GUARDIAN, "이보호", PHONE);

		String firstStoredPhone = storedPhoneOf(firstUserId);
		String secondStoredPhone = storedPhoneOf(secondUserId);
		assertThat(firstStoredPhone).as("첫 번째 DB 원문").isNotNull().isNotEqualTo(PHONE);
		assertThat(secondStoredPhone).as("두 번째 DB 원문").isNotNull().isNotEqualTo(PHONE);
		assertThat(firstStoredPhone).as("두 암호문").isNotEqualTo(secondStoredPhone);

		assertThat(loadPhoneOf(firstUserId)).isEqualTo(PHONE);
		assertThat(loadPhoneOf(secondUserId)).isEqualTo(PHONE);
	}

	@Test
	@DisplayName("phone이 null인 CHILD는 users.phone에 null로 저장되고 읽어도 null이다")
	void nullPhoneIsStoredAndReadAsNull() {
		long userId = persistUser(Role.CHILD, "민준", null);

		assertThat(storedPhoneOf(userId)).isNull();
		assertThat(loadPhoneOf(userId)).isNull();
	}

	private long persistUser(Role role, String name, String phone) {
		return transactionTemplate.execute(status -> {
			UserEntity user = new UserEntity(null, familyId, role, name, phone);
			entityManager.persist(user);
			entityManager.flush();
			entityManager.clear();
			return user.getUserId();
		});
	}

	private String storedPhoneOf(long userId) {
		return jdbcTemplate.queryForObject("select phone from users where user_id = ?", String.class, userId);
	}

	private String loadPhoneOf(long userId) {
		return transactionTemplate.execute(status -> {
			entityManager.clear();
			return entityManager.find(UserEntity.class, userId).getPhone();
		});
	}

}
