package com.example.pickupservice.pickup.adapter.out.persistence;

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
class PickupPersistenceEntitiesMappingTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String INTEGER = "integer";
	private static final String SMALLINT = "smallint";
	private static final String NUMERIC = "numeric";
	private static final String VARCHAR = "character varying";
	private static final String BOOLEAN = "boolean";
	private static final String DATE = "date";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String JSONB = "jsonb";
	private static final String USER_DEFINED = "USER-DEFINED";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private static final List<String> PICKUP_TABLES = List.of(
			"pickup_session", "session_child", "session_event", "pickup_call",
			"pickup_outcome", "guardian_data_setting", "processed_vehicle_event");

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("픽업 엔티티 7개는 JPA 메타모델에 등록되고 pickup_session · session_child · session_event · pickup_call · pickup_outcome · guardian_data_setting · processed_vehicle_event 테이블이 생긴다")
	void entitiesAreManagedAndTablesExist() {
		var metamodel = entityManagerFactory.getMetamodel();
		assertThat(metamodel.getEntities())
				.<Class<?>>extracting(entityType -> entityType.getJavaType())
				.as("JPA 메타모델에 등록된 엔티티")
				.contains(PickupSessionEntity.class, SessionChildEntity.class, SessionEventEntity.class,
						CallEntity.class, PickupOutcomeEntity.class, GuardianDataSettingEntity.class,
						ProcessedVehicleEventEntity.class);

		for (String table : PICKUP_TABLES) {
			assertThat(columnsOf(table)).as("%s 테이블 컬럼", table).isNotEmpty();
		}
		assertThat(columnsOf("call")).as("Call 테이블은 SQL 예약어 call을 피해 pickup_call로 만든다").isEmpty();
		for (String table : List.of("pickup_session_entity", "session_child_entity", "session_event_entity",
				"call_entity", "pickup_outcome_entity", "guardian_data_setting_entity",
				"processed_vehicle_event_entity")) {
			assertThat(columnsOf(table)).as("%s 테이블은 만들지 않는다", table).isEmpty();
		}
	}

	@Test
	@DisplayName("pickup_session 테이블은 설계서 컬럼 27개(incident_child_user_id BIGINT NULL 포함,family_id BIGINT NOT NULL · spot_name VARCHAR(50) · date DATE · 상태 3개 VARCHAR(20) · 버전 3개 INT · call_held BOOLEAN, route_spot_version INT · ETA · 탑승 · incident 칸 NULL)와 created_at · updated_at만 갖는다")
	void pickupSessionTableHasDesignColumns() {
		assertThat(columnsOf("pickup_session")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("session_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("vehicle_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("family_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("spot_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("spot_name", new ColumnType(VARCHAR, 50, NOT_NULL)),
				Map.entry("stop_point", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("meeting_point", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("date", new ColumnType(DATE, null, NOT_NULL)),
				Map.entry("session_status", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("vehicle_phase", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("spot_status", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("state_version", new ColumnType(INTEGER, null, NOT_NULL)),
				Map.entry("spot_version", new ColumnType(INTEGER, null, NOT_NULL)),
				Map.entry("route_version", new ColumnType(INTEGER, null, NOT_NULL)),
				Map.entry("route_spot_version", new ColumnType(INTEGER, null, NULLABLE)),
				Map.entry("call_held", new ColumnType(BOOLEAN, null, NOT_NULL)),
				Map.entry("eta_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("remaining_meters", new ColumnType(INTEGER, null, NULLABLE)),
				Map.entry("eta_updated_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("boarding_detected_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("boarding_cancelable_until", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("boarding_door", new ColumnType(VARCHAR, 20, NULLABLE)),
				Map.entry("incident_type", new ColumnType(VARCHAR, 30, NULLABLE)),
				Map.entry("incident_cause", new ColumnType(VARCHAR, 100, NULLABLE)),
				Map.entry("incident_child_user_id", new ColumnType(BIGINT, null, NULLABLE)),
				Map.entry("incident_opened_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("incident_resolved_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("pickup_session.state_version · spot_version · route_version은 DB 기본값 0, call_held는 DB 기본값 false를 갖는다")
	void pickupSessionVersionsDefaultToZeroAndCallHeldDefaultsToFalse() {
		for (String column : List.of("state_version", "spot_version", "route_version")) {
			assertThat(normalizedDefaultOf("pickup_session", column))
					.as("pickup_session.%s의 DB 기본값", column)
					.isEqualTo("0");
		}
		assertThat(normalizedDefaultOf("pickup_session", "call_held"))
				.as("pickup_session.call_held의 DB 기본값")
				.isEqualTo("false");
	}

	@Test
	@DisplayName("pickup_session.stop_point · meeting_point는 타입 Point · SRID 4326인 geography 컬럼으로 만들어진다")
	void pickupSessionStopPointAndMeetingPointAreGeographyPoint4326() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select f_geography_column as column_name, type, srid
				from geography_columns
				where f_table_schema = current_schema()
				  and f_table_name = 'pickup_session'
				""");

		Map<Object, Map<String, Object>> byColumn = columns.stream()
				.collect(Collectors.toMap(row -> row.get("column_name"), row -> row));
		assertThat(byColumn).containsOnlyKeys("stop_point", "meeting_point");
		for (String columnName : List.of("stop_point", "meeting_point")) {
			assertThat(byColumn.get(columnName))
					.as("pickup_session.%s의 geography 타입 · SRID", columnName)
					.containsEntry("type", "Point")
					.containsEntry("srid", 4326);
		}
	}

	@Test
	@DisplayName("pickup_session에는 (vehicle_id, date) 순서의 유니크가 아닌 인덱스가 1개 있다")
	void pickupSessionHasVehicleIdDateIndex() {
		assertThat(indexDefinitionsOf("pickup_session"))
				.as("pickup_session 인덱스 정의")
				.filteredOn(definition -> !definition.startsWith("CREATE UNIQUE INDEX")
						&& (definition.endsWith("(vehicle_id, date)") || definition.endsWith("(vehicle_id, \"date\")")))
				.hasSize(1);
	}

	@Test
	@DisplayName("session_child 테이블은 session_child_id · session_id · child_user_id · schedule_id · child_phase VARCHAR(20) · extra_prep_minutes SMALLINT · call_not_before NULL · last_lat · last_lng NUMERIC NULL · last_located_at · acknowledged_at · boarded_at NULL과 created_at · updated_at만 갖는다")
	void sessionChildTableHasDesignColumns() {
		assertThat(columnsOf("session_child")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("session_child_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("session_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("child_user_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("schedule_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("child_phase", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("extra_prep_minutes", new ColumnType(SMALLINT, null, NOT_NULL)),
				Map.entry("call_not_before", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("last_lat", new ColumnType(NUMERIC, null, NULLABLE)),
				Map.entry("last_lng", new ColumnType(NUMERIC, null, NULLABLE)),
				Map.entry("last_located_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("acknowledged_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("boarded_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("session_child.last_lat · last_lng는 NUMERIC(9,6)이다")
	void sessionChildLastLatLngAreNumeric9Scale6() {
		for (String column : List.of("last_lat", "last_lng")) {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
					select numeric_precision, numeric_scale
					from information_schema.columns
					where table_schema = current_schema()
					  and table_name = 'session_child'
					  and column_name = ?
					""", column);
			assertThat(rows).as("session_child.%s 컬럼", column).hasSize(1);
			assertThat(((Number) rows.getFirst().get("numeric_precision")).intValue())
					.as("session_child.%s의 정밀도", column)
					.isEqualTo(9);
			assertThat(((Number) rows.getFirst().get("numeric_scale")).intValue())
					.as("session_child.%s의 소수 자리", column)
					.isEqualTo(6);
		}
	}

	@Test
	@DisplayName("session_child.extra_prep_minutes는 DB 기본값 0을 갖는다")
	void sessionChildExtraPrepMinutesDefaultsToZero() {
		assertThat(normalizedDefaultOf("session_child", "extra_prep_minutes"))
				.as("session_child.extra_prep_minutes의 DB 기본값")
				.isEqualTo("0");
	}

	@Test
	@DisplayName("session_child에는 (session_id, child_user_id) 유니크 인덱스가 1개 있다")
	void sessionChildHasUniqueSessionIdChildUserIdIndex() {
		assertThat(indexDefinitionsOf("session_child"))
				.as("session_child 인덱스 정의")
				.filteredOn(definition -> definition.startsWith("CREATE UNIQUE INDEX")
						&& definition.endsWith("(session_id, child_user_id)"))
				.hasSize(1);
	}

	@Test
	@DisplayName("session_event 테이블은 event_id · session_id · seq BIGINT · type VARCHAR(40) · state_version INT · occurred_at · payload JSONB NULL과 created_at · updated_at만 갖는다")
	void sessionEventTableHasDesignColumns() {
		assertThat(columnsOf("session_event")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("event_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("session_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("seq", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("type", new ColumnType(VARCHAR, 40, NOT_NULL)),
				Map.entry("state_version", new ColumnType(INTEGER, null, NOT_NULL)),
				Map.entry("occurred_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("payload", new ColumnType(JSONB, null, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("session_event에는 (session_id, seq) 유니크 인덱스가 1개 있다")
	void sessionEventHasUniqueSessionIdSeqIndex() {
		assertThat(indexDefinitionsOf("session_event"))
				.as("session_event 인덱스 정의")
				.filteredOn(definition -> definition.startsWith("CREATE UNIQUE INDEX")
						&& definition.endsWith("(session_id, seq)"))
				.hasSize(1);
	}

	@Test
	@DisplayName("pickup_call 테이블은 call_id · session_id · prev_call_id NULL · trigger_type · status VARCHAR(20) · spot_version INT · sent_at · delivered_at · acknowledged_at · revoked_at NULL · revoke_reason VARCHAR(100) NULL과 created_at · updated_at만 갖는다")
	void pickupCallTableHasDesignColumns() {
		assertThat(columnsOf("pickup_call")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("call_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("session_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("prev_call_id", new ColumnType(BIGINT, null, NULLABLE)),
				Map.entry("trigger_type", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("status", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("spot_version", new ColumnType(INTEGER, null, NOT_NULL)),
				Map.entry("sent_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("delivered_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("acknowledged_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("revoked_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("revoke_reason", new ColumnType(VARCHAR, 100, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("pickup_outcome 테이블은 session_id · completed_at · boarding_method VARCHAR(20) · 예측 · 실제 · 정정 정차 시각 NULL · correction_reason VARCHAR(30) · correction_status VARCHAR(20) · corrected_by · correction_note VARCHAR(200) NULL과 created_at · updated_at만 갖는다")
	void pickupOutcomeTableHasDesignColumns() {
		assertThat(columnsOf("pickup_outcome")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("session_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("completed_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("boarding_method", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("predicted_stop_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("actual_stop_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("corrected_stop_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("correction_reason", new ColumnType(VARCHAR, 30, NULLABLE)),
				Map.entry("correction_status", new ColumnType(VARCHAR, 20, NULLABLE)),
				Map.entry("corrected_by", new ColumnType(BIGINT, null, NULLABLE)),
				Map.entry("correction_note", new ColumnType(VARCHAR, 200, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("guardian_data_setting 테이블은 user_id · learning_consent BOOLEAN과 created_at · updated_at만 갖고 learning_consent의 DB 기본값은 false다")
	void guardianDataSettingTableHasDesignColumns() {
		assertThat(columnsOf("guardian_data_setting")).containsExactlyInAnyOrderEntriesOf(Map.of(
				"user_id", new ColumnType(BIGINT, null, NOT_NULL),
				"learning_consent", new ColumnType(BOOLEAN, null, NOT_NULL),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
		assertThat(normalizedDefaultOf("guardian_data_setting", "learning_consent"))
				.as("guardian_data_setting.learning_consent의 DB 기본값")
				.isEqualTo("false");
	}

	@Test
	@DisplayName("processed_vehicle_event 테이블은 event_id VARCHAR(36) · session_id BIGINT와 created_at · updated_at만 갖는다")
	void processedVehicleEventTableHasDesignColumns() {
		assertThat(columnsOf("processed_vehicle_event")).containsExactlyInAnyOrderEntriesOf(Map.of(
				"event_id", new ColumnType(VARCHAR, 36, NOT_NULL),
				"session_id", new ColumnType(BIGINT, null, NOT_NULL),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
	}

	@Test
	@DisplayName("PK는 pickup_session.session_id · session_child.session_child_id · session_event.event_id · pickup_call.call_id가 IDENTITY이고 pickup_outcome.session_id · guardian_data_setting.user_id · processed_vehicle_event.event_id는 IDENTITY가 아니다")
	void primaryKeysMatchDesign() {
		Map<String, String> identityPrimaryKeys = Map.of(
				"pickup_session", "session_id",
				"session_child", "session_child_id",
				"session_event", "event_id",
				"pickup_call", "call_id");
		Map<String, String> assignedPrimaryKeys = Map.of(
				"pickup_outcome", "session_id",
				"guardian_data_setting", "user_id",
				"processed_vehicle_event", "event_id");

		identityPrimaryKeys.forEach((table, column) -> {
			assertThat(primaryKeyColumnsOf(table)).as("%s의 PK 컬럼", table).containsExactly(column);
			assertThat(isIdentity(table, column)).as("%s.%s는 IDENTITY여야 한다", table, column).isEqualTo("YES");
		});
		assignedPrimaryKeys.forEach((table, column) -> {
			assertThat(primaryKeyColumnsOf(table)).as("%s의 PK 컬럼", table).containsExactly(column);
			assertThat(isIdentity(table, column)).as("%s.%s는 IDENTITY가 아니어야 한다", table, column).isEqualTo("NO");
		});
	}

	@Test
	@DisplayName("픽업 테이블 7개에는 DB 외래 키 제약을 걸지 않는다")
	void pickupTablesHaveNoForeignKeyConstraints() {
		List<String> foreignKeys = jdbcTemplate.queryForList("""
				select table_name || '.' || constraint_name
				from information_schema.table_constraints
				where table_schema = current_schema()
				  and constraint_type = 'FOREIGN KEY'
				  and table_name in (?, ?, ?, ?, ?, ?, ?)
				""", String.class, PICKUP_TABLES.toArray());

		assertThat(PICKUP_TABLES)
				.as("픽업 테이블이 모두 만들어져 있어야 한다")
				.allSatisfy(table -> assertThat(columnsOf(table)).as("%s 테이블 컬럼", table).isNotEmpty());
		assertThat(foreignKeys).as("픽업 테이블의 외래 키 제약").isEmpty();
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

	private String normalizedDefaultOf(String tableName, String columnName) {
		List<String> defaults = jdbcTemplate.queryForList("""
				select column_default
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = ?
				  and column_name = ?
				""", String.class, tableName, columnName);
		assertThat(defaults).as("%s.%s 컬럼", tableName, columnName).hasSize(1);
		String columnDefault = defaults.getFirst();
		assertThat(columnDefault).as("%s.%s의 DB 기본값", tableName, columnName).isNotNull();
		return columnDefault
				.replace("::smallint", "")
				.replace("::integer", "")
				.replace("::bigint", "")
				.replace("::boolean", "")
				.replace("(", "")
				.replace(")", "")
				.replace("'", "")
				.trim();
	}

	private List<String> indexDefinitionsOf(String tableName) {
		return jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = ?
				""", String.class, tableName);
	}

	private List<String> primaryKeyColumnsOf(String tableName) {
		return jdbcTemplate.queryForList("""
				select kcu.column_name
				from information_schema.table_constraints tc
				join information_schema.key_column_usage kcu
				  on kcu.constraint_schema = tc.constraint_schema
				 and kcu.constraint_name = tc.constraint_name
				 and kcu.table_name = tc.table_name
				where tc.table_schema = current_schema()
				  and tc.table_name = ?
				  and tc.constraint_type = 'PRIMARY KEY'
				order by kcu.ordinal_position
				""", String.class, tableName);
	}

	private String isIdentity(String tableName, String columnName) {
		List<String> rows = jdbcTemplate.queryForList("""
				select is_identity
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = ?
				  and column_name = ?
				""", String.class, tableName, columnName);
		assertThat(rows).as("%s.%s 컬럼", tableName, columnName).hasSize(1);
		return rows.getFirst();
	}

	private record ColumnType(String dataType, Integer maxLength, String isNullable) {
	}

}
