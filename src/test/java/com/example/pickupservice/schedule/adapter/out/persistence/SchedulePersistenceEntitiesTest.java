package com.example.pickupservice.schedule.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulePersistenceEntitiesTest {

	private static final List<Class<?>> ENTITIES = List.of(AcademyEntity.class, PickupScheduleEntity.class);
	private static final String GEOGRAPHY_POINT_4326 = "geography(Point,4326)";

	@Test
	@DisplayName("AcademyEntity · PickupScheduleEntity는 @Entity이고 BaseTimeEntity를 바로 상속한다")
	void entitiesExtendBaseTimeEntity() {
		for (Class<?> entity : ENTITIES) {
			assertThat(entity.isAnnotationPresent(Entity.class))
					.as("%s에 @Entity가 있어야 한다", entity.getSimpleName())
					.isTrue();
			assertThat(entity.getSuperclass())
					.as("%s는 BaseTimeEntity를 바로 상속해야 한다", entity.getSimpleName())
					.isEqualTo(BaseTimeEntity.class);
		}
	}

	@Test
	@DisplayName("두 엔티티의 PK는 @Id와 @GeneratedValue(IDENTITY)로 만들고 이름은 academyId · scheduleId다")
	void entitiesUseIdentityPrimaryKey() {
		assertIdentityId(AcademyEntity.class, "academyId");
		assertIdentityId(PickupScheduleEntity.class, "scheduleId");
	}

	@Test
	@DisplayName("AcademyEntity의 location · entrance는 JTS Point 필드이고 columnDefinition geography(Point,4326)로 매핑된다")
	void academyLocationAndEntranceAreGeographyPoint4326() throws NoSuchFieldException {
		for (String fieldName : List.of("location", "entrance")) {
			Field field = AcademyEntity.class.getDeclaredField(fieldName);
			assertThat(field.getType())
					.as("AcademyEntity.%s는 org.locationtech.jts.geom.Point여야 한다", fieldName)
					.isEqualTo(Point.class);
			Column column = field.getAnnotation(Column.class);
			assertThat(column).as("AcademyEntity.%s에 @Column이 있어야 한다", fieldName).isNotNull();
			assertThat(column.columnDefinition())
					.as("AcademyEntity.%s의 columnDefinition", fieldName)
					.isEqualTo(GEOGRAPHY_POINT_4326);
		}
	}

	@Test
	@DisplayName("PickupScheduleEntity의 status는 enum 필드이고 @Enumerated(STRING)으로 저장한다")
	void pickupScheduleStatusIsStoredAsString() throws NoSuchFieldException {
		Field status = PickupScheduleEntity.class.getDeclaredField("status");

		assertThat(status.getType().isEnum()).as("PickupScheduleEntity.status는 enum이어야 한다").isTrue();
		Enumerated enumerated = status.getAnnotation(Enumerated.class);
		assertThat(enumerated).as("PickupScheduleEntity.status에 @Enumerated가 있어야 한다").isNotNull();
		assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
	}

	private static void assertIdentityId(Class<?> entity, String expectedName) {
		List<Field> idFields = Arrays.stream(entity.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Id.class))
				.toList();
		assertThat(idFields).as("%s의 @Id 필드", entity.getSimpleName()).hasSize(1);
		Field idField = idFields.getFirst();
		assertThat(idField.getName()).isEqualTo(expectedName);
		assertThat(idField.getType()).isEqualTo(Long.class);
		GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
		assertThat(generatedValue).as("%s의 @GeneratedValue", entity.getSimpleName()).isNotNull();
		assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
	}

}
