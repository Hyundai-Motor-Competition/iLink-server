package com.example.pickupservice.notification.adapter.out.persistence;

import com.example.pickupservice.common.fixture.IntegrationTestSupport;
import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class NotificationEntityTest extends IntegrationTestSupport {

	private static final String BIGINT = "bigint";
	private static final String VARCHAR = "character varying";
	private static final String TIMESTAMPTZ = "timestamp with time zone";
	private static final String NULLABLE = "YES";
	private static final String NOT_NULL = "NO";

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("NotificationEntity가 BaseTimeEntity를 바로 상속하고 @Entity · @Table(name = \"notification\")로 선언된다")
	void entityExtendsBaseTimeEntityAndDeclaresTable() {
		assertThat(NotificationEntity.class.isAnnotationPresent(Entity.class))
				.as("NotificationEntity에 @Entity가 있어야 한다")
				.isTrue();
		assertThat(NotificationEntity.class.getSuperclass())
				.as("NotificationEntity는 BaseTimeEntity를 바로 상속해야 한다")
				.isEqualTo(BaseTimeEntity.class);
		Table table = NotificationEntity.class.getAnnotation(Table.class);
		assertThat(table).as("NotificationEntity에 @Table이 있어야 한다").isNotNull();
		assertThat(table.name()).as("테이블 이름").isEqualTo("notification");
	}

	@Test
	@DisplayName("NotificationEntity의 PK는 Long notificationId 필드에 @Id와 @GeneratedValue(IDENTITY)로 만들고 JPA 메타모델에 등록된다")
	void entityUsesIdentityPrimaryKeyAndIsManaged() {
		List<Field> idFields = Arrays.stream(NotificationEntity.class.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Id.class))
				.toList();
		assertThat(idFields).as("@Id 필드").hasSize(1);
		Field idField = idFields.getFirst();
		assertThat(idField.getName()).isEqualTo("notificationId");
		assertThat(idField.getType()).isEqualTo(Long.class);
		GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
		assertThat(generatedValue).as("@GeneratedValue").isNotNull();
		assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);

		assertThat(entityManagerFactory.getMetamodel().getEntities())
				.<Class<?>>extracting(entityType -> entityType.getJavaType())
				.as("JPA 메타모델에 등록된 엔티티")
				.contains(NotificationEntity.class);
	}

	@Test
	@DisplayName("notification 테이블은 notification_id · session_id · device_id BIGINT, call_id BIGINT NULL, type VARCHAR(30), sent_at · delivered_at · opened_at TIMESTAMPTZ NULL과 created_at · updated_at만 갖는다")
	void notificationTableHasDesignColumns() {
		assertThat(columnsOf("notification")).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
				Map.entry("notification_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("session_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("call_id", new ColumnType(BIGINT, null, NULLABLE)),
				Map.entry("type", new ColumnType(VARCHAR, 30, NOT_NULL)),
				Map.entry("device_id", new ColumnType(BIGINT, null, NOT_NULL)),
				Map.entry("sent_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("delivered_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("opened_at", new ColumnType(TIMESTAMPTZ, null, NULLABLE)),
				Map.entry("created_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL)),
				Map.entry("updated_at", new ColumnType(TIMESTAMPTZ, null, NOT_NULL))));
		assertThat(columnsOf("notification_entity")).as("notification_entity 테이블은 만들지 않는다").isEmpty();
	}

	@Test
	@DisplayName("notification.notification_id는 PK이고 IDENTITY이며 notification에는 DB 외래 키 제약을 걸지 않는다")
	void notificationPrimaryKeyIsIdentityAndHasNoForeignKeys() {
		assertThat(primaryKeyColumnsOf("notification")).as("notification의 PK 컬럼").containsExactly("notification_id");
		List<String> identity = jdbcTemplate.queryForList("""
				select is_identity
				from information_schema.columns
				where table_schema = current_schema()
				  and table_name = 'notification'
				  and column_name = 'notification_id'
				""", String.class);
		assertThat(identity).as("notification.notification_id 컬럼").containsExactly("YES");

		List<String> foreignKeys = jdbcTemplate.queryForList("""
				select constraint_name
				from information_schema.table_constraints
				where table_schema = current_schema()
				  and constraint_type = 'FOREIGN KEY'
				  and table_name = 'notification'
				""", String.class);
		assertThat(columnsOf("notification")).as("notification 테이블 컬럼").isNotEmpty();
		assertThat(foreignKeys).as("notification의 외래 키 제약").isEmpty();
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

	private record ColumnType(String dataType, Integer maxLength, String isNullable) {
	}

}
