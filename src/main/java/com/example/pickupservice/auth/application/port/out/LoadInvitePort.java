package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.MemberInvite;

import java.util.Optional;

public interface LoadInvitePort {

	Optional<MemberInvite> loadInviteByToken(String token);

	Optional<MemberInvite> loadLatestInvite(long userId);
}
