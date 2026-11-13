package com.example.pickupservice.place.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface PickupSpotJpaRepository extends JpaRepository<PickupSpotEntity, Long> {

	List<PickupSpotEntity> findByFamilyIdAndTemporaryFalse(Long familyId);

	List<PickupSpotEntity> findByFamilyIdAndAcademyIdAndTemporaryFalse(Long familyId, Long academyId);

	Optional<PickupSpotEntity> findByFamilyIdAndAcademyIdAndIsDefaultTrueAndTemporaryFalse(Long familyId,
			Long academyId);

	@Query(value = """
			SELECT * FROM pickup_spot
			WHERE family_id = :familyId
			  AND NOT temporary
			  AND ST_DWithin(stop_point, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM)
			""", nativeQuery = true)
	List<PickupSpotEntity> findWithin(@Param("familyId") long familyId, @Param("lat") double lat,
			@Param("lng") double lng, @Param("radiusM") double radiusM);

	@Query(value = """
			SELECT * FROM pickup_spot
			WHERE family_id = :familyId
			  AND academy_id = :academyId
			  AND NOT temporary
			  AND ST_DWithin(stop_point, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM)
			""", nativeQuery = true)
	List<PickupSpotEntity> findWithinByAcademy(@Param("familyId") long familyId, @Param("academyId") long academyId,
			@Param("lat") double lat, @Param("lng") double lng, @Param("radiusM") double radiusM);

}
