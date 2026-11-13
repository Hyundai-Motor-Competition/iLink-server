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
	private boolean childLocationEnabled;

	public static Family create() {
		return new Family(null, JoinCallMode.AUTO, 0, false);
	}

	public static Family restore(Long familyId, JoinCallMode joinCallMode, int settingsVersion) {
		return new Family(familyId, joinCallMode, settingsVersion, false);
	}

	public static Family restore(Long familyId, JoinCallMode joinCallMode, int settingsVersion,
			boolean childLocationEnabled) {
		return new Family(familyId, joinCallMode, settingsVersion, childLocationEnabled);
	}

	public void changeChildLocation(boolean enabled) {
		this.childLocationEnabled = enabled;
		this.settingsVersion++;
	}

	public void changeCallMode(JoinCallMode mode) {
		this.joinCallMode = mode;
		this.settingsVersion++;
	}
}
