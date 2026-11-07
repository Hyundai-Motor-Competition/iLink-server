package com.example.pickupservice.user.domain;

import com.example.pickupservice.common.security.Role;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

	private final Long userId;
	private final Long familyId;
	private Role role;
	private String name;
	private String phone;

	public static User create(long familyId, Role role, String name, String phone) {
		return new User(null, familyId, role, name, phone);
	}

	public static User restore(Long userId, Long familyId, Role role, String name, String phone) {
		return new User(userId, familyId, role, name, phone);
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void changePhone(String phone) {
		this.phone = phone;
	}

	public void changeRole(Role role) {
		this.role = role;
	}
}
