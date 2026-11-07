package com.example.pickupservice.user.application.port.out;

import com.example.pickupservice.user.domain.User;

public interface SaveUserPort {

	User saveUser(User user);

	void deleteUser(long userId);
}
