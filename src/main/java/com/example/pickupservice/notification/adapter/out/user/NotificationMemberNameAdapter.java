package com.example.pickupservice.notification.adapter.out.user;

import com.example.pickupservice.notification.application.port.out.LoadMemberNamePort;
import com.example.pickupservice.user.application.port.in.LoadMemberUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationMemberNameAdapter implements LoadMemberNamePort {

	private final LoadMemberUseCase loadMemberUseCase;

	@Override
	public Map<Long, String> loadNames(List<Long> userIds) {
		Map<Long, String> names = new HashMap<>();
		if (userIds.isEmpty()) {
			return names;
		}
		for (Long userId : userIds) {
			loadMemberUseCase.findMember(userId)
					.ifPresent(member -> names.put(userId, member.name()));
		}
		return names;
	}
}
