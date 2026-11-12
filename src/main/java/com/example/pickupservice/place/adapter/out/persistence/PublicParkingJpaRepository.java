package com.example.pickupservice.place.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface PublicParkingJpaRepository extends JpaRepository<PublicParkingEntity, String> {

	@Query(value = """
			SELECT * FROM public_parking
			WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusM)
			""", nativeQuery = true)
	List<PublicParkingEntity> findWithin(@Param("lat") double lat, @Param("lng") double lng,
			@Param("radiusM") double radiusM);

}
