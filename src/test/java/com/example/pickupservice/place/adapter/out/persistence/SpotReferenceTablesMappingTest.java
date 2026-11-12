package com.example.pickupservice.place.adapter.out.persistence;

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
class SpotReferenceTablesMappingTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String INTEGER = "integer";
	private static final String VARCHAR = "character varying";
	private static final String BOOLEAN = "boolean";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String USER_DEFINED = "USER-DEFINED";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("PublicParkingEntity · CurbStopCandidateEntity는 JPA 메타모델에 등록된다")
	void referenceEntitiesAreManaged() {
		assertThat(entityManagerFactory.getMetamodel().getEntities())
				.<Class<?>>extracting(entityType -> entityType.getJavaType())
				.contains(PublicParkingEntity.class, CurbStopCandidateEntity.class);
	}

	@Test
	@DisplayName("public_parking 테이블은 parking_id VARCHAR(20) · name VARCHAR(100) · location · spaces · base_minutes · base_fee_won(NULL 가능)과 created_at · updated_at 컬럼만 갖는다")
	void publicParkingTableHasDesignColumns() {
		assertThat(columnsOf("public_parking")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("parking_id", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("name", new ColumnType(VARCHAR, 100, NOT_NULL)),
				Map.entry("location", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("spaces", new ColumnType(INTEGER, null, NULLABLE)),
				Map.entry("base_minutes", new ColumnType(INTEGER, null, NULLABLE)),
				Map.entry("base_fee_won", new ColumnType(INTEGER, null, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("curb_stop_candidate 테이블은 candidate_id BIGINT · location · road_name VARCHAR(100) NULL · line_kind VARCHAR(20) · near_enforcement_cctv BOOLEAN과 created_at · updated_at 컬럼만 갖는다")
	void curbStopCandidateTableHasDesignColumns() {
		assertThat(columnsOf("curb_stop_candidate")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("candidate_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("location", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("road_name", new ColumnType(VARCHAR, 100, NULLABLE)),
				Map.entry("line_kind", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("near_enforcement_cctv", new ColumnType(BOOLEAN, null, NOT_NULL)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("curb_stop_candidate.candidate_id는 자동 생성(IDENTITY) 컬럼이 아니다")
	void candidateIdIsNotIdentity() {
		List<String> identity = jdbcTemplate.queryForList("""
				select is_identity
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = 'curb_stop_candidate'
				  and column_name = 'candidate_id'
				""", String.class);
		assertThat(identity).containsExactly("NO");
	}

	@Test
	@DisplayName("public_parking.location · curb_stop_candidate.location은 타입 Point · SRID 4326인 geography 컬럼이다")
	void locationsAreGeographyPoint4326() {
		for (String table : List.of("public_parking", "curb_stop_candidate")) {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
					select f_geography_column as column_name, type, srid
					from geography_columns
					where f_table_schema = current_schema()
					  and f_table_name = ?
					""", table);
			assertThat(rows).as("%s의 geography 컬럼", table).hasSize(1);
			assertThat(rows.getFirst())
					.containsEntry("column_name", "location")
					.containsEntry("type", "Point")
					.containsEntry("srid", 4326);
		}
	}

	@Test
	@DisplayName("서버가 뜨면 public_parking.location · curb_stop_candidate.location에 GIST 인덱스가 만들어진다")
	void locationsHaveGistIndex() {
		for (String table : List.of("public_parking", "curb_stop_candidate")) {
			List<String> indexDefinitions = jdbcTemplate.queryForList("""
					select indexdef
					from pg_indexes
					where schemaname = current_schema()
					  and tablename = ?
					""", String.class, table);
			assertThat(indexDefinitions)
					.as("%s 인덱스 정의", table)
					.filteredOn(definition -> definition.contains("USING gist (location)"))
					.hasSize(1);
		}
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
