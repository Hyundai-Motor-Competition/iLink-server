package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.place.fixture.DefaultSpotIndexes;
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
class PlacePersistenceEntitiesMappingTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String VARCHAR = "character varying";
	private static final String BOOLEAN = "boolean";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String USER_DEFINED = "USER-DEFINED";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("PickupSpotEntity는 JPA 메타모델에 등록되고 pickup_spot 테이블이 생긴다")
	void pickupSpotEntityIsManagedAndTableExists() {
		var metamodel = entityManagerFactory.getMetamodel();
		assertThat(metamodel.getEntities())
				.<Class<?>>extracting(entityType -> entityType.getJavaType())
				.as("JPA 메타모델에 등록된 엔티티")
				.contains(PickupSpotEntity.class);

		assertThat(columnsOf("pickup_spot")).as("pickup_spot 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("pickup_spot_entity")).as("pickup_spot_entity 테이블은 만들지 않는다").isEmpty();
	}

	@Test
	@DisplayName("pickup_spot 테이블은 spot_id · family_id · academy_id · name VARCHAR(50) · memo VARCHAR(200) NULL · stop_point · meeting_point · is_default · created_by와 created_at · updated_at 컬럼만 갖는다")
	void pickupSpotTableHasDesignColumns() {
		assertThat(columnsOf("pickup_spot")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("spot_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("family_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("academy_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("name", new ColumnType(VARCHAR, 50, NOT_NULL)),
				Map.entry("memo", new ColumnType(VARCHAR, 200, NULLABLE)),
				Map.entry("stop_point", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("meeting_point", new ColumnType(USER_DEFINED, null, NOT_NULL)),
				Map.entry("is_default", new ColumnType(BOOLEAN, null, NOT_NULL)),
				Map.entry("created_by", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("isDefault 필드는 default · is_is_default가 아니라 is_default 컬럼 하나로만 매핑된다")
	void isDefaultFieldMapsToIsDefaultColumn() {
		Map<String, ColumnType> columns = columnsOf("pickup_spot");

		assertThat(columns).containsKey("is_default");
		assertThat(columns).doesNotContainKeys("default", "is_is_default", "isdefault");
	}

	@Test
	@DisplayName("pickup_spot.stop_point · pickup_spot.meeting_point는 타입 Point · SRID 4326인 geography 컬럼으로 만들어진다")
	void stopPointAndMeetingPointAreGeographyPoint4326() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
				select f_geography_column as column_name, type, srid
				from geography_columns
				where f_table_schema = current_schema()
				  and f_table_name = 'pickup_spot'
				""");

		Map<Object, Map<String, Object>> byColumn = columns.stream()
				.collect(Collectors.toMap(row -> row.get("column_name"), row -> row));
		assertThat(byColumn).containsOnlyKeys("stop_point", "meeting_point");
		for (String columnName : List.of("stop_point", "meeting_point")) {
			assertThat(byColumn.get(columnName))
					.as("pickup_spot.%s의 geography 타입 · SRID", columnName)
					.containsEntry("type", "Point")
					.containsEntry("srid", 4326);
		}
	}

	@Test
	@DisplayName("서버가 뜨면 pickup_spot.stop_point에 GIST 인덱스가 만들어진다")
	void stopPointHasGistIndex() {
		List<String> indexDefinitions = jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = 'pickup_spot'
				""", String.class);

		assertThat(indexDefinitions)
				.filteredOn(definition -> definition.contains("USING gist (stop_point)"))
				.hasSize(1);
	}

	@Test
	@DisplayName("서버가 뜨면 pickup_spot에 (family_id, academy_id) WHERE is_default 부분 유니크 인덱스가 만들어진다")
	void pickupSpotHasPartialUniqueIndexOnFamilyAndAcademyWhereIsDefault() {
		assertThat(DefaultSpotIndexes.partialUniqueOnFamilyAcademy(jdbcTemplate))
				.as("pickup_spot 인덱스 정의")
				.hasSize(1);
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
