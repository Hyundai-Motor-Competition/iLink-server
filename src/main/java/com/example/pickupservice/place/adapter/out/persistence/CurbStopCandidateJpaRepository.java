package com.example.pickupservice.place.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface CurbStopCandidateJpaRepository extends JpaRepository<CurbStopCandidateEntity, Long> {

	@Query(value = """
			SELECT * FROM curb_stop_candidate
			WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM)
			""", nativeQuery = true)
	List<CurbStopCandidateEntity> findWithin(@Param("lat") double lat, @Param("lng") double lng,
			@Param("radiusM") double radiusM);

}
