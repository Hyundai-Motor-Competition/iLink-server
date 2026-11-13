package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface LoadUserPort {

	Optional<User> loadUser(long userId);

	List<User> loadUsers(long familyId);

	List<User> loadUsersByIds(List<Long> userIds);

	long countNonChildMembers(long familyId);
}
