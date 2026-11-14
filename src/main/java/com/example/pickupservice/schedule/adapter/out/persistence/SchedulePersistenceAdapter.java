package com.example.pickupservice.schedule.adapter.out.persistence;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.schedule.application.port.out.LoadAcademyPort;
import com.example.pickupservice.schedule.application.port.out.LoadSchedulePort;
import com.example.pickupservice.schedule.application.port.out.SaveAcademyPort;
import com.example.pickupservice.schedule.application.port.out.SaveSchedulePort;
import com.example.pickupservice.schedule.domain.Academy;
import com.example.pickupservice.schedule.domain.PickupSchedule;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class SchedulePersistenceAdapter implements LoadAcademyPort, SaveAcademyPort, LoadSchedulePort, SaveSchedulePort {

	private static final String WEEKDAY_DELIMITER = ",";
	private static final int WEEKDAY_ABBREVIATION_LENGTH = 3;

	private final AcademyJpaRepository academyJpaRepository;
	private final PickupScheduleJpaRepository pickupScheduleJpaRepository;

	@Override
	public Optional<Academy> loadAcademy(long academyId) {
		return academyJpaRepository.findById(academyId).map(SchedulePersistenceAdapter::toDomain);
	}

	@Override
	public Optional<Academy> loadAcademyByExternalPlaceId(long familyId, String externalPlaceId) {
		return academyJpaRepository.findByFamilyIdAndExternalPlaceId(familyId, externalPlaceId)
				.map(SchedulePersistenceAdapter::toDomain);
	}

	@Override
	public Academy saveAcademy(Academy academy) {
		if (academy.getAcademyId() != null) {
			return toDomain(academyJpaRepository.save(toEntity(academy)));
		}
		Long academyId = academyJpaRepository.upsertByFamilyIdAndExternalPlaceId(academy.getFamilyId(),
				academy.getExternalPlaceId(), academy.getName(), academy.getAddress(), toEwkt(academy.getLocation()),
				toEwkt(academy.getEntrance()), academy.getPhone());
		return findAcademy(academyId);
	}

	private static String toEwkt(LatLng latLng) {
		return latLng == null ? null
				: String.format(Locale.ROOT, "SRID=4326;POINT(%s %s)", latLng.lng(), latLng.lat());
	}

	@Override
	public Optional<PickupSchedule> loadSchedule(long scheduleId) {
		return pickupScheduleJpaRepository.findById(scheduleId).map(this::toDomainWithAcademy);
	}

	@Override
	public List<PickupSchedule> loadSchedulesOfChild(long childUserId) {
		return toDomains(pickupScheduleJpaRepository.findByChildUserId(childUserId));
	}

	@Override
	public List<PickupSchedule> loadSchedulesOfChildren(List<Long> childUserIds) {
		if (childUserIds.isEmpty()) {
			return List.of();
		}
		return toDomains(pickupScheduleJpaRepository.findByChildUserIdIn(childUserIds));
	}

	@Override
	public List<PickupSchedule> loadSchedules(List<Long> scheduleIds) {
		if (scheduleIds.isEmpty()) {
			return List.of();
		}
		return toDomains(pickupScheduleJpaRepository.findAllById(scheduleIds));
	}

	@Override
	public List<PickupSchedule> loadSchedulesOfAcademy(long academyId) {
		return toDomains(pickupScheduleJpaRepository.findByAcademyId(academyId));
	}

	@Override
	public PickupSchedule saveSchedule(PickupSchedule schedule) {
		PickupScheduleEntity saved = pickupScheduleJpaRepository.save(toEntity(schedule));
		return toDomain(saved, schedule.getAcademy());
	}

	private PickupSchedule toDomainWithAcademy(PickupScheduleEntity entity) {
		return toDomain(entity, findAcademy(entity.getAcademyId()));
	}

	private List<PickupSchedule> toDomains(List<PickupScheduleEntity> entities) {
		Set<Long> academyIds = entities.stream().map(PickupScheduleEntity::getAcademyId).collect(Collectors.toSet());
		Map<Long, Academy> academies = academyJpaRepository.findAllById(academyIds).stream()
				.map(SchedulePersistenceAdapter::toDomain)
				.collect(Collectors.toMap(Academy::getAcademyId, Function.identity()));
		return entities.stream().map(entity -> toDomain(entity, academyOf(academies, entity.getAcademyId()))).toList();
	}

	private Academy findAcademy(Long academyId) {
		return academyJpaRepository.findById(academyId)
				.map(SchedulePersistenceAdapter::toDomain)
				.orElseThrow(() -> missingAcademy(academyId));
	}

	private static Academy academyOf(Map<Long, Academy> academies, Long academyId) {
		Academy academy = academies.get(academyId);
		if (academy == null) {
			throw missingAcademy(academyId);
		}
		return academy;
	}

	private static IllegalStateException missingAcademy(Long academyId) {
		return new IllegalStateException("일정이 가리키는 학원이 없습니다: academyId=" + academyId);
	}

	private static Academy toDomain(AcademyEntity entity) {
		return Academy.restore(entity.getAcademyId(), entity.getFamilyId(), entity.getExternalPlaceId(),
				entity.getName(), entity.getAddress(), toLatLng(entity.getLocation()), toLatLng(entity.getEntrance()),
				entity.getPhone());
	}

	private static AcademyEntity toEntity(Academy academy) {
		return new AcademyEntity(academy.getAcademyId(), academy.getFamilyId(), academy.getExternalPlaceId(),
				academy.getName(), academy.getAddress(),
				toPoint(academy.getLocation()), toPoint(academy.getEntrance()), academy.getPhone());
	}

	private static PickupSchedule toDomain(PickupScheduleEntity entity, Academy academy) {
		return PickupSchedule.restore(entity.getScheduleId(), entity.getChildUserId(), academy,
				toWeekdays(entity.getWeekdays()), entity.getClassEndTime(), entity.getTargetPickup(),
				entity.getStatus());
	}

	private static PickupScheduleEntity toEntity(PickupSchedule schedule) {
		return new PickupScheduleEntity(schedule.getScheduleId(), schedule.getChildUserId(),
				schedule.getAcademy().getAcademyId(), toWeekdaysColumn(schedule.getWeekdays()),
				schedule.getClassEndTime(), (short) schedule.getTargetPickup(), schedule.getStatus());
	}

	private static Point toPoint(LatLng latLng) {
		return latLng == null ? null : GeoPoints.toPoint(latLng.lat(), latLng.lng());
	}

	private static LatLng toLatLng(Point point) {
		return point == null ? null : GeoPoints.toLatLng(point);
	}

	private static String toWeekdaysColumn(Set<DayOfWeek> weekdays) {
		return weekdays.stream()
				.sorted()
				.map(SchedulePersistenceAdapter::abbreviationOf)
				.collect(Collectors.joining(WEEKDAY_DELIMITER));
	}

	private static Set<DayOfWeek> toWeekdays(String column) {
		return Arrays.stream(column.split(WEEKDAY_DELIMITER))
				.map(SchedulePersistenceAdapter::weekdayOf)
				.collect(Collectors.toSet());
	}

	private static String abbreviationOf(DayOfWeek weekday) {
		return weekday.name().substring(0, WEEKDAY_ABBREVIATION_LENGTH);
	}

	private static DayOfWeek weekdayOf(String abbreviation) {
		return Arrays.stream(DayOfWeek.values())
				.filter(weekday -> abbreviationOf(weekday).equals(abbreviation))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("알 수 없는 요일 값입니다: " + abbreviation));
	}

}
