package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.place.domain.LineKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotReferenceEntitiesTest {

	private static final String GEOGRAPHY_POINT_4326 = "geography(Point,4326)";

	@Test
	@DisplayName("PublicParkingEntity는 public_parking 테이블의 @Entity이고 BaseTimeEntity를 바로 상속한다")
	void publicParkingEntityIsEntityOfPublicParkingTable() {
		assertEntityOf(PublicParkingEntity.class, "public_parking");
	}

	@Test
	@DisplayName("PublicParkingEntity의 PK는 String parkingId(길이 20)이고 값을 직접 넣는다(@GeneratedValue 없음)")
	void publicParkingEntityUsesAssignedStringPrimaryKey() {
		Field idField = assertAssignedId(PublicParkingEntity.class, "parkingId", String.class);
		assertThat(idField.getAnnotation(Column.class)).isNotNull();
		assertThat(idField.getAnnotation(Column.class).length()).isEqualTo(20);
	}

	@Test
	@DisplayName("PublicParkingEntity는 name VARCHAR(100) · location · spaces · baseMinutes · baseFeeWon(NULL 가능 Integer) 필드를 갖는다")
	void publicParkingEntityHasDesignFields() throws NoSuchFieldException {
		Field name = PublicParkingEntity.class.getDeclaredField("name");
		assertThat(name.getType()).isEqualTo(String.class);
		assertThat(name.getAnnotation(Column.class).length()).isEqualTo(100);
		assertThat(name.getAnnotation(Column.class).nullable()).isFalse();

		for (String fieldName : List.of("spaces", "baseMinutes", "baseFeeWon")) {
			Field field = PublicParkingEntity.class.getDeclaredField(fieldName);
			assertThat(field.getType()).as("PublicParkingEntity.%s 타입", fieldName).isEqualTo(Integer.class);
			Column column = field.getAnnotation(Column.class);
			assertThat(column == null || column.nullable())
					.as("PublicParkingEntity.%s는 NULL 가능", fieldName)
					.isTrue();
		}
		assertLocationIsGeographyPoint(PublicParkingEntity.class);
	}

	@Test
	@DisplayName("CurbStopCandidateEntity는 curb_stop_candidate 테이블의 @Entity이고 BaseTimeEntity를 바로 상속한다")
	void curbStopCandidateEntityIsEntityOfCurbStopCandidateTable() {
		assertEntityOf(CurbStopCandidateEntity.class, "curb_stop_candidate");
	}

	@Test
	@DisplayName("CurbStopCandidateEntity의 PK는 Long candidateId이고 값을 직접 넣는다(@GeneratedValue 없음)")
	void curbStopCandidateEntityUsesAssignedLongPrimaryKey() {
		assertAssignedId(CurbStopCandidateEntity.class, "candidateId", Long.class);
	}

	@Test
	@DisplayName("CurbStopCandidateEntity는 roadName VARCHAR(100) NULL · lineKind 문자열 enum VARCHAR(20) · nearEnforcementCctv boolean · location 필드를 갖는다")
	void curbStopCandidateEntityHasDesignFields() throws NoSuchFieldException {
		Field roadName = CurbStopCandidateEntity.class.getDeclaredField("roadName");
		assertThat(roadName.getType()).isEqualTo(String.class);
		assertThat(roadName.getAnnotation(Column.class).length()).isEqualTo(100);
		assertThat(roadName.getAnnotation(Column.class).nullable()).isTrue();

		Field lineKind = CurbStopCandidateEntity.class.getDeclaredField("lineKind");
		assertThat(lineKind.getType()).isEqualTo(LineKind.class);
		assertThat(lineKind.getAnnotation(Enumerated.class)).isNotNull();
		assertThat(lineKind.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
		assertThat(lineKind.getAnnotation(Column.class).length()).isEqualTo(20);
		assertThat(lineKind.getAnnotation(Column.class).nullable()).isFalse();

		Field cctv = CurbStopCandidateEntity.class.getDeclaredField("nearEnforcementCctv");
		assertThat(cctv.getType()).isEqualTo(boolean.class);
		assertLocationIsGeographyPoint(CurbStopCandidateEntity.class);
	}

	private static void assertEntityOf(Class<?> entityClass, String tableName) {
		assertThat(entityClass.isAnnotationPresent(Entity.class))
				.as("%s에 @Entity가 있어야 한다", entityClass.getSimpleName())
				.isTrue();
		assertThat(entityClass.getAnnotation(Table.class)).isNotNull();
		assertThat(entityClass.getAnnotation(Table.class).name()).isEqualTo(tableName);
		assertThat(entityClass.getSuperclass())
				.as("%s는 BaseTimeEntity를 바로 상속해야 한다", entityClass.getSimpleName())
				.isEqualTo(BaseTimeEntity.class);
	}

	private static Field assertAssignedId(Class<?> entityClass, String idName, Class<?> idType) {
		List<Field> idFields = Arrays.stream(entityClass.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Id.class))
				.toList();
		assertThat(idFields).as("%s의 @Id 필드", entityClass.getSimpleName()).hasSize(1);
		Field idField = idFields.getFirst();
		assertThat(idField.getName()).isEqualTo(idName);
		assertThat(idField.getType()).isEqualTo(idType);
		assertThat(idField.isAnnotationPresent(GeneratedValue.class))
				.as("%s.%s는 CSV의 값을 그대로 쓰므로 자동 생성이 아니다", entityClass.getSimpleName(), idName)
				.isFalse();
		return idField;
	}

	private static void assertLocationIsGeographyPoint(Class<?> entityClass) throws NoSuchFieldException {
		Field location = entityClass.getDeclaredField("location");
		assertThat(location.getType())
				.as("%s.location은 JTS Point여야 한다", entityClass.getSimpleName())
				.isEqualTo(Point.class);
		Column column = location.getAnnotation(Column.class);
		assertThat(column).as("%s.location에 @Column이 있어야 한다", entityClass.getSimpleName()).isNotNull();
		assertThat(column.columnDefinition()).isEqualTo(GEOGRAPHY_POINT_4326);
		assertThat(column.nullable()).isFalse();
	}
}
