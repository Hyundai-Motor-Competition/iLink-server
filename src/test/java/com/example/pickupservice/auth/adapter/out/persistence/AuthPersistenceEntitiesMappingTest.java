package com.example.pickupservice.auth.adapter.out.persistence;

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
class AuthPersistenceEntitiesMappingTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String VARCHAR = "character varying";
	private static final String CHAR = "character";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String BOOLEAN = "boolean";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("VehicleLinkCodeEntity · MemberInviteEntity · DeviceEntity는 JPA 메타모델에 등록되고 vehicle_link_code · member_invite · device 테이블이 생긴다")
	void entitiesAreManagedAndTablesExist() {
		var metamodel = entityManagerFactory.getMetamodel();
		assertThat(metamodel.entity(VehicleLinkCodeEntity.class)).isNotNull();
		assertThat(metamodel.entity(MemberInviteEntity.class)).isNotNull();
		assertThat(metamodel.entity(DeviceEntity.class)).isNotNull();

		assertThat(columnsOf("vehicle_link_code")).as("vehicle_link_code 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("member_invite")).as("member_invite 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("device")).as("device 테이블 컬럼").isNotEmpty();
		assertThat(columnsOf("vehicle_link_code_entity")).as("vehicle_link_code_entity 테이블은 만들지 않는다").isEmpty();
		assertThat(columnsOf("member_invite_entity")).as("member_invite_entity 테이블은 만들지 않는다").isEmpty();
		assertThat(columnsOf("device_entity")).as("device_entity 테이블은 만들지 않는다").isEmpty();
	}

	@Test
	@DisplayName("vehicle_link_code 테이블은 code_id · vehicle_id · issued_device_id NULL · issued_before_link BOOLEAN · code CHAR(8) · status VARCHAR(20) · expires_at · used_at NULL · mode VARCHAR(10) NULL · token_delivered_at NULL과 created_at · updated_at 컬럼을 갖는다")
	void vehicleLinkCodeTableHasDesignColumns() {
		assertThat(columnsOf("vehicle_link_code")).containsAllEntriesOf(Map.ofEntries(
				Map.entry("code_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("vehicle_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("issued_device_id", new ColumnType(BIGINT, null, NULLABLE)),
				Map.entry("issued_before_link", new ColumnType(BOOLEAN, null, NOT_NULL)),
				Map.entry("code", new ColumnType(CHAR, 8, NOT_NULL)),
				Map.entry("status", new ColumnType(VARCHAR, 20, NOT_NULL)),
				Map.entry("expires_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("used_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("mode", new ColumnType(VARCHAR, 10, NULLABLE)),
				Map.entry("token_delivered_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
	}

	@Test
	@DisplayName("vehicle_link_code.issued_before_link는 기본값이 false라 그 컬럼 없이 넣은 기존 방식의 행도 false로 읽힌다")
	void vehicleLinkCodeIssuedBeforeLinkDefaultsToFalse() {
		assertThat(jdbcTemplate.queryForObject("""
				select column_default
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = 'vehicle_link_code'
				  and column_name = 'issued_before_link'
				""", String.class)).as("issued_before_link 기본값").isEqualTo("false");

		Long codeId = jdbcTemplate.queryForObject("""
				insert into vehicle_link_code (vehicle_id, code, status, expires_at, created_at, updated_at)
				values (?, 'LEGACY01', 'ISSUED', now(), now(), now())
				returning code_id
				""", Long.class, 9_876_543_210L);
		try {
			assertThat(jdbcTemplate.queryForObject(
					"select issued_before_link from vehicle_link_code where code_id = ?", Boolean.class, codeId))
					.as("issued_before_link 없이 넣은 행").isFalse();
		} finally {
			jdbcTemplate.update("delete from vehicle_link_code where code_id = ?", codeId);
		}
	}

	@Test
	@DisplayName("vehicle_link_code 테이블은 code 컬럼 하나로 된 인덱스를 갖는다")
	void vehicleLinkCodeTableHasCodeIndex() {
		assertThat(indexDefinitionsOf("vehicle_link_code"))
				.as("vehicle_link_code 인덱스 정의")
				.anySatisfy(definition -> assertThat(definition).endsWith("(code)"));
	}

	@Test
	@DisplayName("member_invite 테이블은 invite_id · user_id · token VARCHAR(64) · expires_at · used_at NULL과 created_at · updated_at 컬럼을 갖는다")
	void memberInviteTableHasDesignColumns() {
		assertThat(columnsOf("member_invite")).containsAllEntriesOf(Map.of(
				"invite_id", new ColumnType(BIGINT, null, NOT_NULL),
				"user_id", new ColumnType(BIGINT, null, NOT_NULL),
				"token", new ColumnType(VARCHAR, 64, NOT_NULL),
				"expires_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"used_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
	}

	@Test
	@DisplayName("member_invite 테이블은 token 컬럼에 유니크 인덱스를 갖는다")
	void memberInviteTableHasUniqueTokenIndex() {
		assertThat(indexDefinitionsOf("member_invite"))
				.as("member_invite 인덱스 정의")
				.anySatisfy(definition -> assertThat(definition)
						.startsWith("CREATE UNIQUE INDEX")
						.endsWith("(token)"));
	}

	@Test
	@DisplayName("device 테이블은 device_id · owner_type VARCHAR(10) · owner_id · platform VARCHAR(10) · push_token VARCHAR(255) NULL · push_updated_at NULL · refresh_token_hash CHAR(64) · revoked_at NULL과 created_at · updated_at 컬럼을 갖는다")
	void deviceTableHasDesignColumns() {
		assertThat(columnsOf("device")).containsAllEntriesOf(Map.of(
				"device_id", new ColumnType(BIGINT, null, NOT_NULL),
				"owner_type", new ColumnType(VARCHAR, 10, NOT_NULL),
				"owner_id", new ColumnType(BIGINT, null, NOT_NULL),
				"platform", new ColumnType(VARCHAR, 10, NOT_NULL),
				"push_token", new ColumnType(VARCHAR, 255, NULLABLE),
				"push_updated_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE),
				"refresh_token_hash", new ColumnType(CHAR, 64, NOT_NULL),
				"revoked_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE),
				"created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL),
				"updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)));
	}

	@Test
	@DisplayName("device 테이블은 값이 없는 옛 행을 위해 NULL을 허용하는 refresh_token_expires_at TIMESTAMPTZ 컬럼을 갖는다")
	void deviceTableHasNullableRefreshTokenExpiresAtColumn() {
		assertThat(columnsOf("device"))
				.containsEntry("refresh_token_expires_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE));
	}

	@Test
	@DisplayName("device 테이블은 (owner_type, owner_id) 순서의 인덱스를 갖는다")
	void deviceTableHasOwnerIndex() {
		assertThat(indexDefinitionsOf("device"))
				.as("device 인덱스 정의")
				.anySatisfy(definition -> assertThat(definition).endsWith("(owner_type, owner_id)"));
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

	private List<String> indexDefinitionsOf(String tableName) {
		return jdbcTemplate.queryForList("""
				select indexdef
				from pg_indexes
				where schemaname = current_schema()
				  and tablename = ?
				""", String.class, tableName);
	}

	private record ColumnType(String dataType, Integer maxLength, String isNullable) {
	}

}
