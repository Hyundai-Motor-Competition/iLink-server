package com.example.pickupservice.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface MemberInviteJpaRepository extends JpaRepository<MemberInviteEntity, Long> {

	Optional<MemberInviteEntity> findByToken(String token);

	Optional<MemberInviteEntity> findFirstByUserIdOrderByInviteIdDesc(Long userId);

}
