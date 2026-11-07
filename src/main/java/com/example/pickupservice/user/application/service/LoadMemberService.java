package com.example.pickupservice.user.application.service;

import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import com.example.pickupservice.user.application.port.in.MemberInfo;
import com.example.pickupservice.user.application.port.out.LoadUserPort;
import com.example.pickupservice.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoadMemberService implements LoadMemberUseCase {

	private final LoadUserPort loadUserPort;

	@Override
	public Optional<MemberInfo> findMember(long userId) {
		return loadUserPort.loadUser(userId).map(LoadMemberService::toInfo);
	}

	@Override
	public List<MemberInfo> findMembers(long familyId) {
		return loadUserPort.loadUsers(familyId).stream()
				.map(LoadMemberService::toInfo)
				.toList();
	}

	private static MemberInfo toInfo(User user) {
		return new MemberInfo(user.getUserId(), user.getFamilyId(), user.getRole(), user.getName(), user.getPhone());
	}
}
