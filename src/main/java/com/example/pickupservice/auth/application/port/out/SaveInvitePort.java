package com.example.pickupservice.auth.application.port.out;

import com.example.pickupservice.auth.domain.MemberInvite;

public interface SaveInvitePort {

	MemberInvite saveInvite(MemberInvite invite);
}
