package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.pickup.application.port.out.ProcessedVehicleEventPort;
import com.example.pickupservice.pickup.fixture.PickupSeed;
import com.example.pickupservice.place.fixture.PlaceSeed;
import com.example.pickupservice.schedule.fixture.ScheduleSeed;
import com.example.pickupservice.user.fixture.UserSeed;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RequiredArgsConstructor
@Import({UserSeed.class, ScheduleSeed.class, PlaceSeed.class, PickupSeed.class})
class ProcessedVehicleEventPersistenceAdapterTest extends IntegrationTestSupport {

	private final ProcessedVehicleEventPort processedVehicleEventPort;
	private final PickupSeed pickupSeed;
	private final TransactionTemplate transactionTemplate;
	private final JdbcTemplate jdbcTemplate;

	private PickupSeed.Result seeded;

	@BeforeEach
	void seed() {
		seeded = pickupSeed.seed();
	}

	@AfterEach
	void clear() {
		jdbcTemplate.update("delete from processed_vehicle_event where session_id = ?", seeded.sessionId());
		pickupSeed.clear(seeded);
	}

	@Test
	@DisplayName("markProcessed는 처음 보는 eventId에 true, 뒤 트랜잭션에서 같은 eventId에 false, 다른 eventId에 true를 돌려주고 processed_vehicle_event에는 eventId마다 행이 1건씩 sessionId와 함께 남는다")
	void markProcessedIsTrueOnlyForFirstEventIdAcrossTransactions() {
		String eventId = UUID.randomUUID().toString();
		String otherEventId = UUID.randomUUID().toString();

		Boolean first = transactionTemplate.execute(tx -> processedVehicleEventPort.markProcessed(seeded.sessionId(),
				eventId));
		Boolean duplicate = transactionTemplate.execute(
				tx -> processedVehicleEventPort.markProcessed(seeded.sessionId(), eventId));
		Boolean other = transactionTemplate.execute(
				tx -> processedVehicleEventPort.markProcessed(seeded.sessionId(), otherEventId));

		assertThat(first).as("처음 eventId").isTrue();
		assertThat(duplicate).as("같은 eventId").isFalse();
		assertThat(other).as("다른 eventId").isTrue();
		assertThat(jdbcTemplate.queryForList("select event_id, session_id from processed_vehicle_event where session_id = ?",
				seeded.sessionId()))
				.extracting(row -> row.get("event_id"), row -> ((Number) row.get("session_id")).longValue())
				.containsExactlyInAnyOrder(tuple(eventId, seeded.sessionId()), tuple(otherEventId, seeded.sessionId()));
	}

	@Test
	@DisplayName("markProcessed는 한 트랜잭션 안에서 같은 eventId를 두 번 받으면 두 번째에 false를 돌려주고, 그 트랜잭션은 깨지지 않아 이어서 쿼리 · 커밋할 수 있다")
	void markProcessedReturnsFalseForSameEventIdInSameTransactionWithoutBreakingIt() {
		String eventId = UUID.randomUUID().toString();

		List<Object> inTransaction = transactionTemplate.execute(tx -> {
			boolean first = processedVehicleEventPort.markProcessed(seeded.sessionId(), eventId);
			boolean duplicate = processedVehicleEventPort.markProcessed(seeded.sessionId(), eventId);
			Integer rows = jdbcTemplate.queryForObject(
					"select count(*) from processed_vehicle_event where event_id = ?", Integer.class, eventId);
			return List.<Object>of(first, duplicate, rows);
		});

		assertThat(inTransaction).as("처음 · 두 번째 결과와 그 뒤 같은 트랜잭션에서 센 행 수")
				.containsExactly(true, false, 1);
		assertThat(jdbcTemplate.queryForObject("select count(*) from processed_vehicle_event where event_id = ?",
				Integer.class, eventId)).as("커밋 뒤 행 수").isEqualTo(1);
	}

}
