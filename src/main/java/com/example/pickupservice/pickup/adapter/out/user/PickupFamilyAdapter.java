package com.example.pickupservice.pickup.adapter.out.user;

import com.example.pickupservice.pickup.application.port.out.LoadFamilyPort;
import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.LoadFamilyUseCase;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PickupFamilyAdapter implements LoadFamilyPort {

	private final LoadFamilyUseCase loadFamilyUseCase;
	private final LoadMemberUseCase loadMemberUseCase;

	@Override
	public FamilyInfo loadFamily(long familyId) {
		return loadFamilyUseCase.findFamily(familyId);
	}

	@Override
	public List<MemberInfo> loadMembers(long familyId) {
		return loadMemberUseCase.findMembers(familyId);
	}

	@Override
	public Optional<MemberInfo> loadMember(long userId) {
		return loadMemberUseCase.findMember(userId);
	}
}
