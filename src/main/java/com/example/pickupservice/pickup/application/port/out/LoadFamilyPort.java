package com.example.pickupservice.pickup.application.port.out;

import com.example.pickupservice.user.application.port.in.FamilyInfo;
import com.example.pickupservice.user.application.port.in.MemberInfo;

import java.util.List;
import java.util.Optional;

public interface LoadFamilyPort {

	FamilyInfo loadFamily(long familyId);

	List<MemberInfo> loadMembers(long familyId);

	Optional<MemberInfo> loadMember(long userId);

}
