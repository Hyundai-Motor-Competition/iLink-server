package com.example.pickupservice.auth.adapter.out.persistence;

import com.example.pickupservice.auth.domain.LinkCodeStatus;
import com.example.pickupservice.auth.domain.LinkMode;
import com.example.pickupservice.auth.domain.OwnerType;
import com.example.pickupservice.auth.domain.Platform;
import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPersistenceEntitiesTest {

	private static final List<Class<?>> ENTITIES = List.of(
			VehicleLinkCodeEntity.class, MemberInviteEntity.class, DeviceEntity.class);

	private static final Map<Class<?>, String> TABLE_NAMES = Map.of(
			VehicleLinkCodeEntity.class, "vehicle_link_code",
			MemberInviteEntity.class, "member_invite",
			DeviceEntity.class, "device");

	@Test
	@DisplayName("VehicleLinkCodeEntity · MemberInviteEntity · DeviceEntity가 BaseTimeEntity를 상속하고 테이블 정의대로 매핑된다")
	void entitiesExtendBaseTimeEntityAndMapToDesignTables() {
		for (Class<?> entity : ENTITIES) {
			assertThat(entity.isAnnotationPresent(Entity.class))
					.as("%s에 @Entity가 있어야 한다", entity.getSimpleName())
					.isTrue();
			assertThat(entity.getSuperclass())
					.as("%s는 BaseTimeEntity를 바로 상속해야 한다", entity.getSimpleName())
					.isEqualTo(BaseTimeEntity.class);
			Table table = entity.getAnnotation(Table.class);
			assertThat(table).as("%s에 @Table이 있어야 한다", entity.getSimpleName()).isNotNull();
			assertThat(table.name())
					.as("%s의 테이블 이름", entity.getSimpleName())
					.isEqualTo(TABLE_NAMES.get(entity));
		}
	}

	@Test
	@DisplayName("세 엔티티의 PK는 codeId · inviteId · deviceId 필드에 @Id와 @GeneratedValue(IDENTITY)로 만든다")
	void entitiesUseIdentityPrimaryKey() {
		Map<Class<?>, String> idFieldNames = Map.of(
				VehicleLinkCodeEntity.class, "codeId",
				MemberInviteEntity.class, "inviteId",
				DeviceEntity.class, "deviceId");
		for (Class<?> entity : ENTITIES) {
			List<Field> idFields = Arrays.stream(entity.getDeclaredFields())
					.filter(field -> field.isAnnotationPresent(Id.class))
					.toList();
			assertThat(idFields).as("%s의 @Id 필드", entity.getSimpleName()).hasSize(1);
			Field idField = idFields.getFirst();
			assertThat(idField.getName()).isEqualTo(idFieldNames.get(entity));
			assertThat(idField.getType()).isEqualTo(Long.class);
			GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
			assertThat(generatedValue).as("%s의 @GeneratedValue", entity.getSimpleName()).isNotNull();
			assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
		}
	}

	@Test
	@DisplayName("DeviceEntity는 NULL을 허용하는 Instant 필드 refreshTokenExpiresAt을 갖는다")
	void deviceEntityHasNullableRefreshTokenExpiresAt() {
		List<Field> fields = Arrays.stream(DeviceEntity.class.getDeclaredFields())
				.filter(field -> field.getName().equals("refreshTokenExpiresAt"))
				.toList();

		assertThat(fields).as("DeviceEntity.refreshTokenExpiresAt 필드").hasSize(1);
		Field field = fields.getFirst();
		assertThat(field.getType()).isEqualTo(Instant.class);
		Column column = field.getAnnotation(Column.class);
		if (column != null) {
			assertThat(column.nullable()).as("refreshTokenExpiresAt은 NULL을 허용해야 한다").isTrue();
		}
	}

	@Test
	@DisplayName("status · mode · ownerType · platform enum 필드는 @Enumerated(STRING)으로 저장한다")
	void enumFieldsAreStoredAsString() {
		List<Field> enumFields = ENTITIES.stream()
				.flatMap(entity -> Arrays.stream(entity.getDeclaredFields()))
				.filter(field -> field.getType().isEnum())
				.toList();

		assertThat(enumFields)
				.extracting(field -> field.getDeclaringClass().getSimpleName() + "." + field.getName()
						+ ":" + field.getType().getName())
				.contains(
						"VehicleLinkCodeEntity.status:" + LinkCodeStatus.class.getName(),
						"VehicleLinkCodeEntity.mode:" + LinkMode.class.getName(),
						"DeviceEntity.ownerType:" + OwnerType.class.getName(),
						"DeviceEntity.platform:" + Platform.class.getName());
		for (Field field : enumFields) {
			Enumerated enumerated = field.getAnnotation(Enumerated.class);
			assertThat(enumerated)
					.as("%s.%s에 @Enumerated가 있어야 한다", field.getDeclaringClass().getSimpleName(), field.getName())
					.isNotNull();
			assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
		}
	}

}
