package com.example.pickupservice.user.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Family {

	private final Long familyId;
	private JoinCallMode joinCallMode;
	private int settingsVersion;

	public static Family create() {
		return new Family(null, JoinCallMode.AUTO, 0);
	}

	public static Family restore(Long familyId, JoinCallMode joinCallMode, int settingsVersion) {
		return new Family(familyId, joinCallMode, settingsVersion);
	}

	public void changeCallMode(JoinCallMode mode) {
		this.joinCallMode = mode;
		this.settingsVersion++;
	}
}
