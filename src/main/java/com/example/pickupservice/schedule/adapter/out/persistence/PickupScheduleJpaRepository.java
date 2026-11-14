package com.example.pickupservice.schedule.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PickupScheduleJpaRepository extends JpaRepository<PickupScheduleEntity, Long> {

	List<PickupScheduleEntity> findByChildUserId(Long childUserId);

	List<PickupScheduleEntity> findByChildUserIdIn(List<Long> childUserIds);

	List<PickupScheduleEntity> findByAcademyId(Long academyId);

}
