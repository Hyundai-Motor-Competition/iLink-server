package com.example.pickupservice.user.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadMemberUseCase {

	Optional<MemberInfo> findMember(long userId);

	List<MemberInfo> findMembers(long familyId);

	Map<Long, String> findProfileImageUrls(List<Long> userIds);

}
