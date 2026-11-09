package com.example.pickupservice.place.adapter.out.persistence;

import com.example.pickupservice.common.geo.GeoPoints;
import com.example.pickupservice.common.geo.LatLng;
import com.example.pickupservice.place.application.port.out.DefaultSpotConflictException;
import com.example.pickupservice.place.application.port.out.LoadSpotPort;
import com.example.pickupservice.place.application.port.out.SaveSpotPort;
import com.example.pickupservice.place.domain.PickupSpot;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlacePersistenceAdapter implements LoadSpotPort, SaveSpotPort {

	private static final String DEFAULT_SPOT_INDEX = "uk_pickup_spot_default";

	private final PickupSpotJpaRepository pickupSpotJpaRepository;

	@Override
	public Optional<PickupSpot> loadSpot(long spotId) {
		return pickupSpotJpaRepository.findById(spotId).map(PlacePersistenceAdapter::toDomain);
	}

	@Override
	public List<PickupSpot> loadSpots(long familyId, Long academyId) {
		List<PickupSpotEntity> entities = academyId == null
				? pickupSpotJpaRepository.findByFamilyId(familyId)
				: pickupSpotJpaRepository.findByFamilyIdAndAcademyId(familyId, academyId);
		return entities.stream().map(PlacePersistenceAdapter::toDomain).toList();
	}

	@Override
	public List<PickupSpot> loadSpotsWithin(long familyId, Long academyId, LatLng near, int radiusM) {
		List<PickupSpotEntity> entities = academyId == null
				? pickupSpotJpaRepository.findWithin(familyId, near.lat(), near.lng(), radiusM)
				: pickupSpotJpaRepository.findWithinByAcademy(familyId, academyId, near.lat(), near.lng(), radiusM);
		return entities.stream().map(PlacePersistenceAdapter::toDomain).toList();
	}

	@Override
	public Optional<PickupSpot> loadDefaultSpot(long familyId, long academyId) {
		return pickupSpotJpaRepository.findByFamilyIdAndAcademyIdAndIsDefaultTrue(familyId, academyId)
				.map(PlacePersistenceAdapter::toDomain);
	}

	@Override
	public PickupSpot saveSpot(PickupSpot spot) {
		try {
			return toDomain(pickupSpotJpaRepository.saveAndFlush(toEntity(spot)));
		} catch (DataIntegrityViolationException violation) {
			if (isDefaultSpotViolation(violation)) {
				throw new DefaultSpotConflictException(violation);
			}
			throw violation;
		}
	}

	private static boolean isDefaultSpotViolation(DataIntegrityViolationException violation) {
		for (Throwable cause = violation; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation) {
				return DEFAULT_SPOT_INDEX.equalsIgnoreCase(constraintViolation.getConstraintName());
			}
		}
		return false;
	}

	@Override
	public void deleteSpot(long spotId) {
		pickupSpotJpaRepository.deleteById(spotId);
	}

	private static PickupSpot toDomain(PickupSpotEntity entity) {
		return PickupSpot.restore(entity.getSpotId(), entity.getFamilyId(), entity.getAcademyId(), entity.getName(),
				entity.getMemo(), GeoPoints.toLatLng(entity.getStopPoint()),
				GeoPoints.toLatLng(entity.getMeetingPoint()), entity.isDefault(), entity.getCreatedBy());
	}

	private static PickupSpotEntity toEntity(PickupSpot spot) {
		return new PickupSpotEntity(spot.getSpotId(), spot.getFamilyId(), spot.getAcademyId(), spot.getName(),
				spot.getMemo(), GeoPoints.toPoint(spot.getStopPoint().lat(), spot.getStopPoint().lng()),
				GeoPoints.toPoint(spot.getMeetingPoint().lat(), spot.getMeetingPoint().lng()), spot.isDefault(),
				spot.getCreatedBy());
	}

}
