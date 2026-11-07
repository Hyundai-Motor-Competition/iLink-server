package com.example.pickupservice.user.adapter.out.persistence;

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
class UserPersistenceEntitiesMappingTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String VARCHAR = "character varying";
	private static final String CHAR = "character";
	private static final String INTEGER = "integer";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("FamilyEntity · UserEntity · VehicleEntity는 JPA 메타모델에 등록되고 UserEntity의 실제 테이블은 users다")
	void entitiesAreManagedAndUserEntityTableIsUsers() {
		var metamodel = entityManagerFactory.getMetamodel();
		assertThat(metamodel.entity(FamilyEntity.class)).isNotNull();
		assertThat(metamodel.entity(UserEntity.class)).isNotNull();
		assertThat(metamodel.entity(VehicleEntity.class)).isNotNull();

		assertThat(columnsOf("users")).as("users 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("user")).as("user 테이블은 만들지 않는다").isEmpty();
		assertThat(columnsOf("user_entity")).as("user_entity 테이블은 만들지 않는다").isEmpty();
	}

	@Test
	@DisplayName("family 테이블은 family_id · join_call_mode VARCHAR(20) · settings_version INT와 created_at · updated_at 컬럼을 갖는다")
	void familyTableHasDesignColumns() {
		assertThat(columnsOf("family")).containsAllEntriesOf(Map.of(
				"family_id", new ColumnType(BIGINT, null, NOT_NULL),
				"join_call_mode", new ColumnType(VARCHAR, 20, NOT_NULL),
				"settings_version", new ColumnType(INTEGER, null, NOT_NULL),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
	}

	@Test
	@DisplayName("users 테이블은 user_id · family_id · role VARCHAR(20) · name VARCHAR(20) · phone VARCHAR(255) NULL과 created_at · updated_at 컬럼을 갖는다")
	void usersTableHasDesignColumns() {
		assertThat(columnsOf("users")).containsAllEntriesOf(Map.of(
				"user_id", new ColumnType(BIGINT, null, NOT_NULL),
				"family_id", new ColumnType(BIGINT, null, NOT_NULL),
				"role", new ColumnType(VARCHAR, 20, NOT_NULL),
				"name", new ColumnType(VARCHAR, 20, NOT_NULL),
				"phone", new ColumnType(VARCHAR, 255, NULLABLE),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
	}

	@Test
	@DisplayName("vehicle 테이블은 vehicle_id · family_id NULL · vin CHAR(17) · model VARCHAR(50) · plate_number VARCHAR(20) · unlinked_at TIMESTAMPTZ NULL과 created_at · updated_at 컬럼을 갖는다")
	void vehicleTableHasDesignColumns() {
		Map<String, ColumnType> columns = columnsOf("vehicle");

		assertThat(columns).containsAllEntriesOf(Map.of(
				"vehicle_id", new ColumnType(BIGINT, null, NOT_NULL),
				"family_id", new ColumnType(BIGINT, null, NULLABLE),
				"vin", new ColumnType(CHAR, 17, NOT_NULL),
				"unlinked_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
		assertThat(columns.get("model")).isNotNull()
				.extracting(ColumnType::dataType, ColumnType::maxLength)
				.containsExactly(VARCHAR, 50);
		assertThat(columns.get("plate_number")).isNotNull()
				.extracting(ColumnType::dataType, ColumnType::maxLength)
				.containsExactly(VARCHAR, 20);
	}

	@Test
	@DisplayName("vehicle.version 컬럼은 BIGINT NOT NULL이고 DB 기본값 0을 가져 행이 있는 테이블에도 추가할 수 있다")
	void vehicleVersionColumnIsNotNullWithDefaultZero() {
		Map<String, Object> version = jdbcTemplate.queryForMap("""
				select data_type, is_nullable, column_default
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = 'vehicle'
				  and column_name = 'version'
				""");

		assertThat(version.get("data_type")).isEqualTo(BIGINT);
		assertThat(version.get("is_nullable")).isEqualTo(NOT_NULL);
		assertThat(version.get("column_default")).as("vehicle.version의 DB 기본값").isNotNull();
		assertThat(normalizeDefault((String) version.get("column_default"))).isEqualTo("0");
	}

	private static String normalizeDefault(String columnDefault) {
		return columnDefault.replace("::bigint", "").replace("(", "").replace(")", "").replace("'", "").trim();
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
