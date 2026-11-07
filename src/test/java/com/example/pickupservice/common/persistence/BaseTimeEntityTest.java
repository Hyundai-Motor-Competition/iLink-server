package com.example.pickupservice.common.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class BaseTimeEntityTest extends IntegrationTestSupport {

	private final BaseTimeSampleRepository baseTimeSampleRepository;
	private final JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		baseTimeSampleRepository.deleteAll();
	}

	@Test
	@DisplayName("BaseTimeEntity를 상속한 엔티티를 저장하면 DB에서 다시 읽은 createdAt과 updatedAt이 저장 시각으로 채워져 있다")
	void savingEntityFillsCreatedAtAndUpdatedAt() {
		Instant before = Instant.now();

		BaseTimeSampleEntity saved = baseTimeSampleRepository.save(new BaseTimeSampleEntity("sample"));

		Instant after = Instant.now();
		BaseTimeSampleEntity found = baseTimeSampleRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getCreatedAt())
				.isNotNull()
				.isBetween(before.minusSeconds(1), after.plusSeconds(1));
		assertThat(found.getUpdatedAt())
				.isNotNull()
				.isBetween(before.minusSeconds(1), after.plusSeconds(1));
	}

	@Test
	@DisplayName("BaseTimeEntity를 상속한 엔티티를 수정해 저장하면 DB에서 다시 읽은 updatedAt만 늦은 시각으로 바뀌고 createdAt은 그대로다")
	void updatingEntityChangesOnlyUpdatedAt() throws InterruptedException {
		Long id = baseTimeSampleRepository.save(new BaseTimeSampleEntity("before")).getId();
		BaseTimeSampleEntity original = baseTimeSampleRepository.findById(id).orElseThrow();
		Instant originalCreatedAt = original.getCreatedAt();
		Instant originalUpdatedAt = original.getUpdatedAt();
		waitUntilClockPasses(originalUpdatedAt.plusMillis(10));

		original.rename("after");
		baseTimeSampleRepository.save(original);

		BaseTimeSampleEntity updated = baseTimeSampleRepository.findById(id).orElseThrow();
		assertThat(updated.getName()).isEqualTo("after");
		assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
		assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
		assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
	}

	private static void waitUntilClockPasses(Instant target) throws InterruptedException {
		while (!Instant.now().isAfter(target)) {
			Thread.sleep(5);
		}
	}

	@Test
	@DisplayName("BaseTimeEntity의 created_at과 updated_at 컬럼은 TIMESTAMPTZ로 만들어진다")
	void createdAtAndUpdatedAtColumnsAreTimestamptz() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select column_name, data_type
				from information_schema.columns
				where table_name = 'base_time_sample'
				  and column_name in ('created_at', 'updated_at')
				""");

		Map<Object, Object> dataTypeByColumn = columns.stream()
				.collect(Collectors.toMap(row -> row.get("column_name"), row -> row.get("data_type")));
		assertThat(dataTypeByColumn)
				.containsEntry("created_at", "timestamp with time zone")
				.containsEntry("updated_at", "timestamp with time zone");
	}

}
