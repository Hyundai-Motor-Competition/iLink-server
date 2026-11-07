package com.example.pickupservice.schedule.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class SchedulePersistenceEntitiesMappingTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String VARCHAR = "character varying";
	private static final String SMALLINT = "smallint";
	private static final String TIME = "time without time zone";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String USER_DEFINED = "USER-DEFINED";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("AcademyEntity · PickupScheduleEntity는 JPA 메타모델에 등록되고 academy · pickup_schedule 테이블이 생긴다")
	void entitiesAreManagedAndTablesExist() {
		var metamodel = entityManagerFactory.getMetamodel();
		assertThat(metamodel.getEntities())
				.<Class<?>>extracting(entityType -> entityType.getJavaType())
				.as("JPA 메타모델에 등록된 엔티티")
				.contains(AcademyEntity.class, PickupScheduleEntity.class);

		assertThat(columnsOf("academy")).as("academy 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("pickup_schedule")).as("pickup_schedule 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("academy_entity")).as("academy_entity 테이블은 만들지 않는다").isEmpty();
		assertThat(columnsOf("pickup_schedule_entity")).as("pickup_schedule_entity 테이블은 만들지 않는다").isEmpty();
	}

	@Test
	@DisplayName("academy 테이블은 academy_id · name VARCHAR(100) · address VARCHAR(255) · location · entrance NULL · phone VARCHAR(20) NULL과 created_at · updated_at 컬럼을 갖는다")
	void academyTableHasDesignColumns() {
		assertThat(columnsOf("academy")).containsAllEntriesOf(Map.ofEntries(
				Map.entry("academy_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("name", new ColumnType(VARCHAR, 100, NOT_NULL)),
				Map.entry("address", new ColumnType(VARCHAR, 255, NOT_NULL)),
				Map.entry("location", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("entrance", new ColumnType(USER_DEFINED, null, NULLABLE)),
				Map.entry("phone", new ColumnType(VARCHAR, 20, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("academy.external_place_id는 VARCHAR(100) NOT NULL 컬럼이다")
	void academyExternalPlaceIdIsNotNullVarchar100() {
		assertThat(columnsOf("academy"))
				.containsEntry("external_place_id", new ColumnType(VARCHAR, 100, NOT_NULL));
	}

	@Test
	@DisplayName("academy.external_place_id에는 그 컬럼 하나만 묶은 유니크 인덱스가 1개 있다")
	void academyExternalPlaceIdHasUniqueIndex() {
		List<String> indexDefinitions = jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = 'academy'
				""", String.class);

		assertThat(indexDefinitions)
				.as("academy 테이블의 인덱스 정의")
				.filteredOn(definition -> definition.startsWith("CREATE UNIQUE INDEX")
						&& definition.endsWith("(external_place_id)"))
				.hasSize(1);
	}

	@Test
	@DisplayName("academy.location · academy.entrance는 타입 Point · SRID 4326인 geography 컬럼으로 만들어진다")
	void academyLocationAndEntranceAreGeographyPoint4326() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select f_geography_column as column_name, type, srid
				from geography_columns
				where f_table_schema = current_schema()
				  and f_table_name = 'academy'
				""");

		Map<Object, Map<String, Object>> byColumn = columns.stream()
				.collect(Collectors.toMap(row -> row.get("column_name"), row -> row));
		assertThat(byColumn).containsOnlyKeys("location", "entrance");
		for (String columnName : List.of("location", "entrance")) {
			assertThat(byColumn.get(columnName))
					.as("academy.%s의 geography 타입 · SRID", columnName)
					.containsEntry("type", "Point")
					.containsEntry("srid", 4326);
		}
	}

	@Test
	@DisplayName("서버가 뜨면 academy.location에 GIST 인덱스가 만들어진다")
	void academyLocationHasGistIndex() {
		List<String> indexDefinitions = jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = 'academy'
				""", String.class);

		assertThat(indexDefinitions)
				.filteredOn(definition -> definition.contains("USING gist (location)"))
				.hasSize(1);
	}

	@Test
	@DisplayName("pickup_schedule 테이블은 schedule_id · child_user_id · academy_id · weekdays VARCHAR(30) · class_end_time TIME · target_pickup SMALLINT · status VARCHAR(10)과 created_at · updated_at 컬럼을 갖는다")
	void pickupScheduleTableHasDesignColumns() {
		assertThat(columnsOf("pickup_schedule")).containsAllEntriesOf(Map.ofEntries(
				Map.entry("schedule_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("child_user_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("academy_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("weekdays", new ColumnType(VARCHAR, 30, NOT_NULL)),
				Map.entry("class_end_time", new ColumnType(TIME, null, NOT_NULL)),
				Map.entry("target_pickup", new ColumnType(SMALLINT, null, NOT_NULL)),
				Map.entry("status", new ColumnType(VARCHAR, 10, NOT_NULL)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	private Map<String, ColumnType> columnsOf(String tableName) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select column_name, data_type, character_maximum_length, is_nullable
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = ?
				""", tableName);
		return rows.stream().collect(Collectors.toMap(
				row -> (String) row.get("column_name"),
				row -> new ColumnType(
						(String) row.get("data_type"),
						row.get("character_maximum_length") == null
								? null
								: ((Number) row.get("character_maximum_length")).intValue(),
						(String) row.get("is_nullable"))));
	}

	private record ColumnType(String dataType, Integer maxLength, String isNullable) {
	}

}
