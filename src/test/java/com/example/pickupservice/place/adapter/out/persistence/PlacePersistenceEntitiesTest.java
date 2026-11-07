package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

class PlacePersistenceEntitiesTest {

	private static final String GEOGRAPHY_POINT_4326 = "geography(Point,4326)";

	@Test
	@DisplayName("PickupSpotEntity는 @Entity이고 BaseTimeEntity를 바로 상속한다")
	void pickupSpotEntityExtendsBaseTimeEntity() {
		assertThat(PickupSpotEntity.class.isAnnotationPresent(Entity.class))
				.as("PickupSpotEntity에 @Entity가 있어야 한다")
				.isTrue();
		assertThat(PickupSpotEntity.class.getSuperclass())
				.as("PickupSpotEntity는 BaseTimeEntity를 바로 상속해야 한다")
				.isEqualTo(BaseTimeEntity.class);
	}

	@Test
	@DisplayName("PickupSpotEntity의 PK는 Long spotId이고 @Id와 @GeneratedValue(IDENTITY)로 만든다")
	void pickupSpotEntityUsesIdentityPrimaryKey() {
		List<Field> idFields = Arrays.stream(PickupSpotEntity.class.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Id.class))
				.toList();
		assertThat(idFields).as("PickupSpotEntity의 @Id 필드").hasSize(1);
		Field idField = idFields.getFirst();
		assertThat(idField.getName()).isEqualTo("spotId");
		assertThat(idField.getType()).isEqualTo(Long.class);
		GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
		assertThat(generatedValue).as("PickupSpotEntity의 @GeneratedValue").isNotNull();
		assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
	}

	@Test
	@DisplayName("PickupSpotEntity의 stopPoint · meetingPoint는 JTS Point 필드이고 columnDefinition geography(Point,4326)로 매핑된다")
	void stopPointAndMeetingPointAreGeographyPoint4326() throws NoSuchFieldException {
		for (String fieldName : List.of("stopPoint", "meetingPoint")) {
			Field field = PickupSpotEntity.class.getDeclaredField(fieldName);
			assertThat(field.getType())
					.as("PickupSpotEntity.%s는 org.locationtech.jts.geom.Point여야 한다", fieldName)
					.isEqualTo(Point.class);
			Column column = field.getAnnotation(Column.class);
			assertThat(column).as("PickupSpotEntity.%s에 @Column이 있어야 한다", fieldName).isNotNull();
			assertThat(column.columnDefinition())
					.as("PickupSpotEntity.%s의 columnDefinition", fieldName)
					.isEqualTo(GEOGRAPHY_POINT_4326);
		}
	}

}
