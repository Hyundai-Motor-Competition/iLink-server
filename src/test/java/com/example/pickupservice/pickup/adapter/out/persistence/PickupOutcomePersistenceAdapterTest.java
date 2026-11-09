package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.out.LoadOutcomePort;
import com.example.pickupservice.pickup.application.port.out.SaveOutcomePort;
import com.example.pickupservice.pickup.domain.BoardingMethod;
import com.example.pickupservice.pickup.domain.CorrectionReason;
import com.example.pickupservice.pickup.domain.CorrectionStatus;
import com.example.pickupservice.pickup.domain.PickupOutcome;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class PickupOutcomePersistenceAdapterTest extends IntegrationTestSupport {

	private static final Instant T0 = Instant.parse("2026-11-09T07:00:00Z");
	private static final Instant PREDICTED_STOP_AT = T0.plusSeconds(600);
	private static final Instant ACTUAL_STOP_AT = T0.plusSeconds(630);
	private static final Instant COMPLETED_AT = T0.plusSeconds(900);
	private static final Instant CORRECTED_STOP_AT = T0.plusSeconds(700);
	private static final String CORRECTION_NOTE = "실제로 차가 선 시각이 달라요";
	/** PostgreSQL이 PRIMARY KEY(session_id)에 붙이는 기본 이름. */
	private static final String OUTCOME_PRIMARY_KEY = "pickup_outcome_pkey";

	private final LoadOutcomePort loadOutcomePort;
	private final SaveOutcomePort saveOutcomePort;
	private final PickupSeed pickupSeed;
	private final EntityManager entityManager;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private PickupSeed.Result seeded;

	@BeforeEach
	void seed() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void clear() {
		transactionTemplate.executeWithoutResult(status -> entityManager
				.createQuery("delete from PickupOutcomeEntity o where o.sessionId = :sessionId")
				.setParameter("sessionId", seeded.sessionId())
				.executeUpdate());
		pickupSeed.clear(seeded);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(BoardingMethod.class)
	@DisplayName("saveOutcome으로 저장한 PickupOutcome은 session_id를 키로 pickup_outcome 행 1건이 되고, loadOutcome(sessionId)으로 읽으면 completedAt · boardingMethod · predictedStopAt · actualStopAt이 같고 정정 값은 비어 있으며, 저장 전에는 빈 값이다")
	void savedOutcomeIsStoredBySessionIdAndLoadedWithSameValues(BoardingMethod boardingMethod) {
		assertThat(loadOutcomePort.loadOutcome(seeded.sessionId())).as("저장 전").isEmpty();
		PickupOutcome toSave = PickupOutcome.complete(seeded.sessionId(), COMPLETED_AT, boardingMethod,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT);

		PickupOutcome saved = saveOutcomePort.saveOutcome(toSave);

		assertThat(saved).usingRecursiveComparison().as("저장 결과").isEqualTo(toSave);
		assertThat(loadOutcomePort.loadOutcome(seeded.sessionId()).orElseThrow())
				.usingRecursiveComparison()
				.as("다시 읽은 값")
				.isEqualTo(toSave);
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select boarding_method, correction_status from pickup_outcome where session_id = ?",
				seeded.sessionId());
		assertThat(row.get("boarding_method")).isEqualTo(boardingMethod.name());
		assertThat(row.get("correction_status")).as("정정 전 correction_status").isNull();
		assertThat(countOutcomeRows()).as("그 세션의 pickup_outcome 행 수").isEqualTo(1);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(CorrectionReason.class)
	@DisplayName("같은 세션의 PickupOutcome을 정정해 한 번 더 saveOutcome하면 두 번째 행이 생기지 않고 그 행이 바뀌어, actualStopAt 원본은 그대로이고 correctedStopAt · correctionReason · correctionStatus REQUESTED · correctedBy · correctionNote가 저장된다")
	void savingOutcomeAgainForSameSessionUpdatesTheOnlyRow(CorrectionReason reason) {
		saveOutcomePort.saveOutcome(completedOutcome());
		PickupOutcome corrected = completedOutcome();
		corrected.correct(CORRECTED_STOP_AT, reason, seeded.guardianId(), CORRECTION_NOTE);

		PickupOutcome saved = saveOutcomePort.saveOutcome(corrected);

		PickupOutcome expected = PickupOutcome.restore(seeded.sessionId(), COMPLETED_AT, BoardingMethod.DOOR,
				PREDICTED_STOP_AT, ACTUAL_STOP_AT, CORRECTED_STOP_AT, reason, CorrectionStatus.REQUESTED,
				seeded.guardianId(), CORRECTION_NOTE);
		assertThat(saved).usingRecursiveComparison().as("저장 결과").isEqualTo(expected);
		assertThat(loadOutcomePort.loadOutcome(seeded.sessionId()).orElseThrow())
				.usingRecursiveComparison()
				.as("다시 읽은 값")
				.isEqualTo(expected);
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select correction_reason, correction_status from pickup_outcome where session_id = ?",
				seeded.sessionId());
		assertThat(row.get("correction_reason")).isEqualTo(reason.name());
		assertThat(row.get("correction_status")).isEqualTo(CorrectionStatus.REQUESTED.name());
		assertThat(countOutcomeRows()).as("두 번 저장한 뒤 그 세션의 pickup_outcome 행 수").isEqualTo(1);
	}

	@Test
	@DisplayName("PickupOutcome이 저장된 세션에 session_id가 같은 행을 하나 더 넣으면 pickup_outcome PK(session_id) 위반 DuplicateKeyException이 나고, 행은 1건 · 값은 처음 저장한 그대로다")
	void secondRowWithSameSessionIdIsRejectedByPrimaryKey() {
		PickupOutcome first = completedOutcome();
		saveOutcomePort.saveOutcome(first);

		Throwable thrown = catchThrowable(() -> jdbcTemplate.update("""
						insert into pickup_outcome (session_id, completed_at, boarding_method, created_at, updated_at)
						values (?, ?, ?, now(), now())
						""", seeded.sessionId(), COMPLETED_AT.plusSeconds(60).atOffset(ZoneOffset.UTC),
				BoardingMethod.MANUAL.name()));

		assertThat(thrown).isInstanceOf(DuplicateKeyException.class).hasMessageContaining(OUTCOME_PRIMARY_KEY);
		assertThat(countOutcomeRows()).as("그 세션의 pickup_outcome 행 수").isEqualTo(1);
		assertThat(loadOutcomePort.loadOutcome(seeded.sessionId()).orElseThrow())
				.usingRecursiveComparison()
				.as("처음 저장한 값")
				.isEqualTo(first);
	}

	private PickupOutcome completedOutcome() {
		return PickupOutcome.complete(seeded.sessionId(), COMPLETED_AT, BoardingMethod.DOOR, PREDICTED_STOP_AT,
				ACTUAL_STOP_AT);
	}

	private int countOutcomeRows() {
		return jdbcTemplate.queryForObject("select count(*) from pickup_outcome where session_id = ?", Integer.class,
				seeded.sessionId());
	}

}
