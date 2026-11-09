package com.example.pickupservice.pickup.adapter.out.persistence;

import com.example.pickupservice.common.persistence.BaseTimeEntity;
import com.example.pickupservice.pickup.domain.CallStatus;
import com.example.pickupservice.pickup.domain.CallTriggerType;
import com.example.pickupservice.pickup.domain.ChildPhase;
import com.example.pickupservice.pickup.domain.SessionStatus;
import com.example.pickupservice.pickup.domain.SpotStatus;
import com.example.pickupservice.pickup.domain.VehiclePhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PickupPersistenceEntitiesTest {

	private static final List<Class<?>> ENTITIES = List.of(
			PickupSessionEntity.class,
			SessionChildEntity.class,
			SessionEventEntity.class,
			CallEntity.class,
			PickupOutcomeEntity.class,
			GuardianDataSettingEntity.class,
			ProcessedVehicleEventEntity.class);
	private static final String GEOGRAPHY_POINT_4326 = "geography(Point,4326)";

	@Test
	@DisplayName("픽업 엔티티 7개(PickupSession · SessionChild · SessionEvent · Call · PickupOutcome · GuardianDataSetting · ProcessedVehicleEvent)는 @Entity이고 BaseTimeEntity를 바로 상속한다")
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
	@DisplayName("PickupSessionEntity · SessionChildEntity · SessionEventEntity · CallEntity의 PK는 Long sessionId · sessionChildId · eventId · callId이고 @GeneratedValue(IDENTITY)로 만든다")
	void generatedEntitiesUseIdentityPrimaryKey() {
		assertIdentityId(PickupSessionEntity.class, "sessionId");
		assertIdentityId(SessionChildEntity.class, "sessionChildId");
		assertIdentityId(SessionEventEntity.class, "eventId");
		assertIdentityId(CallEntity.class, "callId");
	}

	@Test
	@DisplayName("PickupOutcomeEntity의 PK는 Long sessionId이고 생성 전략 없이 세션 ID를 그대로 쓴다")
	void pickupOutcomeUsesSessionIdAsAssignedPrimaryKey() {
		assertAssignedId(PickupOutcomeEntity.class, "sessionId", Long.class);
	}

	@Test
	@DisplayName("GuardianDataSettingEntity의 PK는 Long userId이고 생성 전략 없이 보호자 사용자 ID를 그대로 쓴다")
	void guardianDataSettingUsesUserIdAsAssignedPrimaryKey() {
		assertAssignedId(GuardianDataSettingEntity.class, "userId", Long.class);
	}

	@Test
	@DisplayName("ProcessedVehicleEventEntity의 PK는 String eventId이고 생성 전략 없이 차량이 보낸 eventId를 그대로 쓴다")
	void processedVehicleEventUsesEventIdAsAssignedPrimaryKey() {
		assertAssignedId(ProcessedVehicleEventEntity.class, "eventId", String.class);
	}

	@Test
	@DisplayName("PickupSessionEntity의 stopPoint · meetingPoint는 JTS Point 필드이고 columnDefinition geography(Point,4326)로 매핑된다")
	void pickupSessionStopPointAndMeetingPointAreGeographyPoint4326() {
		for (String fieldName : List.of("stopPoint", "meetingPoint")) {
			Field field = declaredField(PickupSessionEntity.class, fieldName);
			assertThat(field.getType())
					.as("PickupSessionEntity.%s는 org.locationtech.jts.geom.Point여야 한다", fieldName)
					.isEqualTo(Point.class);
			Column column = field.getAnnotation(Column.class);
			assertThat(column).as("PickupSessionEntity.%s에 @Column이 있어야 한다", fieldName).isNotNull();
			assertThat(column.columnDefinition())
					.as("PickupSessionEntity.%s의 columnDefinition", fieldName)
					.isEqualTo(GEOGRAPHY_POINT_4326);
		}
	}

	@Test
	@DisplayName("상태 컬럼 6개는 pickup.domain enum 필드이고 @Enumerated(STRING)으로 저장한다(sessionStatus · vehiclePhase · spotStatus · childPhase · Call.status · Call.triggerType)")
	void stateColumnsAreDomainEnumsStoredAsString() {
		assertStringEnum(PickupSessionEntity.class, "sessionStatus", SessionStatus.class);
		assertStringEnum(PickupSessionEntity.class, "vehiclePhase", VehiclePhase.class);
		assertStringEnum(PickupSessionEntity.class, "spotStatus", SpotStatus.class);
		assertStringEnum(SessionChildEntity.class, "childPhase", ChildPhase.class);
		assertStringEnum(CallEntity.class, "status", CallStatus.class);
		assertStringEnum(CallEntity.class, "triggerType", CallTriggerType.class);
	}

	@Test
	@DisplayName("SessionEventEntity의 payload는 @JdbcTypeCode(SqlTypes.JSON)으로 JSON 값을 저장한다")
	void sessionEventPayloadIsJson() {
		Field payload = declaredField(SessionEventEntity.class, "payload");

		JdbcTypeCode jdbcTypeCode = payload.getAnnotation(JdbcTypeCode.class);
		assertThat(jdbcTypeCode).as("SessionEventEntity.payload에 @JdbcTypeCode가 있어야 한다").isNotNull();
		assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.JSON);
	}

	private static void assertIdentityId(Class<?> entity, String expectedName) {
		Field idField = singleIdField(entity);
		assertThat(idField.getName()).as("%s의 @Id 필드 이름", entity.getSimpleName()).isEqualTo(expectedName);
		assertThat(idField.getType()).as("%s의 @Id 필드 타입", entity.getSimpleName()).isEqualTo(Long.class);
		GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
		assertThat(generatedValue).as("%s의 @GeneratedValue", entity.getSimpleName()).isNotNull();
		assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
	}

	private static void assertAssignedId(Class<?> entity, String expectedName, Class<?> expectedType) {
		Field idField = singleIdField(entity);
		assertThat(idField.getName()).as("%s의 @Id 필드 이름", entity.getSimpleName()).isEqualTo(expectedName);
		assertThat(idField.getType()).as("%s의 @Id 필드 타입", entity.getSimpleName()).isEqualTo(expectedType);
		assertThat(idField.getAnnotation(GeneratedValue.class))
				.as("%s.%s에는 @GeneratedValue가 없어야 한다", entity.getSimpleName(), expectedName)
				.isNull();
	}

	private static Field singleIdField(Class<?> entity) {
		List<Field> idFields = Arrays.stream(entity.getDeclaredFields())
				.filter(field -> field.isAnnotationPresent(Id.class))
				.toList();
		assertThat(idFields).as("%s의 @Id 필드", entity.getSimpleName()).hasSize(1);
		return idFields.getFirst();
	}

	private static void assertStringEnum(Class<?> entity, String fieldName, Class<? extends Enum<?>> enumType) {
		Field field = declaredField(entity, fieldName);
		assertThat(field.getType())
				.as("%s.%s는 %s여야 한다", entity.getSimpleName(), fieldName, enumType.getSimpleName())
				.isEqualTo(enumType);
		Enumerated enumerated = field.getAnnotation(Enumerated.class);
		assertThat(enumerated)
				.as("%s.%s에 @Enumerated가 있어야 한다", entity.getSimpleName(), fieldName)
				.isNotNull();
		assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
	}

	private static Field declaredField(Class<?> entity, String fieldName) {
		Optional<Field> field = Arrays.stream(entity.getDeclaredFields())
				.filter(candidate -> candidate.getName().equals(fieldName))
				.findFirst();
		assertThat(field).as("%s.%s 필드가 있어야 한다", entity.getSimpleName(), fieldName).isPresent();
		return field.get();
	}

}
