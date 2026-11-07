package com.example.pickupservice.user.adapter.out.persistence;

import com.example.pickupservice.common.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

	List<UserEntity> findByFamilyId(Long familyId);

	long countByFamilyIdAndRoleNot(Long familyId, Role role);

}
