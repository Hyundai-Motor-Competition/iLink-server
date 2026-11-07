package com.example.pickupservice.schedule.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

interface AcademyJpaRepository extends JpaRepository<AcademyEntity, Long> {

	Optional<AcademyEntity> findByExternalPlaceId(String externalPlaceId);

	/**
	 * 같은 external_place_id 행이 이미 있으면(동시 등록 포함) 유니크 위반 없이 그 행을 갱신한다.
	 * entrance · phone은 새 값이 비어 있으면 기존 값을 유지한다. 좌표는 EWKT 문자열로 받는다.
	 */
	@Transactional
	@Query(value = """
			INSERT INTO academy (external_place_id, name, address, location, entrance, phone, created_at, updated_at)
			VALUES (:externalPlaceId, :name, :address,
			        ST_GeogFromText(CAST(:location AS text)),
			        ST_GeogFromText(CAST(:entrance AS text)),
			        CAST(:phone AS varchar),
			        now(), now())
			ON CONFLICT (external_place_id) DO UPDATE SET
			    name = EXCLUDED.name,
			    address = EXCLUDED.address,
			    location = EXCLUDED.location,
			    entrance = COALESCE(EXCLUDED.entrance, academy.entrance),
			    phone = COALESCE(EXCLUDED.phone, academy.phone),
			    updated_at = EXCLUDED.updated_at
			RETURNING academy_id
			""", nativeQuery = true)
	Long upsertByExternalPlaceId(@Param("externalPlaceId") String externalPlaceId, @Param("name") String name,
			@Param("address") String address, @Param("location") String location, @Param("entrance") String entrance,
			@Param("phone") String phone);
}
