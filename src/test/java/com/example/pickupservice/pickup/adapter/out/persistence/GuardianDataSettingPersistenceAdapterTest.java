package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.out.LoadDataSettingPort;
import com.example.pickupservice.pickup.application.port.out.SaveDataSettingPort;
import com.example.pickupservice.pickup.domain.GuardianDataSetting;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@Import(UserSeed.class)
class GuardianDataSettingPersistenceAdapterTest extends IntegrationTestSupport {

	private final LoadDataSettingPort loadDataSettingPort;
	private final SaveDataSettingPort saveDataSettingPort;
	private final UserSeed userSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private UserSeed.Result family;
	/** 행이 없는 보호자가 다른 보호자의 행을 받지 않는지 볼 때 쓰는 두 번째 가족. */
	private UserSeed.Result otherFamily;

	@BeforeEach
	void seed() {
		family = userSeed.seed();
		otherFamily = userSeed.seed();
	}

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> entityManager
				.createQuery("delete from GuardianDataSettingEntity s where s.userId in :userIds")
				.setParameter("userIds", List.of(family.guardianId(), otherFamily.guardianId()))
				.executeUpdate());
		userSeed.clear(family);
		userSeed.clear(otherFamily);
	}

	@ParameterizedTest(name = "learningConsent {0}")
	@ValueSource(booleans = {true, false})
	@DisplayName("GuardianDataSetting을 saveDataSetting으로 저장하고 loadDataSetting(userId)으로 읽으면 userId · learningConsent가 같고 guardian_data_setting 행은 1건이다")
	void savedDataSettingIsLoadedWithSameLearningConsent(boolean learningConsent) {
		GuardianDataSetting toSave = GuardianDataSetting.restore(family.guardianId(), learningConsent);

		GuardianDataSetting saved = saveDataSettingPort.saveDataSetting(toSave);

		assertThat(saved).usingRecursiveComparison().as("저장 결과").isEqualTo(toSave);
		assertThat(loadDataSettingPort.loadDataSetting(family.guardianId()).orElseThrow())
				.usingRecursiveComparison()
				.as("다시 읽은 값")
				.isEqualTo(toSave);
		assertThat(jdbcTemplate.queryForObject("select learning_consent from guardian_data_setting where user_id = ?",
				Boolean.class, family.guardianId())).as("DB learning_consent").isEqualTo(learningConsent);
		assertThat(countRows(family.guardianId())).as("그 보호자의 행 수").isEqualTo(1);
	}

	@Test
	@DisplayName("learningConsent를 바꿔 다시 saveDataSetting하면 같은 행이 바뀌어 loadDataSetting이 바뀐 값을 돌려주고 행은 1건 그대로다")
	void changedLearningConsentIsSavedToTheSameRow() {
		saveDataSettingPort.saveDataSetting(GuardianDataSetting.restore(family.guardianId(), true));
		GuardianDataSetting setting = GuardianDataSetting.restore(family.guardianId(), true);

		setting.changeLearningConsent(false);
		saveDataSettingPort.saveDataSetting(setting);

		assertThat(loadDataSettingPort.loadDataSetting(family.guardianId()).orElseThrow().isLearningConsent())
				.as("true에서 false로 바꾼 뒤").isFalse();

		setting.changeLearningConsent(true);
		saveDataSettingPort.saveDataSetting(setting);

		assertThat(loadDataSettingPort.loadDataSetting(family.guardianId()).orElseThrow().isLearningConsent())
				.as("false에서 다시 true로 바꾼 뒤").isTrue();
		assertThat(countRows(family.guardianId())).as("세 번 저장한 뒤 그 보호자의 행 수").isEqualTo(1);
	}

	@Test
	@DisplayName("guardian_data_setting 행이 없는 보호자는 loadDataSetting이 빈 값을 돌려주고, 다른 보호자의 행이 저장돼 있어도 빈 값이다")
	void loadDataSettingIsEmptyForGuardianWithoutRow() {
		assertThat(loadDataSettingPort.loadDataSetting(family.guardianId())).as("아무 행도 없을 때").isEmpty();

		saveDataSettingPort.saveDataSetting(GuardianDataSetting.restore(otherFamily.guardianId(), true));

		assertThat(loadDataSettingPort.loadDataSetting(family.guardianId())).as("다른 보호자 행만 있을 때").isEmpty();
		assertThat(countRows(family.guardianId())).as("그 보호자의 행 수").isZero();
	}

	private int countRows(long userId) {
		return jdbcTemplate.queryForObject("select count(*) from guardian_data_setting where user_id = ?",
				Integer.class, userId);
	}

}
