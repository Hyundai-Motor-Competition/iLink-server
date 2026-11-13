package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.application.port.out.ProfileImageStoragePort;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadMemberService implements LoadMemberUseCase {

	private final LoadUserPort loadUserPort;
	private final ProfileImageStoragePort profileImageStoragePort;

	@Override
	public Optional<MemberInfo> findMember(long userId) {
		return loadUserPort.loadUser(userId).map(LoadMemberService::toInfo);
	}

	@Override
	public List<MemberInfo> findMembers(long familyId) {
		return loadUserPort.loadUsers(familyId).stream()
				.sorted(Comparator.comparing(User::getUserId))
				.map(LoadMemberService::toInfo)
				.toList();
	}

	@Override
	public Map<Long, String> findProfileImageUrls(List<Long> userIds) {
		Map<Long, String> urls = new HashMap<>();
		for (User user : loadUserPort.loadUsersByIds(userIds)) {
			ProfileImageUrls.of(profileImageStoragePort, user).ifPresent(url -> urls.put(user.getUserId(), url));
		}
		return urls;
	}

	private static MemberInfo toInfo(User user) {
		return new MemberInfo(user.getUserId(), user.getFamilyId(), user.getRole(), user.getName(), user.getPhone());
	}
}
