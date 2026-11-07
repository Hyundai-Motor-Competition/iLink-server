package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserPersistenceEntitiesTest {

	private static final List<Class<?>> ENTITIES = List.of(FamilyEntity.class, UserEntity.class, VehicleEntity.class);

	@Test
	@DisplayName("FamilyEntity · UserEntity · VehicleEntity가 BaseTimeEntity를 상속하고 UserEntity는 users 테이블에 매핑된다")
	void entitiesExtendBaseTimeEntityAndUserEntityMapsToUsers() {
		for (Class<?> entity : ENTITIES) {
			assertThat(entity.isAnnotationPresent(Entity.class))
					.as("%s에 @Entity가 있어야 한다", entity.getSimpleName())
					.isTrue();
			assertThat(entity.getSuperclass())
					.as("%s는 BaseTimeEntity를 바로 상속해야 한다", entity.getSimpleName())
					.isEqualTo(BaseTimeEntity.class);
		}
		Table table = UserEntity.class.getAnnotation(Table.class);
		assertThat(table).as("UserEntity에 @Table이 있어야 한다").isNotNull();
		assertThat(table.name()).isEqualTo("users");
	}

	@Test
	@DisplayName("세 엔티티의 PK는 @Id와 @GeneratedValue(IDENTITY)로 만든다")
	void entitiesUseIdentityPrimaryKey() {
		for (Class<?> entity : ENTITIES) {
			List<Field> idFields = Arrays.stream(entity.getDeclaredFields())
					.filter(field -> field.isAnnotationPresent(Id.class))
					.toList();
			assertThat(idFields).as("%s의 @Id 필드", entity.getSimpleName()).hasSize(1);
			GeneratedValue generatedValue = idFields.getFirst().getAnnotation(GeneratedValue.class);
			assertThat(generatedValue).as("%s의 @GeneratedValue", entity.getSimpleName()).isNotNull();
			assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
		}
	}

	@Test
	@DisplayName("세 엔티티의 enum 필드는 @Enumerated(STRING)으로 저장한다")
	void enumFieldsAreStoredAsString() {
		List<Field> enumFields = ENTITIES.stream()
				.flatMap(entity -> Arrays.stream(entity.getDeclaredFields()))
				.filter(field -> field.getType().isEnum())
				.toList();

		assertThat(enumFields)
				.as("FamilyEntity의 joinCallMode와 UserEntity의 role은 enum 필드여야 한다")
				.extracting(field -> field.getDeclaringClass().getSimpleName() + "." + field.getType().getSimpleName())
				.contains("FamilyEntity.JoinCallMode", "UserEntity.Role");
		for (Field field : enumFields) {
			Enumerated enumerated = field.getAnnotation(Enumerated.class);
			assertThat(enumerated).as("%s에 @Enumerated가 있어야 한다", field.getName()).isNotNull();
			assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
		}
	}

}
